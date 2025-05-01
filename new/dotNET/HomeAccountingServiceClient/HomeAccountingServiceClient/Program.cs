using System.Text.Json;
using FileServiceClientLibrary;
using HomeAccountingServiceClientLibrary;
using HomeAccountingServiceClientLibrary.entities;
using File = System.IO.File;

var jsonString = File.ReadAllText(args[0]);
var settings = JsonSerializer.Deserialize<Settings>(jsonString) ??
            throw new Exception("Invalid settings file");
settings.Validate();
var serverKey = File.ReadAllBytes(settings.ServerKeyFileName);
var key = File.ReadAllBytes(settings.KeyFileName);

var config = new FileServiceConfig(settings.UserId, serverKey, settings.HostName, settings.Port, settings.TimeoutMs,
                                    settings.DbName);
var service = new HomeAccountingService(config, key);

var dicts = service.GetDicts();
Console.WriteLine("Dicts retrieved.");

var dateTo = DateTime.UtcNow;
var records = new SortedDictionary<int, FinanceRecord>();
    
for (;;)
{
    var dateFrom = dateTo.AddMonths(-settings.MaxRequestMonths);
    var data = service.GetFinanceRecords(DateToInt(dateFrom), DateToInt(dateTo));
    if (data.Count == 0)
        break;
    foreach (var record in data)
        records.Add(record.Key, record.Value);
    dateTo = dateFrom.AddDays(-1);
}

Console.WriteLine("{0} Finance records retrieved.", records.Count);

return;

int DateToInt(DateTime date)
{
    return date.Year * 10000 + date.Month * 100 + date.Day;
}

record Settings(
    int UserId,
    string ServerKeyFileName,
    string KeyFileName,
    string HostName,
    ushort Port,
    int TimeoutMs,
    string DbName,
    int MaxRequestMonths)
{
    public void Validate()
    {
        if (UserId == 0 || string.IsNullOrEmpty(ServerKeyFileName) || string.IsNullOrEmpty(KeyFileName) ||
            string.IsNullOrEmpty(HostName) || Port == 0 || string.IsNullOrEmpty(DbName) || MaxRequestMonths <= 0)
            throw new Exception("Settings file validation error");
    }
}
