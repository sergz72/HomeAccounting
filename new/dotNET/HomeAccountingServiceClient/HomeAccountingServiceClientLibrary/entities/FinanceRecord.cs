using System.Text;

namespace HomeAccountingServiceClientLibrary.entities;

public enum FinOpPropertyCode {
    Seca,
    Netw,
    Dist,
    Type,
    Ppto,
    Amou
}

public class FinanceRecord
{
    public Dictionary<int, long> Totals;
    public List<FinanceOperation> Operations { get; }

    public FinanceRecord(List<FinanceOperation> operations, Dictionary<int, long> totals)
    {
        Totals = totals;
        Operations = operations;
    }
    
    public FinanceRecord(): this([], new Dictionary<int, long>())
    {
    }

    internal static FinanceRecord FromBinary(byte[] data)
    {
        using var reader = new BinaryReader(new MemoryStream(data));
        
        var length = reader.ReadInt16();
        var totals = new Dictionary<int, long>();
        while (length-- > 0)
        {
            var accountId = (int)reader.ReadInt16();
            totals[accountId] = reader.ReadInt64();
        }

        length = reader.ReadInt16();
        var operations = new List<FinanceOperation>();
        while (length-- > 0)
            operations.Add(FinanceOperation.FromBinary(reader));
        if (reader.BaseStream.Position != reader.BaseStream.Length)
            throw new DbException("incorrect data");
        return new FinanceRecord(operations, totals);
    }
    
    internal void UpdateChanges(FinanceChanges changes, Dicts dicts)
    {
        Operations.ForEach(op => op.UpdateChanges(changes, dicts));
    }

    internal FinanceChanges BuildChanges(Dicts dicts)
    {
        var changes = new FinanceChanges(Totals);
        Operations.ForEach(op => op.UpdateChanges(changes, dicts));
        return changes;
    }
    
    internal FinanceRecord BuildNextRecord(Dicts dicts)
    {
        return new FinanceRecord([], BuildChanges(dicts).BuildTotals());
    }
}

public record FinanceOperation(long? Amount, long Summa, int SubcategoryId, int AccountId, List<FinOpProperty> Properties)
{
    internal static FinanceOperation FromBinary(BinaryReader reader)
    {
        var amount = reader.ReadInt64();
        var summa = reader.ReadInt64();
        var subcategoryId = (int)reader.ReadInt16();
        var accountId = (int)reader.ReadInt16();
        var propertiesCount = (int)reader.ReadByte();
        var properties = new List<FinOpProperty>();
        while (propertiesCount-- > 0)
            properties.Add(FinOpProperty.FromBinary(reader));
        return new FinanceOperation(amount == 0 ? null : amount, summa, subcategoryId, accountId, properties);
    }
    
   internal void UpdateChanges(FinanceChanges changes, Dicts dicts)
    {
        var subcategory = dicts.Subcategories[SubcategoryId];
        switch (subcategory.OperationCode)
        {
            case SubcategoryOperationCode.Incm:
                changes.Income(AccountId, Summa);
                break;
            case SubcategoryOperationCode.Expn:
                changes.Expenditure(AccountId, Summa);
                break;
            case SubcategoryOperationCode.Spcl:
                switch (subcategory.Code)
                {
                    // Пополнение карточного счета наличными
                    case SubcategoryCode.Incc:
                        HandleIncc(changes, dicts.Accounts);
                        break;
                    // Снятие наличных в банкомате
                    case SubcategoryCode.Expc:
                        HandleExpc(changes, dicts.Accounts);
                        break;
                    // Обмен валюты
                    case SubcategoryCode.Exch:
                        HandleExch(changes);
                        break;
                    // Перевод средств между платежными картами
                    case SubcategoryCode.Trfr:
                        HandleTrfr(changes);
                        break;
                    default: throw new DbException("unknown subcategory code");
                }
                break;
            default: throw new DbException("unknown subcategory operation code");
        }
    }

