package com.sz.homeaccounting2.ui.operations.entities

import com.sz.home_accounting.core.entities.FinanceOperation

data class FinanceTotalAndOperations(val accountId: Int,
                                     val summaIncome: Long,
                                     val summaExpenditure: Long,
                                     val balance: Long,
                                     val operations: List<FinanceOperation>?)
