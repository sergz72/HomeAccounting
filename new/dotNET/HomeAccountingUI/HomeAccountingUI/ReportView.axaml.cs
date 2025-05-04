using System.Collections.Generic;
using System.Linq;
using Avalonia.Controls;
using Avalonia.Controls.Presenters;
using Avalonia.Input;
using Avalonia.Interactivity;
using HomeAccountingServiceClientLibrary;
using HomeAccountingServiceClientLibrary.entities;

namespace HomeAccountingUI;

public partial class ReportView : UserControl
{
    private readonly Db _db;
    
    public ReportView()
    {
        InitializeComponent();
        LbData.Items.Add(ReportRow.BuildHeaderRow());
    }

    public ReportView(Db db)
    {
        InitializeComponent();
        _db = db;
        FillAccounts();
        FillCategories();
        FillSubcategories();
        LbData.Items.Add(ReportRow.BuildHeaderRow());
    }

    private void FillAccounts()
    {
        var accounts = _db.BuildAccounts(Db.GetIntDate(DcDates.GetDateFrom()));
        var idNameList = IdName.FromAccounts(accounts);
        CbAccount.ItemsSource = idNameList;
        CbAccount.SelectedIndex = 0;
    }

    private void FillCategories()
    {
        var idNameList = IdName.FromCategories(_db.Categories);
        CbCategory.ItemsSource = idNameList;
        CbCategory.SelectedIndex = 0;
    }

    private void FillSubcategories()
    {
        var category = (IdName)CbCategory.SelectedItem!;
        var list = category.Id == null 
            ? new Dictionary<int, Subcategory>()
            : _db.Subcategories.Where(kv => kv.Value.Category == category.Id).ToDictionary();
        var idNameList = IdName.FromSubcategories(list);
        CbSubcategory.ItemsSource = idNameList;
        CbSubcategory.SelectedIndex = 0;
    }
    
    private void Generate_OnClick(object? sender, RoutedEventArgs e)
    {
        var account = (IdName)CbAccount.SelectedItem!;
        var category = (IdName)CbCategory.SelectedItem!;
        var subcategory = (IdName)CbSubcategory.SelectedItem!;
        var grouping = (ReportGrouping)CbGrouping.SelectedIndex;
        var result = _db.BuildReport(DcDates.GetDateFrom(), DcDates.GetDateTo(), grouping,
            account.Id, category.Id, subcategory.Id);
        LbData.Items.Clear();
        LbData.Items.Add(ReportRow.BuildHeaderRow());
        result.ForEach(row => LbData.Items.Add(row));
    }

    private void DcDates_OnDateFromChanged(object? sender, RoutedEventArgs e)
    {
        FillAccounts();
    }

    private void CbCategory_OnSelectionChanged(object? sender, SelectionChangedEventArgs e)
    {
        FillSubcategories();
    }

    private void LbData_OnDoubleTapped(object? sender, TappedEventArgs e)
    {
        if (e.Source is TextBlock { DataContext: ReportRow reportRow })
            Enter(reportRow);
    }

    private void Enter(ReportRow reportRow)
    {
    }

    private void Back_OnClick(object? sender, RoutedEventArgs e)
    {
    }
}