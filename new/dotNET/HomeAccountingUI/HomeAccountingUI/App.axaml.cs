using System;
using Avalonia;
using Avalonia.Controls.ApplicationLifetimes;
using Avalonia.Markup.Xaml;
using HomeAccountingServiceClientLibrary;

namespace HomeAccountingUI;

public partial class App : Application
{
    public override void Initialize()
    {
        AvaloniaXamlLoader.Load(this);
    }

    public override void OnFrameworkInitializationCompleted()
    {
        if (ApplicationLifetime is IClassicDesktopStyleApplicationLifetime desktop)
        {
            if (desktop.Args?.Length != 1)
            {
                desktop.MainWindow = new MessageBoxWindow("Error", "Invalid number of arguments");
            }
            else
            {
                try
                {
                    var logger = new ListControlLogger();
                    var db = new Db(desktop.Args[0], logger);
                    desktop.MainWindow = new MainWindow(db, logger);
                }
                catch (Exception e)
                {
                    desktop.MainWindow = new MessageBoxWindow("Error", e.Message);
                }
            }
        }

        base.OnFrameworkInitializationCompleted();
    }
}