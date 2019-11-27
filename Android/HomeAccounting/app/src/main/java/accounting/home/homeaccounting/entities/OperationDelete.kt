package accounting.home.homeaccounting.entities

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

import java.time.LocalDate

data class OperationDelete(@SerializedName("Date") @JsonAdapter(LocalDateAdapter::class) val date: LocalDate,
                           @SerializedName("Id") val id: Int)
