using HomeAccountingServiceClientLibrary;

if (args.Length < 2)
{
    Usage();
    return;
}


var db = new Db(args[0], new ConsoleLogger());
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
            var from = db.BuildDate(args[2]); 
            var to = db.BuildDate(args[3]);
            PrintReportResult(db.BuildReport(from, to, ReportGrouping.Month, null, null, null));
        }
        break;
    case "account_report":
        if (args.Length != 4)
            Usage();
        else
        {
            var from = db.BuildDate(args[2]); 
            var to = db.BuildDate(args[3]);
            PrintReportResult(db.BuildReport(from, to, ReportGrouping.Account, null, null, null));
        }
        break;
    case "category_report":
        if (args.Length != 4)
            Usage();
        else
        {
            var from = db.BuildDate(args[2]); 
            var to = db.BuildDate(args[3]);
            PrintReportResult(db.BuildReport(from, to, ReportGrouping.Category, null, null, null));
        }
        break;
    case "detailed_report":
        if (args.Length != 4)
            Usage();
        else
        {
            var from = db.BuildDate(args[2]); 
            var to = db.BuildDate(args[3]);
            PrintReportResult(db.BuildReport(from, to, ReportGrouping.Detailed, null, null, null));
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

void Usage()
{
    Console.WriteLine("Usage: HomeAccountingServiceClient settingsFileName [test][month_report|account_report|category_report|detailed_report YYYYMM YYYYMM]");
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

