package accounting.home.homeaccounting

import android.app.DatePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import accounting.home.homeaccounting.entities.OperationDelete
import accounting.home.homeaccounting.entities.Operations
import android.os.Handler
import com.google.gson.Gson

import kotlinx.android.synthetic.main.fragment_operations.*

class OperationsFragment : Fragment(), IData, View.OnClickListener {
    companion object {
        val UI_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
        val CALL_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    private var mListener: OnFragmentInteractionListener? = null
    private var mDate: LocalDate = LocalDate.now()
    private var mOperationsViewAdapter: OperationsViewAdapter? = null
    private var mTotalsViewAdapter: TotalsViewAdapter? = null
    private var mOperations: Operations? = null
    private var mBroadCastReceiver: MyBroadCastReceiver? = null

    private val mHandler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBroadCastReceiver = MyBroadCastReceiver()
        activity!!.registerReceiver(mBroadCastReceiver, IntentFilter("refresh"))
    }

    override fun onDestroy() {
        super.onDestroy()
        activity!!.unregisterReceiver(mBroadCastReceiver)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_operations, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        select_date.setOnClickListener(this)
        date_prev.setOnClickListener(this)
        date_next.setOnClickListener(this)
        date.text = mDate.format(UI_DATE_FORMAT)

        operations_view.hasFixedSize()
        val lm = LinearLayoutManager(this.context)
        lm.orientation = LinearLayoutManager.VERTICAL
        operations_view.layoutManager = lm
        operations_view.adapter = mOperationsViewAdapter

        flow_view.hasFixedSize()
        flow_view.layoutManager = LinearLayoutManager(this.context)
        flow_view.adapter = mTotalsViewAdapter
    }

    override fun onAttach(context: Context?) {
        mOperationsViewAdapter = OperationsViewAdapter()
        mTotalsViewAdapter = TotalsViewAdapter()
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
        }
        refresh()
    }

    override fun refresh() {
        try {
            Thread.sleep(500)
        } catch (e: InterruptedException) {
        }

        val call = SharedResources.buildOperationsService(mDate.format(CALL_DATE_FORMAT))
        call.execute(object : HomeAccountingService.Callback<Operations> {
            override fun deserialize(response: String): Operations {
                return Gson().fromJson(response, Operations::class.java)
            }

            override fun isString(): Boolean {
                return false
            }

            override fun onResponse(response: Operations) {
                SharedResources.operations = response
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

    private fun dateToInt(): Int {
        return mDate.year * 10000 + mDate.month.value * 100 + mDate.dayOfMonth
    }

    override fun modify() {
        if (mOperations != null) {
            val selectedPosition = mOperationsViewAdapter!!.selectedPosition
            if (selectedPosition < mOperations!!.operations.size) {
                val operationId = mOperations!!.operations[selectedPosition].id
                val intent = Intent(activity, NewOperationActivity::class.java)
                intent.putExtra("operationId", operationId)
                intent.putExtra("date", dateToInt())
                startActivityForResult(intent, MainActivity.MODIFYOPERATION)
            }
        }
    }

    override fun delete() {
        if (mOperations != null) {
            val selectedPosition = mOperationsViewAdapter!!.selectedPosition
            if (selectedPosition < mOperations!!.operations.size) {
                SharedResources.confirm(activity!!, R.string.operation_delete_confirmation, delete(selectedPosition))
            }
        }
    }

    private fun delete(selectedPosition: Int): DialogInterface.OnClickListener {
        return DialogInterface.OnClickListener{ _, _ ->
            val operationId = mOperations!!.operations[selectedPosition].id
            val call = SharedResources.buildDeleteOperationService(OperationDelete(mDate, operationId))
            call.execute(object : HomeAccountingService.Callback<String> {
                override fun deserialize(response: String): String {
                    return response
                }

                override fun isString(): Boolean {
                    return true
                }

                override fun onResponse(response: String) {
                    mHandler.post {
                        if ("OK" != response) {
                            SharedResources.alert(activity!!, response)
                        } else {
                            val intent = Intent("refresh")
                            activity!!.sendBroadcast(intent)
                        }
                    }
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
    }

    override fun add() {
        val intent = Intent(activity, NewOperationActivity::class.java)
        intent.putExtra("date", dateToInt())
        startActivityForResult(intent, MainActivity.NEWOPERATION)
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    private fun showResults(operations: Operations) {
        mOperations = operations
        mOperationsViewAdapter!!.setData(operations.operations)
        mOperationsViewAdapter!!.notifyDataSetChanged()
        mTotalsViewAdapter!!.setData(operations.totals
                .sortedBy { SharedResources.db!!.getAccount(it.accountId)!!.name }
                .toList())
        mTotalsViewAdapter!!.notifyDataSetChanged()
    }

    override fun onClick(v: View) {
        if (v === select_date) {
            val dialog = DatePickerDialog(activity!!, DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                mDate = LocalDate.of(year, monthOfYear + 1, dayOfMonth)
                updateDate()
            }, mDate.year, mDate.monthValue - 1, mDate.dayOfMonth)
            dialog.show()
        } else if (v === date_prev) {
            mDate = mDate.minusDays(1)
            updateDate()
        } else if (v === date_next) {
            mDate = mDate.plusDays(1)
            updateDate()
        }
    }

    private fun updateDate() {
        date.text = mDate.format(UI_DATE_FORMAT)
        refresh()
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    private inner class MyBroadCastReceiver : BroadcastReceiver() {
        override fun onReceive(arg0: Context, intent: Intent) {
            if (intent.action != null && intent.action!!.equals("refresh", ignoreCase = true)) {
                refresh()
            }
        }
    }
}
