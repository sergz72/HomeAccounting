package com.sz.homeaccounting2.ui.operations

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sz.home_accounting.core.DB
import com.sz.home_accounting.core.entities.FinanceRecord
import com.sz.homeaccounting2.ui.operations.entities.FinanceTotalAndOperations
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

    var operations: List<FinanceTotalAndOperations> = listOf()

    private val _date = MutableLiveData<LocalDate>().apply {
        value = LocalDate.now()
    }

    val date: LiveData<LocalDate> = _date

    fun setDate(value: LocalDate) {
        _date.value = value
    }

    fun nextDate() {
        _date.value = _date.value!!.plusDays(1)
    }

    fun prevDate() {
        _date.value = _date.value!!.minusDays(1)
    }

    fun setFinanceRecord(record: FinanceRecord) {

    }
}
