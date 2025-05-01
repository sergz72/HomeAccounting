namespace HomeAccountingServiceClientLibrary.entities;

public static class CategoryMap
{
    internal static Dictionary<int, string> FromBinary(BinaryReader reader)
    {
        var size = reader.ReadInt16();
        var result = new Dictionary<int, string>();
        while (size-- > 0)
        {
            var id = (int)reader.ReadInt16();
            var nameSize = reader.ReadByte();
            var name = System.Text.Encoding.UTF8.GetString(reader.ReadBytes(nameSize));
            result[id] = name;
        }
        return result;
    }
}