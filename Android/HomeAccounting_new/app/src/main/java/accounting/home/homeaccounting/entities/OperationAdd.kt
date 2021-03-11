package accounting.home.homeaccounting.entities

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

import java.time.LocalDate

data class OperationAdd(@SerializedName("Date") @JsonAdapter(LocalDateAdapter::class) val date: LocalDate,
                        @SerializedName("Amount") private val amount: String,
                        @SerializedName("Summa") val summa: String,
                        @SerializedName("SubcategoryId") private val subcategoryId: Int,
                        @SerializedName("AccountId") private val accountId: Int,
                        @SerializedName("Properties") val properties: Array<FinOpProperty>?)
