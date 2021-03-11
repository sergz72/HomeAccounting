package accounting.home.homeaccounting

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import java.util.ArrayList

import accounting.home.homeaccounting.entities.ReportItem
import android.content.res.Resources
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ReportViewAdapter(report: ReportsFragment) : RecyclerView.Adapter<ReportViewAdapter.ViewHolder>() {
    companion object {
        private val SELECTED_ITEM_COLOR = Color.rgb(200, 200, 255)
        val DAY_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val MONTH_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy")

        val AUTO_SIZE_PARAMETERS = listOf(
                ViewAutoSize.AutoSizeParameters(R.id.date, 40, 80),
                ViewAutoSize.AutoSizeParameters(R.id.account, 80, ViewAutoSize.FILL),
                ViewAutoSize.AutoSizeParameters(R.id.category, 80, ViewAutoSize.FILL),
                ViewAutoSize.AutoSizeParameters(R.id.subcategory, 80, ViewAutoSize.FILL),
                ViewAutoSize.AutoSizeParameters(R.id.valutaCode, 30, 40),
                ViewAutoSize.AutoSizeParameters(R.id.income, 70, 80),
                ViewAutoSize.AutoSizeParameters(R.id.expenditure, 70, 80)
        )
    }

    private var mReport: List<ReportItem> = ArrayList()
    private var mSelectedPosition = -1
    private var mResources: Resources? = null
    private var mReportFragment: ReportsFragment = report

    fun setData(data: List<ReportItem>) {
        mReport = data
        mSelectedPosition = -1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        mResources = parent.resources
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.report_row, parent, false) as View
        return ViewHolder(v, parent.measuredWidth, parent.resources.displayMetrics.density)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ri = mReport[position]
        holder.mDateView.text = buildDateRangeName(ri.date1, ri.date2)
        holder.mAccountView.text = if (ri.accountId > 0) SharedResources.db!!.getAccount(ri.accountId)!!.name else ""
        holder.mCategoryView.text = if (ri.categoryId > 0) SharedResources.db!!.getCategoryNameById(ri.categoryId) else ""
        holder.mSubcategoryView.text = if (ri.subcategoryId > 0) SharedResources.db!!.getSubcategoryName(ri.subcategoryId) else ""
        holder.mValutaView.text = ri.valutaCode
        holder.mIncomeView.text = SharedResources.db!!.formatMoney(ri.summaIncome)
        holder.mExpenditureView.text = SharedResources.db!!.formatMoney(ri.summaExpenditure)
        holder.itemView.setBackgroundColor(if (mSelectedPosition == position) SELECTED_ITEM_COLOR else Color.TRANSPARENT)
    }

    private fun buildDateRangeName(date1: LocalDate?, date2: LocalDate?): String {
        return if (date1 != null) {
            date1.format(if (date2 != null) MONTH_DATE_FORMAT else DAY_DATE_FORMAT)
        } else {
            mResources!!.getString(R.string.total)
        }
    }

    override fun getItemCount(): Int {
        return mReport.size
    }

    inner class ViewHolder(v: View, width: Int, density: Float) : RecyclerView.ViewHolder(v), View.OnClickListener {
        var mDateView: TextView = v.findViewById<View>(R.id.date) as TextView
        var mAccountView: TextView = v.findViewById<View>(R.id.account) as TextView
        var mCategoryView: TextView = v.findViewById<View>(R.id.category) as TextView
        var mSubcategoryView: TextView = v.findViewById<View>(R.id.subcategory) as TextView
        var mValutaView: TextView = v.findViewById<View>(R.id.valutaCode) as TextView
        var mIncomeView: TextView = v.findViewById<View>(R.id.income) as TextView
        var mExpenditureView: TextView = v.findViewById<View>(R.id.expenditure) as TextView
        var mAutoSizer: ViewAutoSize? = null

        init {
            v.setOnClickListener(this)
            if (mAutoSizer == null) {
                mAutoSizer = ViewAutoSize(AUTO_SIZE_PARAMETERS, width, density)
            }
            mAutoSizer!!.updateWidths(v)
        }

        override fun onClick(v: View) {
            if (adapterPosition == RecyclerView.NO_POSITION)
                return

            if (adapterPosition == mSelectedPosition) {
                mReportFragment.enter(mReport[mSelectedPosition])
                return
            }

            // Updating old as well as new positions
            notifyItemChanged(mSelectedPosition)
            mSelectedPosition = adapterPosition
            notifyItemChanged(mSelectedPosition)
        }
    }
}
