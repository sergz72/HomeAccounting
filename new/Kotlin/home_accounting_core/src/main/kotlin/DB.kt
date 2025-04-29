package com.sz.home_accounting.core

import com.sz.home_accounting.core.entities.*
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

    private var _date = now
    val date: Int
        get() = _date

    private var _data: FinanceRecord? = null
    val data
        get() = _data

    private var _dicts: Dicts? = null
    val dicts
        get() = _dicts

    fun resetDicts() {
        _dicts = null
    }
    fun init(callback: NetworkService.Callback<FinanceRecord>) {
        service.getDicts(object: NetworkService.Callback<Dicts> {
            override fun onResponse(response: Dicts) {
                _dicts = response
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
            _date = dateIn
        }
        service.getFinanceRecord(date, object: NetworkService.Callback<Pair<Int, FinanceRecord>> {
            override fun onResponse(response: Pair<Int, FinanceRecord>) {
                _data = if (response.first == date)
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

    private fun validateOperation(op: FinanceOperation, excludedIndex: Int,
                                  callback: NetworkService.Callback<FinanceRecord>): Boolean {
        if (data!!.operations
            .filterIndexed { idx, op -> idx != excludedIndex }
            .any { it.subcategory == op.subcategory && it.account == op.account }) {
            callback.onFailure(IllegalStateException("Duplicate operation"))
            return false
        }
        return true
    }

    private fun checkDictsAndData(callback: NetworkService.Callback<FinanceRecord>): Boolean {
        if (dicts == null || data == null) {
            callback.onFailure(IllegalStateException("Dicts or data are not initialized"))
            return false
        }
        return true
    }

    fun add(op: FinanceOperation, callback: NetworkService.Callback<FinanceRecord>) {
        if (!checkDictsAndData(callback))
            return
        if (!validateOperation(op, -1, callback))
            return
        data!!.add(op)
        try {
            updateOperations(callback)
        } catch (t: Throwable) {
            callback.onFailure(t)
        }
    }

    fun update(idx: Int, op: FinanceOperation, callback: NetworkService.Callback<FinanceRecord>) {
        if (!checkDictsAndData(callback))
            return
        if (!validateOperation(op, idx, callback))
            return
        data!!.operations[idx] = op
        try {
            updateOperations(callback)
        } catch (t: Throwable) {
            callback.onFailure(t)
        }
    }

    fun delete(idx: Int, callback: NetworkService.Callback<FinanceRecord>) {
        if (!checkDictsAndData(callback))
            return
        data!!.operations.removeAt(idx)
        try {
            updateOperations(callback)
        } catch (t: Throwable) {
            callback.onFailure(t)
        }
    }

    private fun updateOperations(callback: NetworkService.Callback<FinanceRecord>) {
        val dbVersion = service.dbVersion
        service.getFinanceRecords(date, 99999999, object: NetworkService.Callback<SortedMap<Int, FinanceRecord>> {
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

    private fun save(records: SortedMap<Int, FinanceRecord>, callback: NetworkService.Callback<FinanceRecord>) {
        service.set(records, object: NetworkService.Callback<Unit> {
            override fun onResponse(response: Unit) {
                getFinanceRecord(null, callback)
            }

            override fun onFailure(t: Throwable) {
                callback.onFailure(t)
            }
        })
    }

    private fun calculateTotals(valueMap: SortedMap<Int, FinanceRecord>)
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