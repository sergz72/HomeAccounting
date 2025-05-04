using System.Text.Json;
using FileServiceClientLibrary;
using HomeAccountingServiceClientLibrary.entities;
using File = System.IO.File;

namespace HomeAccountingServiceClientLibrary;

public class Db
{
    private readonly HomeAccountingService _service;
    private readonly Dicts _dicts;
    private readonly int _retryCount;
    private readonly int _maxRequestMonths;
    private readonly ILogger _logger;
    private readonly SortedDictionary<int, FinanceRecord> _records;
    private readonly int _minDate;
    
    private DateTime _from, _to;
    
    public int RecordCount => _records.Count;
    public Dictionary<int, string> Categories => _dicts.Categories;
    public Dictionary<int, Subcategory> Subcategories => _dicts.Subcategories;

    public Db(string configFileName, ILogger logger, int retryCount = 3)
    {
        var jsonString = File.ReadAllText(configFileName);
        var settings = JsonSerializer.Deserialize<Settings>(jsonString) ??
                       throw new Exception("Invalid settings file");
        settings.Validate();
        var serverKey = File.ReadAllBytes(settings.ServerKeyFileName);
        var key = File.ReadAllBytes(settings.KeyFileName);

        var config = new FileServiceConfig(settings.UserId, serverKey, settings.HostName, settings.Port, settings.TimeoutMs,
            settings.DbName);
        _service = new HomeAccountingService(config, key);
        _retryCount = retryCount;
        _maxRequestMonths = settings.MaxRequestMonths;
        _logger = logger;
        _dicts = Get(_service.GetDicts);
        _records = new SortedDictionary<int, FinanceRecord>();
        _from = DateTime.MinValue;
        _to = DateTime.MinValue;
        _minDate = settings.MinDate;
    }
    
    public Db(FileServiceConfig config, byte[] key, int maxRequestMonths, ILogger logger, int minDate, int retryCount = 3)
    {
        _retryCount = retryCount;
        _logger = logger;
        _maxRequestMonths = maxRequestMonths;
        _service = new HomeAccountingService(config, key);
        _dicts = Get(_service.GetDicts);
        _records = new SortedDictionary<int, FinanceRecord>();
        _from = DateTime.MinValue;
        _to = DateTime.MinValue;
        _minDate = minDate;
    }
    
    private T Get<T>(Func<T> f)
    {
        for (var i = 0; i < _retryCount - 1; i++)
        {
            try
            {
                return f();
            }
            catch (Exception e)
            {
                _logger.Error(e.Message);
            }
        }
        return f();
    }

    private void Get(DateTime from, DateTime to)
    {
        if (to < _from || from > _to)
        {
            _records.Clear();
            GetRecords(from, to);
            _from = from;
            _to = to;
        }
        if (from < _from)
        {
            GetRecords(from, _from.AddDays(-1));
            _from = from;
        }
        if (to > _to)
        {
            GetRecords(_to.AddDays(1), to);
            _to = to;
        }
    }
    
    public void GetAll()
    {
        var dateTo = DateTime.UtcNow;
        _records.Clear();
        for (;;)
        {
            var dateFrom = dateTo.AddMonths(-_maxRequestMonths);
            var data = Get(() => _service.GetFinanceRecords(DateToInt(dateFrom), DateToInt(dateTo)));
            if (data.Count == 0)
                break;
            foreach (var record in data)
                _records.Add(record.Key, record.Value);
            dateTo = dateFrom.AddDays(-1);
        }
    }
    
    public void Test()
    {
        FinanceChanges? changes = null;
        var totals = new Dictionary<int, long>();
        foreach (var kv in _records)
        {
            var accounts = BuildAccounts(kv.Key);
            CompareTotals(kv.Key, totals, kv.Value.Totals);
            changes = new FinanceChanges(totals);
            kv.Value.UpdateChanges(changes, _dicts);
            changes.Cleanup(accounts.Keys.ToHashSet());
            totals = changes.BuildTotals();
        }
    }

    public List<ReportRow> BuildReport(DateTime from, DateTime to, ReportGrouping grouping, int? account, int? category,
                                        int? subcategory)
    {
        Get(from, to);
        return grouping switch
        {
            ReportGrouping.Month => BuildByMonthReport(from, to, account, category, subcategory),
            ReportGrouping.Account => BuildByAccountReport(from, to, account, category, subcategory),
            ReportGrouping.Category => BuildByCategoryReport(from, to, account, category, subcategory),
            ReportGrouping.Detailed => BuildDetailedReport(from, to, account, category, subcategory),
            _ => throw new DbException("Unknown grouping type")
        };
    }

    public DateTime BuildDate(string date)
    {
        var intDate = int.Parse(date);
        if (intDate < _minDate)
            throw new Exception("Invalid date");
        return new DateTime(intDate / 100, intDate % 100, 1);
    }

