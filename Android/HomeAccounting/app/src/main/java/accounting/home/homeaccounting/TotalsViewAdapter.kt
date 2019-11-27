package accounting.home.homeaccounting

import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import java.util.ArrayList

import accounting.home.homeaccounting.entities.FinanceTotal

class TotalsViewAdapter : RecyclerView.Adapter<TotalsViewAdapter.ViewHolder>() {

    private var mTotals: List<FinanceTotal> = ArrayList()
    private var mSelectedPosition = -1

    fun setData(data: List<FinanceTotal>) {
        mTotals = data
        mSelectedPosition = -1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.total_row, parent, false) as View
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ft = mTotals[position]
        holder.mAccountView.setText(SharedResources.db!!.getAccount(ft.accountId)!!.name)
        holder.mIncomeView.setText(SharedResources.db!!.formatMoney(ft.summaIncome))
        holder.mExpenditureView.setText(SharedResources.db!!.formatMoney(ft.summaExpenditure))
        holder.mBalanceView.setText(SharedResources.db!!.formatMoney(ft.balance))
        holder.itemView.setBackgroundColor(if (mSelectedPosition == position) SELECTED_ITEM_COLOR else Color.TRANSPARENT)
    }

    override fun getItemCount(): Int {
        return mTotals.size
    }

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
        var mAccountView: TextView = v.findViewById<View>(R.id.account) as TextView
        var mIncomeView: TextView = v.findViewById<View>(R.id.income) as TextView
        var mExpenditureView: TextView = v.findViewById<View>(R.id.expenditure) as TextView
        var mBalanceView: TextView = v.findViewById<View>(R.id.balance) as TextView

        init {
            v.setOnClickListener(this)
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

    companion object {
        private val SELECTED_ITEM_COLOR = Color.rgb(200, 200, 255)
    }
}
