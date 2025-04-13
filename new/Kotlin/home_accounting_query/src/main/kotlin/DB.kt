package com.sz.home_accounting.query

import com.sz.home_accounting.query.entities.*
import com.sz.smart_home.common.NetworkService
import java.time.LocalDateTime

class DB(private val service: HomeAccountingService) {
    companion object {
        val now = getNowAsInt()

        private fun getNowAsInt(): Int {
            val dateNow = LocalDateTime.now()
            return dateNow.year * 10000 + dateNow.month.value * 100 + dateNow.dayOfMonth
        }
    }

    var date: Int = now
    var data: FinanceRecord? = null
    var dicts: Dicts? = null

    fun init(callback: NetworkService.Callback<FinanceRecord>) {
        service.getDicts(object: NetworkService.Callback<Dicts> {
            override fun onResponse(response: Dicts) {
                dicts = response
                getFinanceRecord(null, callback)
            }

            override fun onFailure(t: Throwable) {
                callback.onFailure(t)
            }
        })
    }

    fun getFinanceRecord(dateIn: Int?, callback: NetworkService.Callback<FinanceRecord>) {
        if (dicts == null) {
            callback.onFailure(IllegalStateException("Dicts are not initialized"))
            return
        }
        if (dateIn != null) {
            date = dateIn
        }
        service.getFinanceRecord(date, object: NetworkService.Callback<Pair<Int, FinanceRecord>> {
            override fun onResponse(response: Pair<Int, FinanceRecord>) {
                data = if (response.first == date)
                    response.second
                else
                    response.second.buildNextRecord(dicts!!)
                callback.onResponse(data!!)
            }

            override fun onFailure(t: Throwable) {
                callback.onFailure(t)
            }
        })
    }

    fun add(op: FinanceOperation, callback: NetworkService.Callback<FinanceRecord>) {
        if (dicts == null || data == null) {
            callback.onFailure(IllegalStateException("Dicts or data are not initialized"))
            return
        }
        try {
            TODO()
            //data.add(op)
        } catch (t: Throwable) {
            callback.onFailure(t)
        }
    }

    /*fun calculateTotals()
    {
        var changes: FinanceChanges? = null
        for (kv in data) {
            val accounts =  buildAccounts(kv.key)
            if (changes == null)
                changes = FinanceChanges(kv.value.totals)
            else
                kv.value.totals = changes.buildTotals()
            kv.value.updateChanges(changes, dicts!!)
            changes.cleanup(accounts.keys)
        }
    }*/

    fun buildAccounts(date: Int): Map<Int, Account> {
        return dicts!!.accounts.filter { it.value.activeTo == null || it.value.activeTo!! > date }
    }

    fun buildSubcategories(): Map<String, Int> {
        return dicts!!.subcategories.map { it.value.name + " " + dicts!!.categories[it.value.category] to it.key }.toMap()
    }
}