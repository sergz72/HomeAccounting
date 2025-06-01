using System;
using Avalonia.Controls;
using Avalonia.Interactivity;

namespace HomeAccountingUI;

public partial class DatesControl : UserControl
{
    public static readonly RoutedEvent<RoutedEventArgs> DateFromChangedEvent =
        RoutedEvent.Register<DatesControl, RoutedEventArgs>(nameof(DateFromChanged), RoutingStrategies.Direct);

    public static readonly RoutedEvent<RoutedEventArgs> DateToChangedEvent =
        RoutedEvent.Register<DatesControl, RoutedEventArgs>(nameof(DateToChanged), RoutingStrategies.Direct);
    
    private int _minYear;

    public event EventHandler<RoutedEventArgs> DateFromChanged
    {
        add => AddHandler(DateFromChangedEvent, value);
        remove => RemoveHandler(DateFromChangedEvent, value);
    }

    protected virtual void OnDateFromChanged()
    {
        var args = new RoutedEventArgs(DateFromChangedEvent);
        RaiseEvent(args);
    }

    public event EventHandler<RoutedEventArgs> DateToChanged
    {
        add => AddHandler(DateToChangedEvent, value);
        remove => RemoveHandler(DateToChangedEvent, value);
    }

    protected virtual void OnDateToChanged()
    {
        var args = new RoutedEventArgs(DateToChangedEvent);
        RaiseEvent(args);
    }
    
    public int MinYear
    {
        get => _minYear;
        set
        {
            _minYear = value;
            Init();
        }
    }
    
    public DatesControl()
    {
        InitializeComponent();
        MinYear = 2012;
    }

    private void Init()
    {
        var now = DateTime.Now;
        var yearNow = now.Year;
        var monthNow = now.Month;
        var minYear = _minYear;
        while (minYear <= yearNow)
        {
            CbFromYear.Items.Add(minYear);
            CbToYear.Items.Add(minYear);
            minYear++;
        }
        CbToYear.SelectedIndex = CbToYear.Items.Count - 1;
        CbToMonth.SelectedIndex = monthNow - 1;
        monthNow -= 6;
        var index = CbFromYear.Items.Count - 1;
        if (monthNow <= 0)
        {
            monthNow += 12;
            index--;
        }
        CbFromYear.SelectedIndex = index;
        CbFromMonth.SelectedIndex = monthNow - 1;
    }

    public DateTime GetDateFrom()
    {
        var year = (int)CbFromYear.SelectedItem!;
        var month = CbFromMonth.SelectedIndex + 1;
        return new DateTime(year, month, 1);
    }
    
    public DateTime GetDateTo()
    {
        var year = (int)CbToYear.SelectedItem!;
        var month = CbToMonth.SelectedIndex + 1;
        return new DateTime(year, month, 1).AddMonths(1).AddDays(-1);
    }

    public void SetDates(DateTime dateFrom, DateTime dateTo)
    {
        var yearFrom = dateFrom.Year;
        CbFromYear.SelectedItem = yearFrom;
        var monthFrom = dateFrom.Month;
        CbFromMonth.SelectedIndex = monthFrom - 1;
        var yearTo = dateTo.Year;
        CbToYear.SelectedItem = yearTo;
        var monthTo = dateTo.Month;
        CbToMonth.SelectedIndex = monthTo - 1;
    }
    
    private void CbFromMonth_OnSelectionChanged(object? sender, SelectionChangedEventArgs e)
    {
        OnDateFromChanged();
    }

    private void CbFromYear_OnSelectionChanged(object? sender, SelectionChangedEventArgs e)
    {
        OnDateFromChanged();
    }

    private void CbToMonth_OnSelectionChanged(object? sender, SelectionChangedEventArgs e)
    {
        OnDateToChanged();
    }

    private void CbToYear_OnSelectionChanged(object? sender, SelectionChangedEventArgs e)
    {
        OnDateToChanged();
    }
}