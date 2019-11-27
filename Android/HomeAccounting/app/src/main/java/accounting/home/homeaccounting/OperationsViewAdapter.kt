package accounting.home.homeaccounting

import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import java.util.ArrayList

import accounting.home.homeaccounting.entities.FinanceOperation

class OperationsViewAdapter : RecyclerView.Adapter<OperationsViewAdapter.ViewHolder>() {
    private var mOperations: List<FinanceOperation> = ArrayList()
    var selectedPosition: Int = 0
        private set

    fun setData(data: List<FinanceOperation>) {
        mOperations = data
        selectedPosition = 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.operation_row, parent, false) as View
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val op = mOperations[position]
        holder.mCategoryView.setText(SharedResources.db!!.getCategoryNameBySubcategoryId(op.subcategoryId))
        holder.mSubcategoryView.setText(SharedResources.db!!.getSubcategoryName(op.subcategoryId))
        holder.mAccountView.setText(SharedResources.db!!.getAccount(op.accountId)!!.name)
        holder.mAmountView.setText(SharedResources.db!!.formatMoney3(op.amount))
        holder.mSummaView.setText(SharedResources.db!!.formatMoney(op.summa))
        holder.itemView.setBackgroundColor(if (selectedPosition == position) Color.GREEN else Color.TRANSPARENT)
    }

    override fun getItemCount(): Int {
        return mOperations.size
    }

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
        var mCategoryView: TextView = v.findViewById<View>(R.id.category) as TextView
        var mSubcategoryView: TextView = v.findViewById<View>(R.id.subcategory) as TextView
        var mAccountView: TextView = v.findViewById<View>(R.id.account) as TextView
        var mAmountView: TextView = v.findViewById<View>(R.id.amount) as TextView
        var mSummaView: TextView = v.findViewById<View>(R.id.summa) as TextView

        init {
            v.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            if (adapterPosition == RecyclerView.NO_POSITION)
                return

            // Updating old as well as new positions
            notifyItemChanged(selectedPosition)
            selectedPosition = adapterPosition
            notifyItemChanged(selectedPosition)
        }
    }
}
