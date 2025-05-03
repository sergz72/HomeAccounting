using Avalonia.Controls;
using Avalonia.Interactivity;
using HomeAccountingServiceClientLibrary;

namespace HomeAccountingUI;

public partial class ReportView : UserControl
{
    private readonly Db _db;
    
    public ReportView()
    {
        InitializeComponent();
    }

    public ReportView(Db db)
    {
        InitializeComponent();
        _db = db;
        FillAccounts();
    }

    private void FillAccounts()
    {
        var accounts = _db.BuildAccounts(Db.GetIntDate(DcDates.GetDateFrom()));
        CbAccount.ItemsSource = accounts.Values;
        CbAccount.SelectedIndex = 0;
    }

    private void Generate_OnClick(object? sender, RoutedEventArgs e)
    {
        //var result = _db.BuildReport()
    }

    private void DcDates_OnDateFromChanged(object? sender, RoutedEventArgs e)
    {
        FillAccounts();
    }
}