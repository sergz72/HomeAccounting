package accounting.home.homeaccounting

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View

import java.math.BigDecimal
import java.time.LocalDate
import java.util.ArrayList

import accounting.home.homeaccounting.entities.Account
import accounting.home.homeaccounting.entities.FinOpProperty
import accounting.home.homeaccounting.entities.OperationAdd
import accounting.home.homeaccounting.entities.OperationModify
import accounting.home.homeaccounting.entities.Subcategory
import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class NewOperationActivity : AppCompatActivity(), View.OnClickListener, AdapterView.OnItemClickListener, TextWatcher {
    private var mSelectedSubcategory: Subcategory? = null

    private var mAccountAdapter: ArrayAdapter<Account>? = null
    private var mSubcategoryAdapter: ArrayAdapter<Subcategory>? = null

    private var mOperationId: Int = 0
    private var mDate: LocalDate? = null

    private val mHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_newoperation)

        val add_operation = findViewById<Button>(R.id.add_operation)
        val subcategory_name = findViewById<AutoCompleteTextView>(R.id.subcategory_name)
        val account = findViewById<Spinner>(R.id.account)
        val second_account = findViewById<Spinner>(R.id.second_account)
        val network = findViewById<AutoCompleteTextView>(R.id.network)
        val type = findViewById<AutoCompleteTextView>(R.id.type)
        val amount = findViewById<EditText>(R.id.amount)
        val summa = findViewById<EditText>(R.id.summa)

        mOperationId = intent.getIntExtra("operationId", -1)
        val intDate = intent.getIntExtra("date", -1)
        mDate = LocalDate.of(intDate / 10000, intDate / 100 % 100, intDate % 100)
        add_operation.setOnClickListener(this)

        mSubcategoryAdapter = ArrayAdapter<Subcategory>(this, android.R.layout.simple_spinner_dropdown_item, SharedResources.db!!.getSubcategories())
        subcategory_name.setAdapter<ArrayAdapter<Subcategory>>(mSubcategoryAdapter)
        subcategory_name.threshold = 3
        subcategory_name.onItemClickListener = this
        subcategory_name.addTextChangedListener(this)

        mAccountAdapter = ArrayAdapter<Account>(this, R.layout.textview, SharedResources.db!!.activeAccounts)
        mAccountAdapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        account.adapter = mAccountAdapter

        second_account.adapter = mAccountAdapter

        var items: List<String>? = SharedResources.db!!.getHints("NETW")
        if (items != null) {
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
            network.setAdapter(adapter)
            network.threshold = 1
        }
        items = SharedResources.db!!.getHints("TYPE")
        if (items != null) {
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
            type.setAdapter(adapter)
            type.threshold = 1
        }

        mSelectedSubcategory = null
        if (mOperationId != -1) {
            add_operation.setText(R.string.modify_operation)
            val operation = SharedResources.operations!!.operations
                    .first { op -> op.id == mOperationId }
            val sc = SharedResources.db!!.getSubcategory(operation.subcategoryId)
            if (sc == null) {
                SharedResources.alert(this, "Unknown subcategory id: " + operation.subcategoryId)
                return
            }
            var position = mSubcategoryAdapter!!.getPosition(sc)
            if (position == -1) {
                SharedResources.alert(this, "Unknown subcategory id: " + operation.subcategoryId)
                return
            }
            subcategory_name.listSelection = position
            subcategory_name.setText(sc.toString())
            subcategory_name.performCompletion()
            mSelectedSubcategory = sc
            _updateProperties()

            val accountValue = SharedResources.db!!.getAccount(operation.accountId)
            if (accountValue == null) {
                SharedResources.alert(this, "Unknown account id: " + operation.accountId)
                return
            }
            position = mAccountAdapter!!.getPosition(accountValue)
            if (position == -1) {
                SharedResources.alert(this, "Unknown account id: " + operation.accountId)
                return
            }
            account.setSelection(position)

            amount.setText(SharedResources.db!!.formatMoney3(operation.amount))
            summa.setText(SharedResources.db!!.formatMoney(operation.summa))

            fillProperties(operation.finOpProperies)
        }
    }

    private fun fillProperties(finOpProperies: List<FinOpProperty>?) {
        if (finOpProperies != null) {
            val second_account = findViewById<Spinner>(R.id.second_account)
            val network = findViewById<AutoCompleteTextView>(R.id.network)
            val type = findViewById<AutoCompleteTextView>(R.id.type)
            val distance = findViewById<EditText>(R.id.distance)

            for (property in finOpProperies) {
                when (property.propertyCode) {
                    "SECA" -> {
                        val account = SharedResources.db!!.getAccount(property.numericValue!!.toInt())
                        if (account == null) {
                            SharedResources.alert(this, "Unknown account id: " + property.numericValue)
                            return
                        }
                        val position = mAccountAdapter!!.getPosition(account)
                        if (position == -1) {
                            SharedResources.alert(this, "Unknown account id: " + property.numericValue)
                            return
                        }
                        second_account.setSelection(position)
                    }
                    "DIST" -> distance.setText(property.numericValue.toString())
                    "NETW" -> network.setText(property.stringValue)
                    "TYPE" -> type.setText(property.stringValue)
                    else -> {
                        SharedResources.alert(this, "Unknown PropertyCode: " + property.propertyCode)
                        return
                    }
                }
            }
        }
    }

    private fun buildProperties(): Array<FinOpProperty>? {
        val second_account = findViewById<Spinner>(R.id.second_account)
        val network = findViewById<AutoCompleteTextView>(R.id.network)
        val type = findViewById<AutoCompleteTextView>(R.id.type)
        val distance = findViewById<EditText>(R.id.distance)

        if (mSelectedSubcategory!!.code == null) {
            return null
        }
        when (mSelectedSubcategory!!.code) {
            "EXCH", "TRFR" -> return arrayOf(FinOpProperty("SECA", BigDecimal((second_account.selectedItem as Account).id)))
            "FUEL" -> {
                val result = ArrayList<FinOpProperty>()
                if (distance.text.length > 0) {
                    result.add(FinOpProperty("DIST", BigDecimal(distance.text.toString())))
                }
                if (network.text.length > 0) {
                    result.add(FinOpProperty("NETW", network.text.toString()))
                }
                if (type.text.length > 0) {
                    result.add(FinOpProperty("TYPE", type.text.toString()))
                }
                return if (result.size > 0) result.toTypedArray() else null
            }
        }
        return null
    }

    override fun onClick(v: View) {
        val summa = findViewById<EditText>(R.id.summa)
        val account = findViewById<Spinner>(R.id.account)
        val amount = findViewById<EditText>(R.id.amount)

        if (summa.text.length == 0) {
            SharedResources.alert(this, R.string.empty_summa)
            return
        }
        if (mSelectedSubcategory == null) {
            SharedResources.alert(this, R.string.empty_subcategory)
            return
        }

        val call = if (mOperationId == -1)
            SharedResources.buildAddOperationService(
                    OperationAdd(mDate!!,
                            amount.text.toString(),
                            summa.text.toString(),
                            mSelectedSubcategory!!.id,
                            (account.selectedItem as Account).id,
                            buildProperties())
            )
        else
            SharedResources.buildModifyOperationService(
                    OperationModify(mDate!!,
                            mOperationId,
                            amount.text.toString(),
                            summa.text.toString(),
                            mSelectedSubcategory!!.id,
                            (account.selectedItem as Account).id,
                            buildProperties())
            )
        call.doInBackground(object : HomeAccountingService.Callback<String> {
            override fun deserialize(response: String): String {
                return response
            }

            override fun isString(): Boolean {
                return true
            }

            override fun onResponse(response: String) {
                mHandler.post {
                    if ("OK" != response) {
                        SharedResources.alert(this@NewOperationActivity, response)
                    } else {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }
            }

            override fun onFailure(t: Throwable?, response: String?) {
                mHandler.post {
                    if (t != null) {
                        SharedResources.alert(this@NewOperationActivity, t.message)
                    } else {
                        SharedResources.alert(this@NewOperationActivity, response)
                    }
                }
            }
        })
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        mSelectedSubcategory = parent.getItemAtPosition(position) as Subcategory
        _updateProperties()
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        if (mSelectedSubcategory != null) {
            val subcategory_name = findViewById<AutoCompleteTextView>(R.id.subcategory_name)
            mSelectedSubcategory = null
            subcategory_name.setText("")
        }
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
    }

    override fun afterTextChanged(s: Editable) {
    }

    private fun _updateProperties() {
        val parameter_label = findViewById<TextView>(R.id.parameter_label)
        val seca_label = findViewById<TextView>(R.id.seca_label)
        val dist_label = findViewById<TextView>(R.id.dist_label)
        val network_label = findViewById<TextView>(R.id.network_label)
        val type_label = findViewById<TextView>(R.id.type_label)
        val second_account = findViewById<Spinner>(R.id.second_account)
        val network = findViewById<AutoCompleteTextView>(R.id.network)
        val type = findViewById<AutoCompleteTextView>(R.id.type)
        val distance = findViewById<EditText>(R.id.distance)

        var showSecondAccount = false
        var showDistance = false
        if (mSelectedSubcategory != null) {
            showSecondAccount = "EXCH" == mSelectedSubcategory!!.code || "TRFR" == mSelectedSubcategory!!.code
            showDistance = "FUEL" == mSelectedSubcategory!!.code
        }
        parameter_label.visibility = if (showSecondAccount || showDistance) View.VISIBLE else View.GONE
        var visibility = if (showSecondAccount) View.VISIBLE else View.GONE
        seca_label.visibility = visibility
        second_account.visibility = visibility
        visibility = if (showDistance) View.VISIBLE else View.GONE
        dist_label.visibility = visibility
        distance.visibility = visibility
        network_label.visibility = visibility
        network.visibility = visibility
        type_label.visibility = visibility
        type.visibility = visibility
    }
}