    private void HandleTrfr(FinanceChanges changes)
    {
        HandleTrfrWithSumma(changes, Summa);
    }

    private void HandleExch(FinanceChanges changes)
    {
        if (Amount == null)
            throw new DbException("null summa");
        HandleTrfrWithSumma(changes, Amount.Value / 10);
    }

    private void HandleTrfrWithSumma(FinanceChanges changes, long summa)
    {
        var secondAccountProperty = Properties.Find(property => property.Code == FinOpPropertyCode.Seca);
        var seca = secondAccountProperty?.NumericValue;
        if (seca == null)
            throw new DbException("null ssecond account");
        changes.Expenditure(AccountId, summa);
        changes.Income((int)seca, Summa);
    }
    
    private void HandleExpc(FinanceChanges changes, Dictionary<int, Account> accounts)
    {
        changes.Expenditure(AccountId, Summa);
        // cash account for corresponding currency code
        var cashAccount = accounts[AccountId].CashAccount;
        if (cashAccount == null)
            throw new DbException("null cash account");
        changes.Income((int)cashAccount, Summa);
    }

    private void HandleIncc(FinanceChanges changes, Dictionary<int, Account> accounts)
    {
        changes.Income(AccountId, Summa);
        // cash account for corresponding currency code
        var cashAccount = accounts[AccountId].CashAccount;
        if (cashAccount == null)
            throw new DbException("null cash account");
        changes.Expenditure((int)cashAccount, Summa);
    }}

public record FinOpProperty(long? NumericValue, string? StringValue, int? DateValue, FinOpPropertyCode Code)
{
    internal static FinOpProperty FromBinary(BinaryReader reader)
    {
        var code = (FinOpPropertyCode)reader.ReadByte();
        string? stringValue = null;
        long? numericValue = null;
        switch (code)
        {
            case FinOpPropertyCode.Seca:
            case FinOpPropertyCode.Dist:
            case FinOpPropertyCode.Ppto:
            case FinOpPropertyCode.Amou:
                numericValue = reader.ReadInt64();
                break;
            default:
                var length = (int)reader.ReadByte();
                stringValue = Encoding.ASCII.GetString(reader.ReadBytes(length));
                break;
        }

        return new FinOpProperty(numericValue, stringValue, null, code);
    }
}

internal class FinanceChanges
{
    internal readonly Dictionary<int, FinanceChange> Changes;

    internal FinanceChanges(Dictionary<int, long> totals)
    {
        Changes = totals
            .Select(kv => (kv.Key, new FinanceChange(kv.Value)))
            .ToDictionary();
    }

    internal void Income(int account, long summa)
    {
        if (Changes.TryGetValue(account, out var change))
            change.Income += summa;
        else 
            Changes[account] = new FinanceChange(0, summa, 0);
    }
    
    internal void Expenditure(int account, long summa)
    {
        if (Changes.TryGetValue(account, out var change))
            change.Expenditure += summa;
        else 
            Changes[account] = new FinanceChange(0, 0, summa);
    }

    public Dictionary<int, long> BuildTotals()
    {
        return Changes
            .Select(kv => (kv.Key, kv.Value.GetEndSumma()))
            .ToDictionary();
    }

    public void Cleanup(HashSet<int> accountIds)
    {
        var keysToDelete = Changes.Keys.Except(accountIds);
        foreach (var key in keysToDelete)
            Changes.Remove(key);
    }
}

internal class FinanceChange
{
    internal readonly long Summa;
    internal long Income;
    internal long Expenditure;

    internal FinanceChange(long summa)
    {
        Summa = summa;
        Income = Expenditure = 0;
    }

    internal FinanceChange(long summa, long income, long expenditure)
    {
        Summa = summa;
        Income = income;
        Expenditure = expenditure;
    }

    internal long GetEndSumma()
    {
        return Summa + Income - Expenditure;
    }
}
