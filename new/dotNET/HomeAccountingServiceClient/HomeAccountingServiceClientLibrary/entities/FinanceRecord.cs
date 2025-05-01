using System.Text;

namespace HomeAccountingServiceClientLibrary.entities;

public enum FinOpPropertyCode {
    SECA,
    NETW,
    DIST,
    TYPE,
    PPTO,
    AMOU
}

public class FinanceRecord(List<FinanceOperation> operations, Dictionary<int, long> totals)
{
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
}

public record FinanceOperation(long? Amount, long Summa, int SubcategoryId, int AccountId, FinOpProperty[] Properties)
{
    internal static FinanceOperation FromBinary(BinaryReader reader)
    {
        var amount = reader.ReadInt64();
        var summa = reader.ReadInt64();
        var subcategoryId = (int)reader.ReadInt16();
        var accountId = (int)reader.ReadInt16();
        var propertiesCount = (int)reader.ReadByte();
        var properties = new FinOpProperty[propertiesCount];
        for (var i = 0; i < propertiesCount; i++) {
            properties[i] = FinOpProperty.FromBinary(reader);
        }
        return new FinanceOperation(amount == 0 ? null : amount, summa, subcategoryId, accountId, properties);
    }
}

public record FinOpProperty(long? NumericValue, string? StringValue, int? DateValue, FinOpPropertyCode Code)
{
    internal static FinOpProperty FromBinary(BinaryReader reader)
    {
        var code = (FinOpPropertyCode)reader.ReadByte();
        string? stringValue = null;
        long? numericValue = null;
        switch (code)
        {
            case FinOpPropertyCode.SECA:
            case FinOpPropertyCode.DIST:
            case FinOpPropertyCode.PPTO:
            case FinOpPropertyCode.AMOU:
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