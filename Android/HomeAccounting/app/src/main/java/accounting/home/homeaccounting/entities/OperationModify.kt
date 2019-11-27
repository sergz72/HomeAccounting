package accounting.home.homeaccounting.entities

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

import java.time.LocalDate

data class OperationModify(@SerializedName("Date") @JsonAdapter(LocalDateAdapter::class) val date: LocalDate,
                           @SerializedName("Id") val id: Int,
                           @SerializedName("Amount") val amount: String,
                           @SerializedName("Summa") val summa: String,
                           @SerializedName("SubcategoryId") val subcategoryId: Int,
                           @SerializedName("AccountId") val accountId: Int,
                           @SerializedName("Properties") val properties: Array<FinOpProperty>?)
