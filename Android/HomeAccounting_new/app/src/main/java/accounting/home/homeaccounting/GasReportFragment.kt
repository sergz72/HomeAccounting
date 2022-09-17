package accounting.home.homeaccounting

import accounting.home.homeaccounting.entities.*
import android.app.DatePickerDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate

class GasReportFragment: Fragment(), IData, View.OnClickListener {
    private var mReportViewAdapter: GasReportViewAdapter = GasReportViewAdapter()
    private lateinit var mDateStart: LocalDate
    private lateinit var mDateEnd: LocalDate
    private val mHandler = Handler(Looper.getMainLooper())

    fun setParameters(date1: LocalDate, date2: LocalDate) {
        mDateStart = date1
        mDateEnd = date2
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_gas_report, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val selectDateStart = requireActivity().findViewById<Button>(R.id.select_date_start)
        val selectDateEnd = requireActivity().findViewById<Button>(R.id.select_date_end)
        val generate = requireActivity().findViewById<Button>(R.id.generate)
        val resultsView = requireActivity().findViewById<RecyclerView>(R.id.results_view)

        selectDateStart.setOnClickListener(this)
        selectDateEnd.setOnClickListener(this)
        generate.setOnClickListener(this)
        updateDate()

        resultsView.hasFixedSize()
        resultsView.layoutManager = LinearLayoutManager(this.context)
        resultsView.adapter = mReportViewAdapter
    }

    private fun updateDate() {
        val dateStart = requireActivity().findViewById<TextView>(R.id.date_start)
        val dateEnd = requireActivity().findViewById<TextView>(R.id.date_end)
        dateStart.text = mDateStart.format(OperationsFragment.UI_DATE_FORMAT)
        dateEnd.text = mDateEnd.format(OperationsFragment.UI_DATE_FORMAT)
    }

    override fun refresh() {
        val call = SharedResources.buildGasReportService(
                mDateStart.format(OperationsFragment.CALL_DATE_FORMAT),
                mDateEnd.format(OperationsFragment.CALL_DATE_FORMAT)
        )
        call.doInBackground(object: HomeAccountingService.Callback<List<GasReportItem>> {
            override fun deserialize(response: String): List<GasReportItem> {
                return Gson().fromJson(response, object: TypeToken<List<GasReportItem>>(){}.type)
            }

            override fun isString(): Boolean {
                return false
            }

            override fun onResponse(response: List<GasReportItem>) {
                mHandler.post { showResults(response) }
            }

            override fun onFailure(t: Throwable?, response: String?) {
                mHandler.post {
                    if (t != null) {
                        SharedResources.alert(requireActivity(), t.message)
                    } else {
                        SharedResources.alert(requireActivity(), response)
                    }
                }
            }
        })
    }

    fun showResults(body: List<GasReportItem>) {
        mReportViewAdapter.setData(body)
        mReportViewAdapter.notifyDataSetChanged()
    }

    override fun add() {
    }

    override fun onClick(v: View?) {
        val selectDateStart = requireActivity().findViewById<Button>(R.id.select_date_start)
        val selectDateEnd = requireActivity().findViewById<Button>(R.id.select_date_end)

        when (v) {
            selectDateStart -> {
                val dialog = DatePickerDialog(requireActivity(), { _, year, monthOfYear, dayOfMonth ->
                    mDateStart = LocalDate.of(year, monthOfYear + 1, dayOfMonth)
                    if (mDateEnd.isBefore(mDateStart)) {
                        mDateEnd = mDateStart
                    }
                    updateDate()
                }, mDateStart.year, mDateStart.monthValue - 1, mDateStart.dayOfMonth)
                dialog.show()
            }
            selectDateEnd -> {
                val dialog = DatePickerDialog(requireActivity(), { _, year, monthOfYear, dayOfMonth ->
                    mDateEnd = LocalDate.of(year, monthOfYear + 1, dayOfMonth)
                    if (mDateStart.isAfter(mDateEnd)) {
                        mDateStart = mDateEnd
                    }
                    updateDate()
                }, mDateStart.year, mDateStart.monthValue - 1, mDateStart.dayOfMonth)
                dialog.show()
            }
            else -> { // Generate button
                refresh()
            }
        }
    }
}