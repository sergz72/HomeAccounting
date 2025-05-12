using System;
using System.Collections.Generic;
using System.Linq;
using Avalonia.Controls;
using Avalonia.Input;
using Avalonia.Interactivity;
using HomeAccountingServiceClientLibrary;
using HomeAccountingServiceClientLibrary.entities;

namespace HomeAccountingUI;

internal record ReportParameters(
    DateTime DateFrom,
    DateTime DateTo,
    int Grouping,
    int? AccountId,
    int? CategoryId,
    int? SubcategoryId);

public partial class ReportView : UserControl
{
    private readonly Db _db;
    private readonly Stack<ReportParameters> _parameterStack = new();
    
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
        var allIdx = idNameList.FindIndex(item => item.Id == null);
        CbSubcategory.ItemsSource = idNameList;
        CbSubcategory.SelectedIndex = allIdx;
    }

    private void GenerateReport()
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

    private void Generate_OnClick(object? sender, RoutedEventArgs e)
    {
        GenerateReport();
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
        if (CbGrouping.SelectedIndex != (int)ReportGrouping.Detailed)
        {
            _parameterStack.Push(new ReportParameters(
                    DcDates.GetDateFrom(),
                    DcDates.GetDateTo(),
                    CbGrouping.SelectedIndex,
                    ((IdName)CbAccount.SelectedItem!).Id,
                    ((IdName)CbCategory.SelectedItem!).Id,
                    ((IdName)CbSubcategory.SelectedItem!).Id
                ));
            switch ((ReportGrouping)CbGrouping.SelectedIndex)
            {
                case ReportGrouping.Month:
                {
                    CbGrouping.SelectedIndex = (int)ReportGrouping.Category;
                    var intDate = int.Parse(reportRow.Date);
                    var date = new DateTime(intDate / 100, intDate % 100, 1);
                    DcDates.SetDates(date, date);
                    break;
                }
                case ReportGrouping.Account:
                {
                    if (reportRow.Account == "")
                    {
                        _parameterStack.Pop();
                        return;
                    }
                    CbGrouping.SelectedIndex = (int)ReportGrouping.Category;
                    var accountId = _db.Accounts.First(kv => kv.Value.Name == reportRow.Account).Key;
                    SelectAccount(accountId);
                    break;
                }
                default: // by category
                {
                    CbGrouping.SelectedIndex = (int)ReportGrouping.Detailed;
                    var categoryId = _db.Categories.First(kv => kv.Value == reportRow.Category).Key;
                    SelectCategory(categoryId);
                    break;
                }
            }
            GenerateReport();
        }
    }

    private void Back_OnClick(object? sender, RoutedEventArgs e)
    {
        if (_parameterStack.TryPop(out var parameters))
        {
            DcDates.SetDates(parameters.DateFrom, parameters.DateTo);
            CbGrouping.SelectedIndex = parameters.Grouping;
            SelectAccount(parameters.AccountId);
            SelectCategory(parameters.CategoryId);
            var idx = 0;
            foreach (var item in CbSubcategory.Items)
            {
                if (((IdName)item!).Id == parameters.SubcategoryId)
                    break;
                idx++;
            }
            CbSubcategory.SelectedIndex = idx;
            GenerateReport();
        }
    }

    private void SelectAccount(int? accountId)
    {
        var idx = 0;
        foreach (var item in CbAccount.Items)
        {
            if (((IdName)item!).Id == accountId)
                break;
            idx++;
        }
        CbAccount.SelectedIndex = idx;
    }

    private void SelectCategory(int? categoryId)
    {
        var idx = 0;
        foreach (var item in CbCategory.Items)
        {
            if (((IdName)item!).Id == categoryId)
                break;
            idx++;
        }
        CbCategory.SelectedIndex = idx;
    }
}