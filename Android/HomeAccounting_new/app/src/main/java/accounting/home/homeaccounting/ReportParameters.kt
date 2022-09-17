package accounting.home.homeaccounting

import accounting.home.homeaccounting.entities.ReportItem
import java.time.LocalDate

data class ReportParameters(val date1: LocalDate, val date2: LocalDate, val grouping: String, val accountId: Int,
                            val categoryId: Int, val subcategoryId: Int, val generate: Boolean, val body: List<ReportItem>? = null) {
    companion object {
        fun defaultParameters(): ReportParameters {
            return ReportParameters(LocalDate.now().minusMonths(5).withDayOfMonth(1), LocalDate.now(), "Month", 0, 0, 0,false)
        }
    }
}