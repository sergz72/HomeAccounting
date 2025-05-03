using Avalonia;
using Avalonia.Controls;
using Avalonia.Markup.Xaml;
using HomeAccountingServiceClientLibrary;

namespace HomeAccountingUI;

public partial class GasReportView : UserControl
{
    public GasReportView()
    {
        InitializeComponent();
    }
    
    public GasReportView(Db db)
    {
        InitializeComponent();
    }
}