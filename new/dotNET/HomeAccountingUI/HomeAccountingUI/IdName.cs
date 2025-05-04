using System;
using System.Collections.Generic;
using System.Linq;
using HomeAccountingServiceClientLibrary.entities;

namespace HomeAccountingUI;

internal record IdName(int? Id, string Name)
{
    internal static List<IdName> FromAccounts(Dictionary<int, Account> accounts) =>
        FromAny(accounts, account => account.Name);

    internal static List<IdName> FromCategories(Dictionary<int, string> categories) =>
        FromAny(categories, category => category);

    internal static List<IdName> FromSubcategories(Dictionary<int, Subcategory> subcategories) =>
        FromAny(subcategories, subcategory => subcategory.Name);
    
    private static List<IdName> FromAny<T>(Dictionary<int, T> values, Func<T, string> transformer)
    {
        var result = new List<IdName> { new IdName(null, "All") };
        result.AddRange(values
            .Select(value => new IdName(value.Key, transformer.Invoke(value.Value)))
        );
        result.Sort((a, b) => a.Name.CompareTo(b.Name));
        return result;
    }

    public override string ToString()
    {
        return Name;
    }
}