using FileServiceClientLibrary;
using HomeAccountingServiceClientLibrary.entities;

namespace HomeAccountingServiceClientLibrary;

public class Db
{
    private readonly HomeAccountingService _service;
    private readonly Dicts _dicts;
    private readonly int _retryCount;
    private readonly int _maxRequestMonths;
    private readonly ILogger _logger;
    private readonly SortedDictionary<int, FinanceRecord> _records;

    public int RecordCount => _records.Count;
    
    public Db(FileServiceConfig config, byte[] key, int maxRequestMonths, ILogger logger, int retryCount = 3)
    {
        _retryCount = retryCount;
        _logger = logger;
        _maxRequestMonths = maxRequestMonths;
        _service = new HomeAccountingService(config, key);
        _dicts = Get(_service.GetDicts);
        _records = new SortedDictionary<int, FinanceRecord>();
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

    private static void CompareTotals(int key, Dictionary<int, long> expected, Dictionary<int, long> fact)
    {
        if (fact.Count != expected.Count)
            throw new DbException($"totals counts do not match for key {key}");
    }

    private Dictionary<int, Account> BuildAccounts(int date)
    {
        return _dicts.Accounts
            .Where(account => account.Value.ActiveTo == null || account.Value.ActiveTo > date)
            .ToDictionary();
    }
    
    private static int DateToInt(DateTime date)
    {
        return date.Year * 10000 + date.Month * 100 + date.Day;
    }
}

public interface ILogger
{
    void Error(string message);
    void Info(string message);
}