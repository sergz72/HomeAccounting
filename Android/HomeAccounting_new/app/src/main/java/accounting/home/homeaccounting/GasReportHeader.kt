package accounting.home.homeaccounting

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView

class GasReportHeader(ctx: Context, attrs: AttributeSet) : LinearLayout(ctx, attrs) {
    init {
        val inflater = LayoutInflater.from(ctx)
        inflater.inflate(R.layout.gas_report_row, this)
        val date = findViewById<TextView>(R.id.date)
        val network = findViewById<TextView>(R.id.network)
        val type = findViewById<TextView>(R.id.type)
        val amount = findViewById<TextView>(R.id.amount)
        val pricePerLiter = findViewById<TextView>(R.id.pricePerLiter)
        val distance = findViewById<TextView>(R.id.distance)
        val litersPer100km = findViewById<TextView>(R.id.litersPer100km)

        date.text = resources.getString(R.string.select_date)
        network.text = resources.getString(R.string.network_short)
        type.text = resources.getString(R.string.type_short)
        amount.text = resources.getString(R.string.amount)
        pricePerLiter.text = resources.getString(R.string.pricePerLiter)
        distance.text = resources.getString(R.string.distance)
        litersPer100km.text = resources.getString(R.string.litersPer100km)
        post{
            val autoSizer = ViewAutoSize(GasReportViewAdapter.AUTO_SIZE_PARAMETERS, this.width,
                                         resources.displayMetrics.density)
            autoSizer.updateWidths(this)
        }
    }
}
