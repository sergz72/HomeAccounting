package com.sz.homeaccounting2.ui.operations

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View

import java.util.ArrayList

import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.sz.home_accounting.core.entities.Account
import com.sz.home_accounting.core.entities.FinOpProperty
import com.sz.home_accounting.core.entities.FinOpPropertyCode
import com.sz.home_accounting.core.entities.FinanceOperation
import com.sz.home_accounting.core.entities.Subcategory
import com.sz.home_accounting.core.entities.SubcategoryCode
import com.sz.homeaccounting2.MainActivity
import com.sz.homeaccounting2.MainActivity.Companion.buildOperationsViewModel
import com.sz.homeaccounting2.MainActivity.Companion.getFileServiceConfig
import com.sz.homeaccounting2.R
import com.sz.homeaccounting2.ui.pin.NumericKeyboard
import kotlin.toString

class NewOperationActivity : AppCompatActivity(), View.OnClickListener, AdapterView.OnItemClickListener, TextWatcher,
    View.OnFocusChangeListener {
    private var mSelectedSubcategory: Subcategory? = null

    private var mAccountAdapter: ArrayAdapter<Account>? = null
    private var mSubcategoryAdapter: ArrayAdapter<Subcategory>? = null

    private var mOperationId: Long = 0

    private lateinit var viewModel: OperationsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_newoperation)

        val config = getFileServiceConfig(this) ?: return
        val (_, viewModel) = buildOperationsViewModel(this, config)
        this.viewModel = viewModel

        val addOperation = findViewById<Button>(R.id.add_operation)
        val subcategoryName = findViewById<AutoCompleteTextView>(R.id.subcategory_name)
        val account = findViewById<Spinner>(R.id.account)
        val secondAccount = findViewById<Spinner>(R.id.second_account)
        val network = findViewById<AutoCompleteTextView>(R.id.network)
        val type = findViewById<AutoCompleteTextView>(R.id.type)
        val amount = findViewById<EditText>(R.id.amount)
        val summa = findViewById<EditText>(R.id.summa)

        mOperationId = intent.getLongExtra("operationId", -1)
        addOperation.setOnClickListener(this)

        mSubcategoryAdapter =
            ArrayAdapter<Subcategory>(this, android.R.layout.simple_spinner_dropdown_item,
                                        viewModel.getSubcategories())
        subcategoryName.setAdapter<ArrayAdapter<Subcategory>>(mSubcategoryAdapter)
        subcategoryName.threshold = 3
        subcategoryName.onItemClickListener = this
        subcategoryName.addTextChangedListener(this)

        mAccountAdapter = ArrayAdapter<Account>(this, R.layout.textview, viewModel.getActiveAccounts())
        mAccountAdapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        account.adapter = mAccountAdapter

        secondAccount.adapter = mAccountAdapter

        var items = MainActivity.hints!!.hints["NETW"]
        if (items != null) {
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
            network.setAdapter(adapter)
            network.threshold = 1
        }
        items = MainActivity.hints!!.hints["TYPE"]
        if (items != null) {
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
            type.setAdapter(adapter)
            type.threshold = 1
        }

        mSelectedSubcategory = null
        if (mOperationId != -1L) {
            addOperation.setText(R.string.modify_operation)
            val operation = viewModel.getOperation(mOperationId)
            val sc = viewModel.getSubcategory(operation.subcategory)
            if (sc == null) {
                MainActivity.alert(this, "Unknown subcategory id: " + operation.subcategory)
                return
            }
            var position = mSubcategoryAdapter!!.getPosition(sc)
            if (position == -1) {
                MainActivity.alert(this, "Unknown subcategory id: " + operation.subcategory)
                return
            }
            subcategoryName.listSelection = position
            subcategoryName.setText(sc.toString())
            subcategoryName.performCompletion()
            mSelectedSubcategory = sc
            updateProperties()

            val accountValue = viewModel.getAccount(operation.account)
            if (accountValue == null) {
                MainActivity.alert(this, "Unknown account id: " + operation.account)
                return
            }
            position = mAccountAdapter!!.getPosition(accountValue)
            if (position == -1) {
                MainActivity.alert(this, "Unknown account id: " + operation.account)
                return
            }
            account.setSelection(position)

            amount.setText(MainActivity.formatMoney3(operation.amount))
            summa.setText(MainActivity.formatMoney(operation.summa))

            fillProperties(operation.properties)
        }

        amount.showSoftInputOnFocus = false
        amount.onFocusChangeListener = this
        summa.showSoftInputOnFocus = false
        summa.onFocusChangeListener = this

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            @SuppressLint("UnsafeIntentLaunch")
            override fun handleOnBackPressed() {
                setResult(RESULT_CANCELED, intent)
                finish()
            }
        })

    }

    private fun fillProperties(finOpProperties: List<FinOpProperty>?) {
        if (finOpProperties != null) {
            val secondAccount = findViewById<Spinner>(R.id.second_account)
            val network = findViewById<AutoCompleteTextView>(R.id.network)
            val type = findViewById<AutoCompleteTextView>(R.id.type)
            val distance = findViewById<EditText>(R.id.distance)

            for (property in finOpProperties) {
                when (property.code) {
                    FinOpPropertyCode.SECA -> {
                        val account = viewModel.getAccount(property.numericValue!!.toInt())
                        if (account == null) {
                            MainActivity.alert(this, "Unknown account id: " + property.numericValue)
                            return
                        }
                        val position = mAccountAdapter!!.getPosition(account)
                        if (position == -1) {
                            MainActivity.alert(this, "Unknown account id: " + property.numericValue)
                            return
                        }
                        secondAccount.setSelection(position)
                    }
                    FinOpPropertyCode.DIST -> distance.setText(property.numericValue.toString())
                    FinOpPropertyCode.NETW -> network.setText(property.stringValue)
                    FinOpPropertyCode.TYPE -> type.setText(property.stringValue)
                    else -> {
                        MainActivity.alert(this, "Unknown PropertyCode: " + property.code)
                        return
                    }
                }
            }
        }
    }

    private fun buildProperties(): List<FinOpProperty> {
        val secondAccount = findViewById<Spinner>(R.id.second_account)
        val network = findViewById<AutoCompleteTextView>(R.id.network)
        val type = findViewById<AutoCompleteTextView>(R.id.type)
        val distance = findViewById<EditText>(R.id.distance)

        return when (mSelectedSubcategory!!.code) {
            SubcategoryCode.Exch, SubcategoryCode.Trfr ->
                listOf(FinOpProperty((secondAccount.selectedItem as Account).id.toLong(), null, null,
                                             FinOpPropertyCode.SECA))
            SubcategoryCode.Fuel -> {
                val result = ArrayList<FinOpProperty>()
                if (distance.text.isNotEmpty()) {
                    result.add(FinOpProperty(distance.text.toString().toLong(), null, null,
                                                FinOpPropertyCode.DIST))
                }
                if (network.text.isNotEmpty()) {
                    result.add(FinOpProperty(null, network.text.toString(), null, FinOpPropertyCode.NETW))
                }
                if (type.text.isNotEmpty()) {
                    result.add(FinOpProperty(null, type.text.toString(), null, FinOpPropertyCode.TYPE))
                }
                result
            }
            else -> listOf()
        }
    }

    override fun onClick(v: View) {
        val summa = findViewById<EditText>(R.id.summa)

        if (summa.text.isEmpty()) {
            MainActivity.alert(this, R.string.empty_summa)
            return
        }
        if (mSelectedSubcategory == null) {
            MainActivity.alert(this, R.string.empty_subcategory)
            return
        }

        try {
            val operation = newFinanceOperation(summa.text.toString())
            if (mOperationId == -1L) {
                //add
                viewModel.addOperation(operation)
            } else {
                //modify
                viewModel.modifyOperation(
                    (mOperationId / 1000).toInt(), (mOperationId % 1000).toInt(),
                    operation
                )
            }
        } catch (e: Exception) {
            MainActivity.alert(this, e.message)
        }
    }

    fun newFinanceOperation(summaText: String): FinanceOperation {
        val accountSpinner = findViewById<Spinner>(R.id.account)
        val amountText = findViewById<EditText>(R.id.amount)
        val amount = (amountText.text.toString().toDouble() * 1000).toLong()
        val summa = (summaText.toString().toDouble() * 100).toLong()
        val subcategory = mSelectedSubcategory!!.id
        val account = (accountSpinner.selectedItem as Account).id
        val properties = buildProperties()
        return FinanceOperation(amount, summa, subcategory, account, properties)
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        mSelectedSubcategory = parent.getItemAtPosition(position) as Subcategory
        updateProperties()
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        if (mSelectedSubcategory != null) {
            val subcategoryName = findViewById<AutoCompleteTextView>(R.id.subcategory_name)
            mSelectedSubcategory = null
            subcategoryName.setText("")
        }
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
    }

    override fun afterTextChanged(s: Editable) {
    }

    private fun updateProperties() {
        val parameterLabel = findViewById<TextView>(R.id.parameter_label)
        val secaLabel = findViewById<TextView>(R.id.seca_label)
        val distLabel = findViewById<TextView>(R.id.dist_label)
        val networkLabel = findViewById<TextView>(R.id.network_label)
        val typeLabel = findViewById<TextView>(R.id.type_label)
        val secondAccount = findViewById<Spinner>(R.id.second_account)
        val network = findViewById<AutoCompleteTextView>(R.id.network)
        val type = findViewById<AutoCompleteTextView>(R.id.type)
        val distance = findViewById<EditText>(R.id.distance)

        var showSecondAccount = false
        var showDistance = false
        if (mSelectedSubcategory != null) {
            showSecondAccount = SubcategoryCode.Exch == mSelectedSubcategory!!.code ||
                    SubcategoryCode.Trfr == mSelectedSubcategory!!.code
            showDistance = SubcategoryCode.Fuel == mSelectedSubcategory!!.code
        }
        parameterLabel.visibility = if (showSecondAccount || showDistance) View.VISIBLE else View.GONE
        var visibility = if (showSecondAccount) View.VISIBLE else View.GONE
        secaLabel.visibility = visibility
        secondAccount.visibility = visibility
        visibility = if (showDistance) View.VISIBLE else View.GONE
        distLabel.visibility = visibility
        distance.visibility = visibility
        networkLabel.visibility = visibility
        network.visibility = visibility
        typeLabel.visibility = visibility
        type.visibility = visibility
    }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        val keyboard = findViewById<NumericKeyboard>(R.id.keyboard)
        if (hasFocus) {
            val view = currentFocus
            if (view != null) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                imm!!.hideSoftInputFromWindow(view.windowToken, 0)
            }
            keyboard.textControl = v as EditText
            keyboard.visibility = View.VISIBLE
        } else {
            keyboard.textControl = null
            keyboard.visibility = View.GONE
        }
    }
}
