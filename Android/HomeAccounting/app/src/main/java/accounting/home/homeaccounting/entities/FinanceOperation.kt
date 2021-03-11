package accounting.home.homeaccounting.entities

import com.google.gson.annotations.SerializedName

data class FinanceOperation(@SerializedName("Id") val id: Int,
                            @SerializedName("Amount") val amount: Int?,
                            @SerializedName("Summa") val summa: Int,
                            @SerializedName("SubcategoryId") val subcategoryId: Int,
                            @SerializedName("FinOpProperiesGroupId") val finOpProperiesGroupId: Int?,
                            @SerializedName("AccountId") val accountId: Int) {

    override fun hashCode(): Int {
        return id
    }

    override fun equals(other: Any?): Boolean {
        if (other is FinanceOperation) {
            return id == other.id
        }
        return false
    }
}
