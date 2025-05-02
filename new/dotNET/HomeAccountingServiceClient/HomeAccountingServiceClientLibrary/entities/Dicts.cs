namespace HomeAccountingServiceClientLibrary.entities;

public record Dicts(Dictionary<int, Account> Accounts, Dictionary<int, string> Categories,
                    Dictionary<int, Subcategory> Subcategories,
                    Dictionary<SubcategoryCode, FinOpPropertyCode[]> SubcategoryToPropertyCodeMap)
{
    public static Dicts FromBinary(MemoryStream stream)
    {
        var reader = new BinaryReader(stream);
        var accounts = Account.FromBinary(reader);
        var categories = CategoryMap.FromBinary(reader);
        var subcategories = Subcategory.FromBinary(reader);
        var subcategoryToPropertyCodeMap = ReadSubcategoryToPropertyCodeMap(reader);
        if (reader.BaseStream.Position != reader.BaseStream.Length)
            throw new DbException("incorrect data");
        return new Dicts(accounts, categories, subcategories, subcategoryToPropertyCodeMap);
    }

    private static Dictionary<SubcategoryCode, FinOpPropertyCode[]> ReadSubcategoryToPropertyCodeMap(BinaryReader reader)
    {
        var size = (int)reader.ReadByte();
        var result = new Dictionary<SubcategoryCode, FinOpPropertyCode[]>();
        while (size-- > 0)
        {
            var subcategoryCode = (SubcategoryCode)reader.ReadByte();
            var arraySize = (int)reader.ReadByte();
            var array = new FinOpPropertyCode[arraySize];
            for (var i = 0; i < arraySize; i++)
            {
                var propertyCode = (FinOpPropertyCode)reader.ReadByte();
                array[i] = propertyCode;
            }
            result.Add(subcategoryCode, array);
        }
        return result;
    }
    
    public Account GetAccount(int accountId)
    {
        return Accounts[accountId];
    }

    public int GetCategoryId(int subcategoryId)
    {
        return Subcategories[subcategoryId].Category;
    }

}