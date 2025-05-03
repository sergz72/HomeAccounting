using System;
using Avalonia.Controls;
using HomeAccountingServiceClientLibrary;

namespace HomeAccountingUI;

public class ListControlLogger : ILogger
{
    internal ListBox? Control;
    
    public void Error(string message)
    {
        if (Control == null)
            Console.WriteLine("ERROR: {0}", message);
        else
            Control.Items.Add($"ERROR: {message}");
    }

    public void Info(string message)
    {
        if (Control == null)
            Console.WriteLine("INFO: {0}", message);
        else
            Control.Items.Add($"INFO: {message}");
    }
}