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

var db = new Db(config, key, settings.MaxRequestMonths, new ConsoleLogger());
Console.WriteLine("Dicts retrieved.");

db.GetAll();
Console.WriteLine("{0} Finance records retrieved.", db.RecordCount);
db.Test();
Console.WriteLine("Db test finished.");

return;

internal class ConsoleLogger: ILogger
{
    public void Error(string message)
    {
        Console.WriteLine("ERROR: {0}", message);
    }

    public void Info(string message)
    {
        Console.WriteLine("INFO: {0}", message);
    }
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
