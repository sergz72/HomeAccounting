package accounting.home.homeaccounting

import accounting.home.homeaccounting.entities.FinanceTotalAndOperations
import android.app.DatePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import accounting.home.homeaccounting.entities.OperationDelete
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ExpandableListView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class OperationsFragment : Fragment(), IData, View.OnClickListener, OperationsViewAdapter.OpExecutor {
    companion object {
        val UI_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
        val CALL_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    private var mListener: OnFragmentInteractionListener? = null
    private var mDate: LocalDate = LocalDate.now()
    private var mOperationsViewAdapter: OperationsViewAdapter? = null
    private var mOperationsView: ExpandableListView? = null
    private var mBroadCastReceiver: MyBroadCastReceiver? = null

    private val mHandler = Handler(Looper.getMainLooper())

    private var mActivityResultLauncher: ActivityResultLauncher<Intent>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBroadCastReceiver = MyBroadCastReceiver()
        requireActivity().registerReceiver(mBroadCastReceiver, IntentFilter("refresh"))
        mActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(), activity as MainActivity)
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().unregisterReceiver(mBroadCastReceiver)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_operations, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val selectDate = requireActivity().findViewById<Button>(R.id.select_date)
        val datePrev = requireActivity().findViewById<Button>(R.id.date_prev)
        val dateNext = requireActivity().findViewById<Button>(R.id.date_next)
        val date = requireActivity().findViewById<TextView>(R.id.date)
        mOperationsView = requireActivity().findViewById(R.id.operations_view)

        selectDate.setOnClickListener(this)
        datePrev.setOnClickListener(this)
        dateNext.setOnClickListener(this)
        date.text = mDate.format(UI_DATE_FORMAT)

        mOperationsView!!.setAdapter(mOperationsViewAdapter)
    }

    override fun onAttach(context: Context) {
        mOperationsViewAdapter = OperationsViewAdapter(this.requireContext(), this)
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
        refresh()
    }

    override fun refresh() {
        try {
            Thread.sleep(500)
        } catch (e: InterruptedException) {
        }

        val call = SharedResources.buildOperationsService(mDate.format(CALL_DATE_FORMAT))
        call.doInBackground(object : HomeAccountingService.Callback<List<FinanceTotalAndOperations>> {
            override fun deserialize(response: String): List<FinanceTotalAndOperations> {
                return Gson().fromJson(response, object: TypeToken<List<FinanceTotalAndOperations>>(){}.type)
            }

            override fun isString(): Boolean {
                return false
            }

            override fun onResponse(response: List<FinanceTotalAndOperations>, compressed: ByteArray) {
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

    override fun modify(operationId: Int) {
        val intent = Intent(activity, NewOperationActivity::class.java)
        intent.putExtra("operationId", operationId)
        intent.putExtra("date", dateToInt())
        intent.putExtra("code", MainActivity.MODIFYOPERATION)
        mActivityResultLauncher!!.launch(intent)
    }

    override fun delete(operationId: Int) {
        SharedResources.confirm(requireActivity(), R.string.operation_delete_confirmation, realDelete(operationId))
    }

    private fun realDelete(operationId: Int): DialogInterface.OnClickListener {
        return DialogInterface.OnClickListener{ _, _ ->
            val call = SharedResources.buildDeleteOperationService(OperationDelete(mDate, operationId))
            call.doInBackground(object : HomeAccountingService.Callback<String> {
                override fun deserialize(response: String): String {
                    return response
                }

                override fun isString(): Boolean {
                    return true
                }

                override fun onResponse(response: String, compressed: ByteArray) {
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
        intent.putExtra("code", MainActivity.NEWOPERATION)
        mActivityResultLauncher!!.launch(intent)
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    private fun showResults(operations: List<FinanceTotalAndOperations>) {
        mOperationsViewAdapter!!.setData(operations)
        operations.indices.forEach { mOperationsView!!.expandGroup(it) }
    }

    override fun onClick(v: View) {
        val selectDate = requireActivity().findViewById<Button>(R.id.select_date)
        val datePrev = requireActivity().findViewById<Button>(R.id.date_prev)
        val dateNext = requireActivity().findViewById<Button>(R.id.date_next)

        if (v === selectDate) {
            val dialog = DatePickerDialog(requireActivity(), { _, year, monthOfYear, dayOfMonth ->
                mDate = LocalDate.of(year, monthOfYear + 1, dayOfMonth)
                updateDate()
            }, mDate.year, mDate.monthValue - 1, mDate.dayOfMonth)
            dialog.show()
        } else if (v === datePrev) {
            mDate = mDate.minusDays(1)
            updateDate()
        } else if (v === dateNext) {
            mDate = mDate.plusDays(1)
            updateDate()
        }
    }

    private fun updateDate() {
        val date = requireActivity().findViewById<TextView>(R.id.date)
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
