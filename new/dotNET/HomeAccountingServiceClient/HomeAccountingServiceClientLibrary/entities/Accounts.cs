namespace HomeAccountingServiceClientLibrary.entities;

public record Account(int Id, string Name, string Currency, int? ActiveTo, int? CashAccount)
{
    internal static Dictionary<int, Account> FromBinary(BinaryReader reader)
    {
        var size = reader.ReadInt16();
        var result = new Dictionary<int, Account>();
        while (size-- > 0)
        {
            var account = Create(reader);
            result[account.Id] = account;
        }
        return result;
    }

    private static Account Create(BinaryReader reader)
    {
        var id = (int)reader.ReadInt16();
        var nameSize = reader.ReadByte();
        var name = System.Text.Encoding.UTF8.GetString(reader.ReadBytes(nameSize));
        var currency = System.Text.Encoding.UTF8.GetString(reader.ReadBytes(3));
        var activeTo = reader.ReadInt32();
        var cashAccount = (int)reader.ReadInt16();
        return new Account(id, name, currency, activeTo == 0 ? null : activeTo, 
                cashAccount == 0 ? null : cashAccount);
    }

    public override string ToString()
    {
        return Name;
    }
}
