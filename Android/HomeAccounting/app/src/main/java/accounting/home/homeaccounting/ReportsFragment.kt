package accounting.home.homeaccounting

import accounting.home.homeaccounting.entities.Account
import accounting.home.homeaccounting.entities.IdName
import accounting.home.homeaccounting.entities.ReportItem
import accounting.home.homeaccounting.entities.Subcategory
import android.app.DatePickerDialog
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.fragment_reports.*

import java.time.LocalDate
import kotlin.collections.ArrayList

class ReportsFragment : Fragment(), IData, View.OnClickListener, AdapterView.OnItemSelectedListener {
    private var mParameters: ReportParameters? = null
    private var mReportViewAdapter: ReportViewAdapter = ReportViewAdapter(this)
    private var mCategoryAdapter: ArrayAdapter<IdName>? = null
    private var mAccountsAdapter: ArrayAdapter<Account>? = null
    private lateinit var mDateStart: LocalDate
    private lateinit var mDateEnd: LocalDate
    private val mHandler = Handler()

    fun setParameters(parameters: ReportParameters) {
        mParameters = parameters
        mDateStart = parameters.date1
        mDateEnd = parameters.date2
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        select_date_start.setOnClickListener(this)
        select_date_end.setOnClickListener(this)
        generate.setOnClickListener(this)
        if (mParameters == null) {
            setParameters(ReportParameters.defaultParameters())
        }
        updateDate()

        results_view.hasFixedSize()
        results_view.layoutManager = LinearLayoutManager(this.context)
        results_view.adapter = mReportViewAdapter

        val accountsList = SharedResources.db!!.getAccounts().sortedBy { it.name }
        val accountsWithAll = ArrayList<Account>()
        accountsWithAll.add(Account.buildAccount(0, resources.getString(R.string.all)))
        accountsWithAll.addAll(accountsList)
        mAccountsAdapter = ArrayAdapter(this.context!!, R.layout.textview, accountsWithAll)
        mAccountsAdapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        accounts.adapter = mAccountsAdapter
        var id = if (mParameters!!.accountId == -1) 0 else mParameters!!.accountId
        accounts.setSelection(mAccountsAdapter!!.getPosition(Account.buildAccount(id, "")))

        val categoriesList = SharedResources.db!!.getCategories().sortedBy { it.name }
        val categoriesWithAll = ArrayList<IdName>()
        categoriesWithAll.add(IdName(0, resources.getString(R.string.all)))
        categoriesWithAll.addAll(categoriesList)
        mCategoryAdapter = ArrayAdapter(this.context!!, R.layout.textview, categoriesWithAll)
        mCategoryAdapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categories.adapter = mCategoryAdapter
        categories.setSelection(mCategoryAdapter!!.getPosition(IdName(mParameters!!.categoryId, "")))
        categories.onItemSelectedListener = this
        
        val adapter = fillSubcategories(mParameters!!.categoryId)
        id = if (mParameters!!.subcategoryId == -1) 0 else mParameters!!.subcategoryId
        subcategories.setSelection(adapter.getPosition(Subcategory.buildSubcategory(id, "", mParameters!!.categoryId.toShort())))

        grouping.setSelection(resources.getStringArray(R.array.groupByValues).indexOf(mParameters!!.grouping))

        if (mParameters!!.generate) {
            refresh()
        }
    }

    private fun fillSubcategories(categoryId: Int): ArrayAdapter<Subcategory> {
        val subcategoriesList = if (categoryId == 0) emptyList() else
            SharedResources.db!!.getSubcategories()
                .filter { it.categoryId.toInt() == categoryId }
                .sortedBy { it.name }
        val subcategoriesWithAll = ArrayList<Subcategory>()
        subcategoriesWithAll.add(Subcategory.buildSubcategory(0, resources.getString(R.string.all), categoryId.toShort()))
        subcategoriesWithAll.addAll(subcategoriesList)
        val adapter = ArrayAdapter(this.context!!, R.layout.textview, subcategoriesWithAll)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        subcategories.adapter = adapter

        return adapter
    }

