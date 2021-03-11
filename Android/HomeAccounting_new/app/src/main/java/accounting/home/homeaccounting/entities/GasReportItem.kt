package accounting.home.homeaccounting.entities

import accounting.home.homeaccounting.SharedResources
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.time.LocalDate

data class GasReportItem(@SerializedName("Date") @JsonAdapter(LocalDateAdapter::class) val date: LocalDate,
                         @SerializedName("Network") val network: String,
                         @SerializedName("Type") val type: String,
                         @SerializedName("Amount") val amount: Int,
                         @SerializedName("PricePerLiter") val pricePerLiter: Int,
                         @SerializedName("RelativeDistance") val relativeDistance: Int,
                         @SerializedName("AbsoluteDistance") val absoluteDistance: Int,
                         @SerializedName("LitersPer100km") val litersPer100km: Int): Comparable<GasReportItem> {
    override fun compareTo(other: GasReportItem): Int {
        if (this.date.isAfter(other.date)) {
            return -1
        } else if (this.date.isBefore(other.date)) {
            return 1
        }
        return other.absoluteDistance - this.absoluteDistance
    }
}
