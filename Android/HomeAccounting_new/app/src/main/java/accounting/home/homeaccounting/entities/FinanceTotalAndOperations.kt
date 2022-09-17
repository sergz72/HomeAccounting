package accounting.home.homeaccounting.entities

import com.google.gson.annotations.SerializedName

data class FinanceTotalAndOperations(@SerializedName("AccountId") val accountId: Int,
                                     @SerializedName("SummaIncome") val summaIncome: Int,
                                     @SerializedName("SummaExpenditure") val summaExpenditure: Int,
                                     @SerializedName("Balance") val balance: Int,
                                     @SerializedName("Operations") val operations: List<FinanceOperation>?) {
    companion object {
        fun getOperation(operations: List<FinanceTotalAndOperations>, operationId: Int): FinanceOperation {
            return operations
                .filter { it.operations != null }
                .flatMap { it.operations!! }
                .first { it.id == operationId }
        }
    }
}