    override fun refresh() {
        val groupBy = resources.getStringArray(R.array.groupByValues)[grouping.selectedItemPosition]
        val category = (categories.selectedItem as IdName).id
        val categoryString = if (category == 0) "All" else category.toString()
        val subcategory = (subcategories.selectedItem as Subcategory).id
        val subcategoryString = if (subcategory == 0) "All" else subcategory.toString()
        val account = (accounts.selectedItem as Account).id
        val accountString = if (account == 0) "All" else account.toString()
        val call = SharedResources.buildReportsService(
            mDateStart.format(OperationsFragment.CALL_DATE_FORMAT),
            mDateEnd.format(OperationsFragment.CALL_DATE_FORMAT),
            groupBy,
            categoryString,
            subcategoryString,
            accountString
        )
        call.execute(object: HomeAccountingService.Callback<List<ReportItem>> {
            override fun deserialize(response: String): List<ReportItem> {
                return Gson().fromJson(response, object: TypeToken<List<ReportItem>>(){}.type)
            }

            override fun isString(): Boolean {
                return false
            }

            override fun onResponse(response: List<ReportItem>) {
                mHandler.post { showResults(response) }
            }

            override fun onFailure(t: Throwable?, response: String?) {
                mHandler.post {
                    if (t != null) {
                        SharedResources.alert(activity!!, t.message)
                    } else {
                        SharedResources.alert(activity!!, response)
                    }
                }
            }
        })
    }

    fun showResults(body: List<ReportItem>) {
        val sorted = body.sorted()
        mReportViewAdapter.setData(sorted)
        mReportViewAdapter.notifyDataSetChanged()
    }

    override fun modify() {
    }

    override fun delete() {
    }

    override fun add() {
    }

    override fun onClick(v: View?) {
        if (v == select_date_start) {
            val dialog = DatePickerDialog(activity!!, DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                mDateStart = LocalDate.of(year, monthOfYear + 1, dayOfMonth)
                if (mDateEnd.isBefore(mDateStart)) {
                    mDateEnd == mDateStart
                }
                updateDate()
            }, mDateStart.year, mDateStart.monthValue - 1, mDateStart.dayOfMonth)
            dialog.show()
        } else if (v == select_date_end) {
            val dialog = DatePickerDialog(activity!!, DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                mDateEnd = LocalDate.of(year, monthOfYear + 1, dayOfMonth)
                if (mDateStart.isAfter(mDateEnd)) {
                    mDateStart == mDateEnd
                }
                updateDate()
            }, mDateStart.year, mDateStart.monthValue - 1, mDateStart.dayOfMonth)
            dialog.show()
        } else { // Generate button
            refresh()
        }
    }

    private fun updateDate() {
        date_start.text = mDateStart.format(OperationsFragment.UI_DATE_FORMAT)
        date_end.text = mDateEnd.format(OperationsFragment.UI_DATE_FORMAT)
    }

    fun enter(ri: ReportItem) {
        when (grouping.selectedItemPosition) {
            0 -> (activity!! as MainActivity).showReportsFragment(
                    ReportParameters(ri.date1!!,
                                     ri.date1.withDayOfMonth(ri.date1.lengthOfMonth()),
                            "Category",
                                     (accounts.selectedItem as Account).id,
                                     (categories.selectedItem as IdName).id,
                                     (subcategories.selectedItem as Subcategory).id,
                                     true))
            1 -> (activity!! as MainActivity).showReportsFragment(
                    ReportParameters(mDateStart,
                                     mDateEnd,
                                     "Category",
                                     ri.accountId,
                                     (categories.selectedItem as IdName).id,
                                     (subcategories.selectedItem as Subcategory).id,
                                     true))
            2 -> (activity!! as MainActivity).showReportsFragment(
                    ReportParameters(mDateStart,
                                     mDateEnd,
                            "Detailed",
                                     ri.accountId,
                                     ri.categoryId,
                                     ri.subcategoryId,
                            true))
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val categoryId = (categories.selectedItem as IdName).id
        fillSubcategories(categoryId)
    }
}
