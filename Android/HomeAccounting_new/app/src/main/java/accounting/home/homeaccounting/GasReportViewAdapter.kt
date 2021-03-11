package accounting.home.homeaccounting

import accounting.home.homeaccounting.entities.GasReportItem
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

class GasReportViewAdapter() : RecyclerView.Adapter<GasReportViewAdapter.ViewHolder>() {
    companion object {
        private val SELECTED_ITEM_COLOR = Color.rgb(200, 200, 255)
        val DAY_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

        val AUTO_SIZE_PARAMETERS = listOf(
                ViewAutoSize.AutoSizeParameters(R.id.date, 40, 80),
                ViewAutoSize.AutoSizeParameters(R.id.network, 80, ViewAutoSize.FILL),
                ViewAutoSize.AutoSizeParameters(R.id.type, 80, ViewAutoSize.FILL),
                ViewAutoSize.AutoSizeParameters(R.id.amount, 80, 90),
                ViewAutoSize.AutoSizeParameters(R.id.pricePerLiter, 50, 60),
                ViewAutoSize.AutoSizeParameters(R.id.distance, 90, 100),
                ViewAutoSize.AutoSizeParameters(R.id.litersPer100km, 70, 80)
        )
    }

    private var mReport: List<GasReportItem> = ArrayList()
    private var mSelectedPosition = -1
    private var mResources: Resources? = null

    fun setData(data: List<GasReportItem>) {
        mReport = data
        mSelectedPosition = -1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        mResources = parent.resources
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.gas_report_row, parent, false) as View
        return ViewHolder(v, parent.measuredWidth, parent.resources.displayMetrics.density)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ri = mReport[position]
        holder.mDateView.text = ri.date.format(DAY_DATE_FORMAT)
        holder.mNetworkView.text = ri.network
        holder.mTypeView.text = ri.type
        holder.mAmountView.text = SharedResources.db!!.formatMoney3(ri.amount)
        holder.mPricePerLiterView.text = SharedResources.db!!.formatMoney(ri.pricePerLiter)
        holder.mDistanceView.text = ri.relativeDistance.toString()
        holder.mLitersPer100kmView.text = SharedResources.db!!.format1(ri.litersPer100km)
        holder.itemView.setBackgroundColor(if (mSelectedPosition == position) SELECTED_ITEM_COLOR else Color.TRANSPARENT)
    }

    override fun getItemCount(): Int {
        return mReport.size
    }

    inner class ViewHolder(v: View, width: Int, density: Float) : RecyclerView.ViewHolder(v), View.OnClickListener {
        var mDateView: TextView = v.findViewById<View>(R.id.date) as TextView
        var mNetworkView: TextView = v.findViewById<View>(R.id.network) as TextView
        var mTypeView: TextView = v.findViewById<View>(R.id.type) as TextView
        var mAmountView: TextView = v.findViewById<View>(R.id.amount) as TextView
        var mPricePerLiterView: TextView = v.findViewById<View>(R.id.pricePerLiter) as TextView
        var mDistanceView: TextView = v.findViewById<View>(R.id.distance) as TextView
        var mLitersPer100kmView: TextView = v.findViewById<View>(R.id.litersPer100km) as TextView
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

            // Updating old as well as new positions
            notifyItemChanged(mSelectedPosition)
            mSelectedPosition = adapterPosition
            notifyItemChanged(mSelectedPosition)
        }
    }
}
