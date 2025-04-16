package com.sz.homeaccounting2.ui.operations.entities

import com.sz.home_accounting.core.entities.Dicts
import com.sz.home_accounting.core.entities.FinanceOperation
import com.sz.home_accounting.core.entities.FinanceRecord

data class FinanceTotalAndOperations(val accountId: Int,
                                     val summaIncome: Long,
                                     val summaExpenditure: Long,
                                     val balance: Long,
                                     val operations: List<FinanceOperation>?) {
    companion object {
        fun fromFinanceRecord(record: FinanceRecord, dicts: Dicts): List<FinanceTotalAndOperations> {
            return record.buildChanges(dicts).changes.map {
                FinanceTotalAndOperations(it.key, it.value.income, it.value.expenditure,
                                            it.value.getEndSumma(), record.operations.filter { op ->
                                                op.account == it.key })
            }
        }
    }
}
