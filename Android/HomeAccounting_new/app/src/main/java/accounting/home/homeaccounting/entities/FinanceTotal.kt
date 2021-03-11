package accounting.home.homeaccounting.entities

import com.google.gson.annotations.SerializedName

data class FinanceTotal(@SerializedName("AccountId") val accountId: Int,
                        @SerializedName("SummaIncome") val summaIncome: Int,
                        @SerializedName("SummaExpenditure") val summaExpenditure: Int,
                        @SerializedName("Balance") val balance: Int)
