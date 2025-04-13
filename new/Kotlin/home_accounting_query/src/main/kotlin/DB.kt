package com.sz.home_accounting.query

import com.sz.home_accounting.query.entities.*
import com.sz.smart_home.common.NetworkService
import java.time.LocalDateTime
import java.util.SortedMap

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
            data!!.add(op)
            updateOperations(callback)
        } catch (t: Throwable) {
            callback.onFailure(t)
        }
    }

    fun updateOperations(callback: NetworkService.Callback<FinanceRecord>) {
        val dbVersion = service.dbVersion
        service.getFinanceRecords(date, object: NetworkService.Callback<SortedMap<Int, FinanceRecord>> {
            override fun onResponse(response: SortedMap<Int, FinanceRecord>) {
                if (service.dbVersion != dbVersion) {
                    callback.onFailure(IllegalStateException("DB version mismatch"))
                    return
                }
                response[date] = data
                calculateTotals(response)
                save(response, callback)
            }

            override fun onFailure(t: Throwable) {
                callback.onFailure(t)
            }

        })
    }

    fun save(records: SortedMap<Int, FinanceRecord>, callback: NetworkService.Callback<FinanceRecord>) {
        service.set(records, object: NetworkService.Callback<Unit> {
            override fun onResponse(response: Unit) {
                getFinanceRecord(null, callback)
            }

            override fun onFailure(t: Throwable) {
                callback.onFailure(t)
            }
        })
    }

    fun calculateTotals(valueMap: SortedMap<Int, FinanceRecord>)
    {
        var changes: FinanceChanges? = null
        for (kv in valueMap) {
            val accounts =  buildAccounts(kv.key)
            if (changes == null)
                changes = FinanceChanges(kv.value.totals)
            else
                kv.value.totals = changes.buildTotals()
            kv.value.updateChanges(changes, dicts!!)
            changes.cleanup(accounts.keys)
        }
    }

    fun buildAccounts(date: Int): Map<Int, Account> {
        return dicts!!.accounts.filter { it.value.activeTo == null || it.value.activeTo!! > date }
    }

    fun buildSubcategories(): Map<String, Int> {
        return dicts!!.subcategories.map { it.value.name + " " + dicts!!.categories[it.value.category] to it.key }.toMap()
    }
}