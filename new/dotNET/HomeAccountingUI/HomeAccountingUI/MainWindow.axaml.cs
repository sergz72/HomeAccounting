using Avalonia.Controls;
using Avalonia.Interactivity;
using HomeAccountingServiceClientLibrary;

namespace HomeAccountingUI;

public partial class MainWindow : Window
{
    private readonly ListControlLogger _logger;
    private readonly ReportView _reportView;
    private readonly GasReportView _gasReportView;
    private readonly AccountsReportView _accountsReportView;

    public MainWindow()
    {
        InitializeComponent();
    }
    
    public MainWindow(Db db, ListControlLogger logger)
    {
        InitializeComponent();
        logger.Control = LbLog;
        _logger = logger;
        _reportView = new ReportView(db);
        _gasReportView = new GasReportView(db);
        _accountsReportView = new AccountsReportView(db);
        ShowReportView();
    }

    private void Exit_OnClick(object? sender, RoutedEventArgs e)
    {
        Close();
    }

    private void Report_OnClick(object? sender, RoutedEventArgs e)
    {
        ShowReportView();
    }

    private void GasReport_OnClick(object? sender, RoutedEventArgs e)
    {
        PContents.Children[0] = _gasReportView;
    }

    private void AccountsReport_OnClick(object? sender, RoutedEventArgs e)
    {
        PContents.Children[0] = _accountsReportView;
    }
    
    private void ShowReportView()
    {
        if (PContents.Children.Count == 0)
            PContents.Children.Add(_reportView);
        else
            PContents.Children[0] = _reportView;
    }
}