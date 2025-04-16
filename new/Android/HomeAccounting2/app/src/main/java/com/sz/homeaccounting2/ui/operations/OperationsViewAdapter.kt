package com.sz.homeaccounting2.ui.operations

import android.view.View
import android.view.ViewGroup

import android.content.Context
import android.view.LayoutInflater
import android.widget.BaseExpandableListAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.sz.homeaccounting2.MainActivity
import com.sz.homeaccounting2.R

class OperationsViewAdapter(private val context: Context, private val model: OperationsViewModel,
                            private val executor: OpExecutor) : BaseExpandableListAdapter(),
    View.OnClickListener, SwipeDetector.OnSwipeListener {

    interface OpExecutor {
        fun modify(operationId: Long)
        fun delete(operationId: Long)
    }

    private data class Op(val isDelete: Boolean, val operationId: Long, val otherButton: ImageButton)

    private var mSwipeDetector: SwipeDetector = SwipeDetector(100, this)

    override fun getGroupCount(): Int {
        return model.operations.value!!.size
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return model.operations.value!![groupPosition].operations?.size ?: 0
    }

    override fun getGroup(groupPosition: Int): Any {
        return model.operations.value!![groupPosition]
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return model.operations.value!![groupPosition].operations!![childPosition]
    }

    override fun getGroupId(groupPosition: Int): Long {
        return model.operations.value!![groupPosition].accountId.toLong()
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return (groupPosition * 1000 + childPosition).toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        var v = convertView
        if (v == null) {
            v = LayoutInflater.from(context).inflate(R.layout.total_row, parent, false)
        }

        val accountView = v!!.findViewById<TextView>(R.id.account)
        val incomeView = v.findViewById<TextView>(R.id.income)
        val expenditureView = v.findViewById<TextView>(R.id.expenditure)
        val balanceView = v.findViewById<TextView>(R.id.balance)
        val indicatorView = v.findViewById<ExpandIndicator>(R.id.indicator)

        val ft = model.operations.value!![groupPosition]

        accountView.text = model.getAccountName(ft.accountId)
        incomeView.text = MainActivity.formatMoney(ft.summaIncome)
        expenditureView.text = MainActivity.formatMoney(ft.summaExpenditure)
        balanceView.text = MainActivity.formatMoney(ft.balance)
        if (!ft.operations.isNullOrEmpty()) {
            indicatorView.setExpanded(isExpanded)
        } else {
            indicatorView.setExpanded(null)
        }

        return v
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        var v = convertView
        if (v == null) {
            v = LayoutInflater.from(context).inflate(R.layout.operation_row, parent, false)
            v!!.setOnTouchListener(mSwipeDetector)
            v.findViewById<ImageButton>(R.id.bnModify).setOnClickListener(this)
            v.findViewById<ImageButton>(R.id.bnDelete).setOnClickListener(this)
        }

        val categoryView = v.findViewById<TextView>(R.id.category)
        val subcategoryView = v.findViewById<TextView>(R.id.subcategory)
        val amountView = v.findViewById<TextView>(R.id.amount)
        val summaView = v.findViewById<TextView>(R.id.summa)
        val bnModifyView = v.findViewById<ImageButton>(R.id.bnModify)
        val bnDeleteView = v.findViewById<ImageButton>(R.id.bnDelete)

        val op = model.operations.value!![groupPosition].operations!![childPosition]

        categoryView.text = model.getCategoryNameBySubcategoryId(op.subcategory)
        subcategoryView.text = model.getSubcategoryName(op.subcategory)
        amountView.text = MainActivity.formatMoney3(op.amount)
        summaView.text = MainActivity.formatMoney(op.summa)

        bnModifyView.visibility = View.GONE
        bnModifyView.tag = Op(false, getChildId(groupPosition, childPosition), bnDeleteView)
        bnDeleteView.visibility = View.GONE
        bnDeleteView.tag = Op(true, getChildId(groupPosition, childPosition), bnModifyView)

        return v
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return false
    }

    override fun onClick(v: View?) {
        val op = v!!.tag as Op
        if (op.isDelete) {
            executor.delete(op.operationId)
        } else {
            executor.modify(op.operationId)
        }
        v.visibility = View.GONE
        op.otherButton.visibility = View.GONE
    }

    override fun onSwipe(v: View?, a: SwipeDetector.Action) {
        when (a) {
            SwipeDetector.Action.RL -> {
                v!!.findViewById<ImageView>(R.id.bnModify).visibility = View.VISIBLE
                v.findViewById<ImageView>(R.id.bnDelete).visibility = View.VISIBLE
            }
            SwipeDetector.Action.LR -> {
                v!!.findViewById<ImageView>(R.id.bnModify).visibility = View.GONE
                v.findViewById<ImageView>(R.id.bnDelete).visibility = View.GONE
            }
            else -> {}
        }
    }
}
