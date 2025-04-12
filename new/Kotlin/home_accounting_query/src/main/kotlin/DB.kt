package com.sz.home_accounting.query

import com.sz.home_accounting.query.entities.Dicts
import com.sz.home_accounting.query.entities.FinanceChanges
import com.sz.home_accounting.query.entities.FinanceRecord
import java.util.SortedMap

class DB(private val dicts: Dicts) {
    var data: SortedMap<Int, FinanceRecord> = sortedMapOf()

    fun calculateTotals()
    {
        var changes: FinanceChanges? = null
        for (kv in data) {
            if (changes == null)
                changes = FinanceChanges(kv.value.totals)
            else
                kv.value.totals = changes.buildTotals()
            kv.value.updateChanges(changes, dicts)
        }
    }

    fun printChanges(date: Int) {
        val index = if (date == 0) data.lastKey() else date
        println(index)
        val record = data.getValue(index)
        val changes = record.buildChanges(dicts)
        for (change in changes.changes)
        {
            val accountName = dicts.accounts.getValue(change.key).name
            println("$accountName ${change.value.summa} ${change.value.income} ${change.value.expenditure} ${change.value.getEndSumma()}")
        }
        println("Operations")
        for (op in record.operations) {
            val accountName = dicts.accounts.getValue(op.account).name
            val subcategory = dicts.subcategories.getValue(op.subcategory)
            val category = dicts.categories.getValue(subcategory.category)
            println("$accountName $category ${subcategory.name} ${op.amount} ${op.summa}")
        }
    }
}