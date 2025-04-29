package com.sz.homeaccounting2.ui.operations

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sz.home_accounting.core.DB
import com.sz.home_accounting.core.entities.Account
import com.sz.home_accounting.core.entities.FinanceOperation
import com.sz.home_accounting.core.entities.FinanceRecord
import com.sz.home_accounting.core.entities.Subcategory
import com.sz.homeaccounting2.MainActivity
import com.sz.homeaccounting2.MainActivity.Companion.getIntDate
import com.sz.homeaccounting2.ui.operations.entities.FinanceTotalAndOperations
import com.sz.smart_home.common.NetworkService
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.LocalDate

class OperationsViewModelFactory(private val db: DB) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(OperationsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            OperationsViewModel(db) as T
        } else {
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

class OperationsViewModel(private val db: DB) : ViewModel() {
    sealed class UiState {
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableLiveData<UiState>(UiState.Loading)
    val uiState: LiveData<UiState> = _uiState

    private val mHandler = Handler(Looper.getMainLooper())

    private var record: FinanceRecord? = null

    private val _operations = MutableLiveData<List<FinanceTotalAndOperations>>().apply {
        value = listOf()
    }
    val operations: LiveData<List<FinanceTotalAndOperations>> = _operations

    private val _date = MutableLiveData<LocalDate>().apply {
        value = LocalDate.now()
    }
    val date: LiveData<LocalDate> = _date

    fun setDate(value: LocalDate) {
        _date.value = value
        refresh()
    }

    fun nextDate() {
        setDate(date.value!!.plusDays(1))
    }

    fun prevDate() {
        setDate(_date.value!!.minusDays(1))
    }

    private fun setFinanceRecord(record: FinanceRecord) {
        this.record = record
        _operations.value = FinanceTotalAndOperations.fromFinanceRecord(record, db.dicts!!)
            .sortedWith(compareBy({ -(it.operations?.size ?: 0) }, {getAccountName(it.accountId)}))
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun refresh() {
        _uiState.value = UiState.Loading
        if (db.dicts == null) {
            db.init(object : NetworkService.Callback<FinanceRecord> {
                override fun onResponse(response: FinanceRecord) {
                    mHandler.post {
                        setFinanceRecord(response)
                        _uiState.value = UiState.Success
                    }
                }

                override fun onFailure(t: Throwable) {
                    mHandler.post { _uiState.value = UiState.Error(t.message ?: "Unknown error") }
                }
            })
        } else {
            db.getFinanceRecord(getIntDate(date.value!!),
                object : NetworkService.Callback<FinanceRecord> {
                    override fun onResponse(response: FinanceRecord) {
                        mHandler.post {
                            setFinanceRecord(response)
                            _uiState.value = UiState.Success
                        }
                    }

                    override fun onFailure(t: Throwable) {
                        mHandler.post { _uiState.value = UiState.Error(t.message ?: "Unknown error") }
                    }
                })
        }
    }

    fun getAccountName(accountId: Int): String {
        return db.dicts!!.accounts[accountId]!!.name
    }

    fun getAccount(accountId: Int): Account? {
        return db.dicts!!.accounts[accountId]
    }

    fun getSubcategoryName(subcategoryId: Int): String {
        return db.dicts!!.subcategories[subcategoryId]!!.name
    }

    fun getSubcategory(subcategoryId: Int): Subcategory? {
        return db.dicts!!.subcategories[subcategoryId]
    }

    fun getCategoryNameBySubcategoryId(subcategoryId: Int): String {
        return db.dicts!!.categories[db.dicts!!.subcategories[subcategoryId]!!.category]!!
    }

    fun getSubcategories(): List<Subcategory> {
        return db.dicts!!.subcategories.values.toList()
    }

    fun getActiveAccounts(): List<Account> {
        return db.buildAccounts(db.date).values.toList()
    }

    fun addOperation(op: FinanceOperation) {
        db.add(op, object : NetworkService.Callback<FinanceRecord> {
            override fun onResponse(response: FinanceRecord) {
                mHandler.post {
                    setFinanceRecord(response)
                    _uiState.value = UiState.Success
                }
            }

            override fun onFailure(t: Throwable) {
                mHandler.post { _uiState.value = UiState.Error(t.message ?: "Unknown error") }
            }
        })
    }

    fun modifyOperation(accountIdx: Int, operationIdx: Int, op: FinanceOperation) {
        val id = findOperationId(accountIdx, operationIdx)
        db.update(id, op, object : NetworkService.Callback<FinanceRecord> {
            override fun onResponse(response: FinanceRecord) {
                mHandler.post {
                    setFinanceRecord(response)
                    _uiState.value = UiState.Success
                }
            }

            override fun onFailure(t: Throwable) {
                mHandler.post { _uiState.value = UiState.Error(t.message ?: "Unknown error") }
            }
        })
    }

    fun getOperation(id: Long): FinanceOperation {
        val id = findOperationId((id / 1000).toInt(), (id % 1000).toInt())
        return db.data!!.operations[id]
    }

    private fun findOperationId(accountId: Int, operationId: Int): Int {
        val op = operations.value!![accountId].operations!![operationId]
        return record!!.operations
            .mapIndexed { idx, op -> idx to op }
            .filter { (_, rop) -> op.account == rop.account && op.subcategory == rop.subcategory }
            .map { it -> it.first }
            .first()
    }
}
