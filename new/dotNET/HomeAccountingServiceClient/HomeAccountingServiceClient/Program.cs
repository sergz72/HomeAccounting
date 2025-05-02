using System.Text.Json;
using FileServiceClientLibrary;
using HomeAccountingServiceClientLibrary;
using File = System.IO.File;

if (args.Length < 2)
{
    Usage();
    return;
}

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

switch (args[1])
{
    case "test":
        db.Test();
        Console.WriteLine("Db test finished.");
        break;
    case "month_report":
        if (args.Length != 4)
            Usage();
        else
        {
            var from = BuildDate(args[2]); 
            var to = BuildDate(args[3]);
            PrintReportResult(db.BuildReport(from, to, ReportGrouping.Month, null, null, null));
        }
        break;
    default:
        Usage();
        break;
}

return;

void PrintReportResult(List<ReportRow> result)
{
    foreach (var row in result)
        Console.WriteLine("{0,8}{1,30}{2,30}{3,30} {4} {5}.{6:00}\t{7}.{8:00}", row.Date, row.Account, row.Category, row.Subcategory,
                            row.Currency, row.Income / 100, row.Income % 100, row.Expenditure / 100, row.Expenditure % 100);
}

DateTime BuildDate(string date)
{
    var intDate = int.Parse(date);
    if (intDate < settings.MinDate)
        throw new Exception("Invalid date");
    return new DateTime(intDate / 100, intDate % 100, 1);
}

void Usage()
{
    Console.WriteLine("Usage: HomeAccountingServiceClient settingsFileName [test][month_report YYYYMM YYYYMM]");
}

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
    int MaxRequestMonths,
    int MinDate)
{
    public void Validate()
    {
        if (UserId == 0 || string.IsNullOrEmpty(ServerKeyFileName) || string.IsNullOrEmpty(KeyFileName) ||
            string.IsNullOrEmpty(HostName) || Port == 0 || string.IsNullOrEmpty(DbName) || MaxRequestMonths <= 0 ||
            MinDate <= 0)
            throw new Exception("Settings file validation error");
    }
}
