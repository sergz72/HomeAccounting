namespace HomeAccountingServiceClientLibrary.entities;

public enum SubcategoryCode {
    Comb,
    Comc,
    Fuel,
    Prcn,
    Incc,
    Expc,
    Exch,
    Trfr,
    None
}

public enum SubcategoryOperationCode {
    Incm,
    Expn,
    Spcl
}

public record Subcategory(int Id, string Name, SubcategoryCode Code, SubcategoryOperationCode OperationCode, int Category)
{
    internal static Dictionary<int, Subcategory> FromBinary(BinaryReader reader)
    {
        var size = reader.ReadInt16();
        var result = new Dictionary<int, Subcategory>();
        while (size-- > 0)
        {
            var subcategory = Create(reader);
            result[subcategory.Id] = subcategory;
        }
        return result;
    }
    
    private static Subcategory Create(BinaryReader reader)
    {
        var id = (int)reader.ReadInt16();
        var nameSize = reader.ReadByte();
        var name = System.Text.Encoding.UTF8.GetString(reader.ReadBytes(nameSize));
        var code = (SubcategoryCode)reader.ReadByte();
        var operationCode = (SubcategoryOperationCode)reader.ReadByte();
        var category = (int)reader.ReadInt16();
        return new Subcategory(id, name, code, operationCode, category); 
    }
}