    public DateTime BuildDateEnd(string date)
    {
        var intDate = int.Parse(date);
        if (intDate < _minDate)
            throw new Exception("Invalid date");
        return new DateTime(intDate / 100, intDate % 100, 1).AddMonths(1).AddDays(-1);
    }
    
    private IEnumerable<ReportRow> BuildReport(int from, string date, IEnumerable<FinanceOperation> operations,
                                                int? account, int? category, int? subcategory)
    {
        return operations
            .Where(op =>
                (account == null || op.AccountId == account) &&
                (category == null || _dicts.GetCategoryId(op.SubcategoryId) == category) &&
                (subcategory == null || op.SubcategoryId == subcategory))
            .GroupBy(op => _dicts.GetAccount(op.AccountId).Currency)
            .SelectMany(ops => BuildReportRows(from, date, ops.ToList()));

    }
    
    private List<ReportRow> BuildByMonthReport(DateTime fromDate, DateTime toDate, int? account, int? category, int? subcategory)
    {
        var from = DateToInt(fromDate);
        var to = DateToInt(toDate);
        var result = _records
            .Where(r => r.Key >= from && r.Key <= to)
            .GroupBy(kv => kv.Key / 100) // by month
            .SelectMany(kv => BuildReport(kv.Key * 100 + 1, kv.Key.ToString(),
                kv.SelectMany(r => r.Value.Operations), account, category, subcategory))
            .Reverse()
            .ToList();
        return result;
    }
    
    private IEnumerable<ReportRow> BuildReportRows(int from, string date, List<FinanceOperation> operations)
    {
        var changes = new FinanceChanges(new Dictionary<int, long>());
        var accounts = BuildAccounts(from);
        var intermediateRows = accounts
            .Select(account => account.Value.Currency)
            .Distinct()
            .Select(currency => KeyValuePair.Create(currency, new IntermediateReportRow(date)))
            .ToDictionary();
        foreach (var op in operations)
        {
            var subcategory = _dicts.Subcategories[op.SubcategoryId];
            if (subcategory.Code is SubcategoryCode.Trfr or SubcategoryCode.Incc or SubcategoryCode.Expc)
                continue;
            var currency = _dicts.GetAccount(op.AccountId).Currency;
            var row = intermediateRows[currency];
            row.ProcessOperation(_dicts, op);
            op.UpdateChanges(changes, _dicts);
        }
        return intermediateRows
            .Select(kv => kv.Value.ToReportRow(_dicts, kv.Key, changes))
            .Where(row => row.Expenditure != 0 || row.Income != 0);
    }
    
    private List<ReportRow> BuildByAccountReport(DateTime fromDate, DateTime toDate, int? account, int? category, int? subcategory)
    {
        var from = DateToInt(fromDate);
        var to = DateToInt(toDate);
        var result = _records
            .Where(r => r.Key >= from && r.Key <= to)
            .SelectMany(r => r.Value.Operations)
            .GroupBy(op => op.AccountId)
            .SelectMany(kv => BuildReport(from, "Total",
                                kv, account, category, subcategory))
            .OrderBy(row => row.Currency)
            .ThenBy(row => -row.Expenditure)
            .ThenBy(row => row.Income)
            .ToList();
        return result;
    }

    private List<ReportRow> BuildByCategoryReport(DateTime fromDate, DateTime toDate, int? account, int? category, int? subcategory)
    {
        var from = DateToInt(fromDate);
        var to = DateToInt(toDate);
        var result = _records
            .Where(r => r.Key >= from && r.Key <= to)
            .SelectMany(r => r.Value.Operations)
            .GroupBy(op => _dicts.Subcategories[op.SubcategoryId].Category)
            .SelectMany(kv => BuildReport(from, "Total",
                kv, account, category, subcategory))
            .OrderBy(row => row.Currency)
            .ThenBy(row => -row.Expenditure)
            .ThenBy(row => row.Income)
            .ToList();
        return result;
    }
    
    private List<ReportRow> BuildDetailedReport(DateTime fromDate, DateTime toDate, int? account, int? category, int? subcategory)
    {
        var from = DateToInt(fromDate);
        var to = DateToInt(toDate);
        var result = _records
            .Where(r => r.Key >= from && r.Key <= to)
            .SelectMany(r => BuildRows(r.Key, r.Value.Operations, account, category, subcategory))
            .ToList();
        return result;
    }

    private IEnumerable<ReportRow> BuildRows(int date, List<FinanceOperation> operations, int? account, int? category,
                                                int? subcategory)
    {
        return operations.Where(op =>
                (account == null || op.AccountId == account) &&
                (category == null || _dicts.GetCategoryId(op.SubcategoryId) == category) &&
                (subcategory == null || op.SubcategoryId == subcategory))
            .SelectMany(op => ReportRow.Create(date.ToString(), op, _dicts));
    }

