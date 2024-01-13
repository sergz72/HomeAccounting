package accounting.home.homeaccounting

import accounting.home.homeaccounting.entities.Account
import accounting.home.homeaccounting.entities.IdName
import accounting.home.homeaccounting.entities.ReportItem
import accounting.home.homeaccounting.entities.Subcategory
import android.app.DatePickerDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import java.time.LocalDate
import java.util.*
import kotlin.collections.ArrayList

class ReportsFragment : Fragment(R.layout.fragment_reports), IData, View.OnClickListener,
    AdapterView.OnItemSelectedListener {
    private var mParameters: ReportParameters? = null
    private var mReportViewAdapter: ReportViewAdapter = ReportViewAdapter(this)
    private var mCategoryAdapter: ArrayAdapter<IdName>? = null
    private var mAccountsAdapter: ArrayAdapter<Account>? = null
    private lateinit var mDateStart: LocalDate
    private lateinit var mDateEnd: LocalDate
    private val mHandler = Handler(Looper.getMainLooper())
    private val mHistory = ArrayDeque<ReportParameters>()

    fun setParameters(parameters: ReportParameters) {
        mParameters = parameters
        mDateStart = parameters.date1
        mDateEnd = parameters.date2
    }

    private fun parametersUpdated() {
        mDateStart = mParameters!!.date1
        mDateEnd = mParameters!!.date2

        updateDate()

        val accounts = requireActivity().findViewById<Spinner>(R.id.accounts)
        val categories = requireActivity().findViewById<Spinner>(R.id.categories)
        val subcategories = requireActivity().findViewById<Spinner>(R.id.subcategories)
        val grouping = requireActivity().findViewById<Spinner>(R.id.grouping)

        var id = if (mParameters!!.accountId == -1) 0 else mParameters!!.accountId
        accounts.setSelection(mAccountsAdapter!!.getPosition(Account.buildAccount(id, "")))

        categories.setSelection(mCategoryAdapter!!.getPosition(IdName(mParameters!!.categoryId, "")))

        val adapter = fillSubcategories(mParameters!!.categoryId)
        id = if (mParameters!!.subcategoryId == -1) 0 else mParameters!!.subcategoryId
        subcategories.setSelection(adapter.getPosition(Subcategory.buildSubcategory(id, "", mParameters!!.categoryId.toShort())))

        grouping.setSelection(resources.getStringArray(R.array.groupByValues).indexOf(mParameters!!.grouping))

        if (mParameters!!.generate) {
            refresh()
        } else if (mParameters!!.body != null) {
            mReportViewAdapter.setData(mParameters!!.body!!)
            mReportViewAdapter.notifyDataSetChanged()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val selectDateStart = requireActivity().findViewById<Button>(R.id.select_date_start)
        val selectDateEnd = requireActivity().findViewById<Button>(R.id.select_date_end)
        val generate = requireActivity().findViewById<Button>(R.id.generate)
        val resultsView = requireActivity().findViewById<RecyclerView>(R.id.results_view)
        val accounts = requireActivity().findViewById<Spinner>(R.id.accounts)
        val categories = requireActivity().findViewById<Spinner>(R.id.categories)

        selectDateStart.setOnClickListener(this)
        selectDateEnd.setOnClickListener(this)
        generate.setOnClickListener(this)
        if (mParameters == null) {
            setParameters(ReportParameters.defaultParameters())
        }

        resultsView.hasFixedSize()
        resultsView.layoutManager = LinearLayoutManager(this.context)
        resultsView.adapter = mReportViewAdapter

        val accountsList = SharedResources.db!!.getAccounts().sortedBy { it.name }
        val accountsWithAll = ArrayList<Account>()
        accountsWithAll.add(Account.buildAccount(0, resources.getString(R.string.all)))
        accountsWithAll.addAll(accountsList)
        mAccountsAdapter = ArrayAdapter(requireContext(), R.layout.textview, accountsWithAll)
        mAccountsAdapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        accounts.adapter = mAccountsAdapter

        val categoriesList = SharedResources.db!!.getCategories().sortedBy { it.name }
        val categoriesWithAll = ArrayList<IdName>()
        categoriesWithAll.add(IdName(0, resources.getString(R.string.all)))
        categoriesWithAll.addAll(categoriesList)
        mCategoryAdapter = ArrayAdapter(requireContext(), R.layout.textview, categoriesWithAll)
        mCategoryAdapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categories.adapter = mCategoryAdapter
        categories.onItemSelectedListener = this

        requireActivity().onBackPressedDispatcher.addCallback(requireActivity(), object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (mHistory.isEmpty()) {
                    (requireActivity() as MainActivity).backPressed()
                } else {
                    mParameters = mHistory.removeFirst()
                    parametersUpdated()
                }
            }
        })

        parametersUpdated()
    }

    private fun fillSubcategories(categoryId: Int): ArrayAdapter<Subcategory> {
        val subcategories = requireActivity().findViewById<Spinner>(R.id.subcategories)

        val subcategoriesList = if (categoryId <= 0) emptyList() else
            SharedResources.db!!.getSubcategories()
                .filter { it.categoryId.toInt() == categoryId }
                .sortedBy { it.name }
        val subcategoriesWithAll = ArrayList<Subcategory>()
        subcategoriesWithAll.add(Subcategory.buildSubcategory(0, resources.getString(R.string.all), categoryId.toShort()))
        subcategoriesWithAll.addAll(subcategoriesList)
        val adapter = ArrayAdapter(requireContext(), R.layout.textview, subcategoriesWithAll)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        subcategories.adapter = adapter

        return adapter
    }

    override fun refresh() {
        val accounts = requireActivity().findViewById<Spinner>(R.id.accounts)
        val categories = requireActivity().findViewById<Spinner>(R.id.categories)
        val subcategories = requireActivity().findViewById<Spinner>(R.id.subcategories)
        val grouping = requireActivity().findViewById<Spinner>(R.id.grouping)

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
        call.doInBackground(object: HomeAccountingService.Callback<List<ReportItem>> {
            override fun deserialize(response: String): List<ReportItem> {
                return Gson().fromJson(response, object: TypeToken<List<ReportItem>>(){}.type)
            }

            override fun isString(): Boolean {
                return false
            }

            override fun onResponse(response: List<ReportItem>, compressed: ByteArray) {
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

    fun showResults(body: List<ReportItem>) {
        val sorted = body.sorted()

        val accounts = requireActivity().findViewById<Spinner>(R.id.accounts)
        val categories = requireActivity().findViewById<Spinner>(R.id.categories)
        val subcategories = requireActivity().findViewById<Spinner>(R.id.subcategories)
        val grouping = requireActivity().findViewById<Spinner>(R.id.grouping)

        val category = (categories.selectedItem as IdName).id
        val subcategory = (subcategories.selectedItem as Subcategory).id
        val account = (accounts.selectedItem as Account).id

        mParameters = ReportParameters(mDateStart, mDateEnd, grouping.selectedItem.toString(),
            account, category, subcategory, false, sorted)

        mReportViewAdapter.setData(sorted)
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

    private fun updateDate() {
        val dateStart = requireActivity().findViewById<TextView>(R.id.date_start)
        val dateEnd = requireActivity().findViewById<TextView>(R.id.date_end)
        dateStart.text = mDateStart.format(OperationsFragment.UI_DATE_FORMAT)
        dateEnd.text = mDateEnd.format(OperationsFragment.UI_DATE_FORMAT)
    }

    fun enter(ri: ReportItem) {
        val accounts = requireActivity().findViewById<Spinner>(R.id.accounts)
        val categories = requireActivity().findViewById<Spinner>(R.id.categories)
        val subcategories = requireActivity().findViewById<Spinner>(R.id.subcategories)
        val grouping = requireActivity().findViewById<Spinner>(R.id.grouping)

        when (grouping.selectedItemPosition) {
            0 -> {
                mHistory.addFirst(mParameters!!)
                mParameters = ReportParameters(
                    ri.date1!!,
                    ri.date1.withDayOfMonth(ri.date1.lengthOfMonth()),
                    "Category",
                    (accounts.selectedItem as Account).id,
                    (categories.selectedItem as IdName).id,
                    (subcategories.selectedItem as Subcategory).id,
                    true)
                parametersUpdated()
            }
            1 -> {
                mHistory.addFirst(mParameters!!)
                mParameters = ReportParameters(
                    mDateStart,
                    mDateEnd,
                    "Category",
                    ri.accountId,
                    (categories.selectedItem as IdName).id,
                    (subcategories.selectedItem as Subcategory).id,
                    true)
                parametersUpdated()
            }
            2 -> {
                mHistory.addFirst(mParameters!!)
                mParameters = ReportParameters(mDateStart,
                    mDateEnd,
                    "Detailed",
                    ri.accountId,
                    ri.categoryId,
                    ri.subcategoryId,
                    true)
                parametersUpdated()
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val categories = requireActivity().findViewById<Spinner>(R.id.categories)
        val categoryId = (categories.selectedItem as IdName).id
        fillSubcategories(categoryId)
    }
}
