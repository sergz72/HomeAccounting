package accounting.home.homeaccounting.entities

import accounting.home.homeaccounting.SharedResources
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.time.LocalDate

data class ReportItem(@SerializedName("Date1") @JsonAdapter(LocalDateAdapter::class) val date1: LocalDate?,
                      @SerializedName("Date2") @JsonAdapter(LocalDateAdapter::class) val date2: LocalDate?,
                      @SerializedName("CategoryId") val categoryId: Int,
                      @SerializedName("SubcategoryId") val subcategoryId: Int,
                      @SerializedName("AccountId") val accountId: Int,
                      @SerializedName("ValutaCode") val valutaCode: String,
                      @SerializedName("SummaIncome") val summaIncome: Int,
                      @SerializedName("SummaExpenditure") val summaExpenditure: Int): Comparable<ReportItem> {
    override fun compareTo(other: ReportItem): Int {
        if (this.date1 != null && other.date1 != null) {
            if (this.date1.isAfter(other.date1)) {
                return -1
            } else if (this.date1.isBefore(other.date1)) {
                return 1
            }
        }
        val res1 = this.valutaCode.compareTo(other.valutaCode)
        if (res1 != 0) {
            return res1
        }

        val res3 = other.summaExpenditure - this.summaExpenditure
        if (res3 != 0) {
            return res3
        }

        if (this.accountId > 0 && other.accountId > 0 && this.accountId != other.accountId) {
            val aname1 = SharedResources.db!!.getAccount(this.accountId)!!.name
            val aname2 = SharedResources.db!!.getAccount(other.accountId)!!.name
            val res2 = aname1.compareTo(aname2)
            if (res2 != 0) {
                return res2
            }
        }

        if (this.categoryId > 0 && other.categoryId > 0 && this.categoryId != other.categoryId) {
            val cname1 = SharedResources.db!!.getCategoryNameById(this.categoryId)
            val cname2 = SharedResources.db!!.getCategoryNameById(other.categoryId)
            val res2 = cname1.compareTo(cname2)
            if (res2 != 0) {
                return res2
            }
        }

        if (this.subcategoryId > 0 && other.subcategoryId > 0 && this.subcategoryId != other.subcategoryId) {
            val scname1 = SharedResources.db!!.getSubcategoryName(this.subcategoryId)
            val scname2 = SharedResources.db!!.getSubcategoryName(other.subcategoryId)
            return scname1.compareTo(scname2)
        }

        return 0
    }
}