    private void GetRecords(DateTime from, DateTime to)
    {
        while (from < to)
        {
            var end = from.AddMonths(_maxRequestMonths);
            if (end > to)
                end = to;
            var data = Get(() => _service.GetFinanceRecords(DateToInt(from), DateToInt(end)));
            foreach (var record in data)
                _records.Add(record.Key, record.Value);
            from = end.AddDays(1);
        }
    }
    
    private static void CompareTotals(int key, Dictionary<int, long> expected, Dictionary<int, long> fact)
    {
        if (fact.Count != expected.Count)
            throw new DbException($"totals counts do not match for key {key}");
    }

    public Dictionary<int, Account> BuildAccounts(int date)
    {
        return _dicts.Accounts
            .Where(account => account.Value.ActiveTo == null || account.Value.ActiveTo > date)
            .ToDictionary();
    }
    
    private static int DateToInt(DateTime date)
    {
        return date.Year * 10000 + date.Month * 100 + date.Day;
    }

    public static int GetIntDate(DateTime date)
    {
        return date.Year * 10000 + date.Month * 100 + date.Day;
    }
}

internal class Selection
{
    internal int? Id;
    private bool Selected;

    internal void Process(int id)
    {
        if (!Selected)
        {
            if (Id == null)
                Id = id;
            else if (Id != id)
            {
                Id = null;
                Selected = true;
            }
        }
    }
}

internal class IntermediateReportRow(string date)
{
    internal string Date => date;
    internal readonly Selection AccountSelection = new Selection();
    internal readonly Selection CategorySelection = new Selection();
    internal readonly Selection SubcategorySelection = new Selection();

    internal void ProcessOperation(Dicts dicts, FinanceOperation operation)
    {
        AccountSelection.Process(operation.AccountId);
        SubcategorySelection.Process(operation.SubcategoryId);
        CategorySelection.Process(dicts.Subcategories[operation.SubcategoryId].Category);
    }

    internal ReportRow ToReportRow(Dicts dicts, string currency, FinanceChanges changes)
    {
        var selectedChanges = changes.Changes
            .Where(kv => dicts.GetAccount(kv.Key).Currency == currency)
            .ToDictionary();
        return new ReportRow(
            date,
            AccountSelection.Id == null ? "" : dicts.Accounts[(int)AccountSelection.Id].Name,
            CategorySelection.Id == null ? "" : dicts.Categories[(int)CategorySelection.Id],
            SubcategorySelection.Id == null ? "" : dicts.Subcategories[(int)SubcategorySelection.Id].Name,
            currency, 
            selectedChanges.Values.Select(ch => ch.Income).Sum(),
            selectedChanges.Values.Select(ch => ch.Expenditure).Sum());
    }
}

public enum ReportGrouping
{
    Month,
    Account,
    Category,
    Detailed
}

public record ReportRow(
    string Date,
    string Account,
    string Category,
    string Subcategory,
    string Currency,
    long Income,
    long Expenditure)
{
    public static List<ReportRow> Create(string date, FinanceOperation op, Dicts dicts)
    {
        var changes = new FinanceChanges(new Dictionary<int, long>());
        op.UpdateChanges(changes, dicts);
        return changes.Changes
            .Select(change => Create(date, change.Key, change.Value, op, dicts))
            .ToList();
    }

    private static ReportRow Create(string date, int accountId, FinanceChange change, FinanceOperation op, Dicts dicts)
    {
        var account = dicts.Accounts[accountId];
        var subcategory = dicts.Subcategories[op.SubcategoryId];
        return new ReportRow(
            date,
            account.Name,
            dicts.Categories[subcategory.Category],
            subcategory.Name,
            account.Currency,
            change.Income,
            change.Expenditure
        );
    }

    public override string ToString()
    {
        return $"|{Date,8}|{Account,40}|{Category,40}|{Subcategory,40}|{Currency,8}|{Income / 100,8}.{Income % 100:00}|{Expenditure / 100,8}.{Expenditure % 100:00}|";
    }
    
    public static string BuildHeaderRow()
    {
        const string f1 = "Date";
        const string f2 = "Account";
        const string f3 = "Category";
        const string f4 = "Subcategory";
        const string f5 = "Currency";
        const string f6 = "Income";
        const string f7 = "Expenditure";
        return $"|{f1,-8}|{f2,-40}|{f3,-40}|{f4,-40}|{f5}|{f6,-11}|{f7}|";
    }
}

public interface ILogger
{
    void Error(string message);
    void Info(string message);
}

internal record Settings(
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