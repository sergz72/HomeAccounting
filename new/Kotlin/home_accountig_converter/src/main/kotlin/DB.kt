package com.sz.home_accounting.converter

import com.sz.home_accounting.converter.entities.Account
import com.sz.home_accounting.converter.entities.Dicts
import com.sz.home_accounting.converter.entities.FinanceChanges
import com.sz.home_accounting.converter.entities.FinanceRecord
import java.util.SortedMap

class DB(private val dicts: Dicts, private val data: SortedMap<Int, FinanceRecord>) {
    fun calculateTotals(from: Int)
    {
        var changes: FinanceChanges? = null
        for (kv in data.filter { it.key >= from }) {
            val accounts =  buildAccounts(kv.key)
            if (changes == null)
                changes = FinanceChanges(kv.value.totals)
            else
                kv.value.totals = changes.buildTotals()
            kv.value.updateChanges(changes, dicts)
            changes.cleanup(accounts.keys)
        }
    }

    private fun buildAccounts(date: Int): Map<Int, Account> {
        return dicts.accounts.filter { it.value.activeTo == null || it.value.activeTo!! > date }
    }

    fun printChanges(date: Int) {
        val index = if (date == 0) data.lastKey() else date
        val record = data.getValue(index)
        val changes = record.buildChanges(dicts)
        for (change in changes.changes)
        {
            val accountName = dicts.accounts.getValue(change.key).name
            println("$accountName ${change.value.summa} ${change.value.income} ${change.value.expenditure} ${change.value.getEndSumma()}")
        }
    }
}