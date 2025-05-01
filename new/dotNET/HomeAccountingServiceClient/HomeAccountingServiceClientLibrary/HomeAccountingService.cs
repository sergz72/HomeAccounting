using FileServiceClientLibrary;
using HomeAccountingServiceClientLibrary.entities;
using NetworkServiceClientLibrary;
using ICSharpCode.SharpZipLib.BZip2;

namespace HomeAccountingServiceClientLibrary;

public class HomeAccountingService(FileServiceConfig config, byte[] key)
{
    private readonly FileService _fileService = new FileService(config);

    private FinanceRecord BuildFinanceRecord(byte[] data)
    {
        var decrypted = Decrypt(data);
        return FinanceRecord.FromBinary(decrypted);
    }

    private byte[] Decrypt(byte[] data)
    {
        if (data.Length < 13)
            throw new DbException("Invalid response");
        var cipher = new ChaCha20(key, data[..12]);
        return cipher.Encrypt(data[12..]);
    }

    public KeyValuePair<int, FinanceRecord> GetFinanceRecord(int date)
    {
        var response = _fileService.GetLast(1, date);
        return new KeyValuePair<int, FinanceRecord>(
            response.Key ?? date,
            response.Data != null ? BuildFinanceRecord(response.Data.Data) : new FinanceRecord()
        );
    }

    public SortedDictionary<int, FinanceRecord> GetFinanceRecords(int from, int to)
    {
        var response = _fileService.Get(from, to);
        return new SortedDictionary<int, FinanceRecord>(
            response.Data.
                Select(kv => new KeyValuePair<int, FinanceRecord>(kv.Key, BuildFinanceRecord(kv.Value.Data)))
                .ToDictionary()
        );
    }

    public Dicts GetDicts()
    {
        var response = _fileService.Get(0, 0);
        if (response.Data.Count == 0)
            throw new DbException("Empty response");
        var decrypted = Decrypt(response.Data.First().Value.Data);
        var decompressed = Decompress(decrypted);
        var dicts = Dicts.FromBinary(decompressed);
        return dicts;
    }

    private static MemoryStream Decompress(byte[] decrypted)
    {
        var outStream = new MemoryStream();
        BZip2.Decompress(new MemoryStream(decrypted), outStream, false);
        outStream.Position = 0;
        return outStream;
    }
}

public class DbException(string message) : Exception(message);