using System;
using Avalonia.Controls;
using Avalonia.Interactivity;
using HomeAccountingServiceClientLibrary;

namespace HomeAccountingUI;

public partial class AccountsReportView : UserControl
{
    private readonly Db _db;
    
    private void Init()
    {
        DpDate.SelectedDate = DateTimeOffset.Now;
        LbAccounts.Items.Add(AccountSaldo.BuildHeaderRow());
        LbCurrencies.Items.Add(CurrencySaldo.BuildHeaderRow());
    }
    
    public AccountsReportView()
    {
        InitializeComponent();
        Init(); 
    }
    
    public AccountsReportView(Db db)
    {
        _db = db;
        InitializeComponent();
        Init(); 
    }

    private void GenerateReport()
    {
        var result = _db.BuildAccountsReport(((DateTimeOffset)DpDate.SelectedDate!).DateTime);
        LbAccounts.Items.Clear();
        LbAccounts.Items.Add(AccountSaldo.BuildHeaderRow());
        result.Accounts.ForEach(row => LbAccounts.Items.Add(row));
        LbCurrencies.Items.Clear();
        LbCurrencies.Items.Add(CurrencySaldo.BuildHeaderRow());
        result.Currencies.ForEach(row => LbCurrencies.Items.Add(row));
    }
    
    private void Generate_OnClick(object? sender, RoutedEventArgs e)
    {
        GenerateReport();
    }
}