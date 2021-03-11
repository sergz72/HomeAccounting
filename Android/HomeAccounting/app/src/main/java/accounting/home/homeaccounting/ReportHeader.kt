package accounting.home.homeaccounting

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.report_row.view.*

class ReportHeader(ctx: Context, attrs: AttributeSet) : LinearLayout(ctx, attrs) {
    init {
        val inflater = LayoutInflater.from(ctx)
        inflater.inflate(R.layout.report_row, this)
        date.text = resources.getString(R.string.select_date)
        account.text = resources.getString(R.string.account)
        category.text = resources.getString(R.string.category)
        subcategory.text = resources.getString(R.string.subcategory)
        valutaCode.text = resources.getString(R.string.`val`)
        income.text = resources.getString(R.string.income)
        expenditure.text = resources.getString(R.string.expenditure)
        post{
            val autoSizer = ViewAutoSize(ReportViewAdapter.AUTO_SIZE_PARAMETERS, this.width,
                    resources.displayMetrics.density)
            autoSizer.updateWidths(this)
        }
    }
}
