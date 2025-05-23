package com.sz.homeaccounting2.ui.operations

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.sz.homeaccounting2.MainActivity
import com.sz.homeaccounting2.R
import com.sz.homeaccounting2.databinding.FragmentOperationsBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity

class OperationsFragment : Fragment(), View.OnClickListener, OperationsViewAdapter.OpExecutor {
    companion object {
        val UI_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    }

    private var _binding: FragmentOperationsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var mActivityResultLauncher: ActivityResultLauncher<Intent>

    private lateinit var viewModel: OperationsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(), activity as MainActivity)
        viewModel = (this.activity as MainActivity).operationsViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOperationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.date
        viewModel.date.observe(viewLifecycleOwner) {
            textView.text = it.format(UI_DATE_FORMAT)
        }

        binding.selectDate.setOnClickListener {
            val dialog = DatePickerDialog(requireActivity(), { _, year, monthOfYear, dayOfMonth ->
                viewModel.setDate(LocalDate.of(year, monthOfYear + 1, dayOfMonth))
            }, viewModel.date.value!!.year, viewModel.date.value!!.monthValue - 1,
                viewModel.date.value!!.dayOfMonth)
            dialog.show()
        }
        binding.datePrev.setOnClickListener {
            viewModel.prevDate()
        }
        binding.dateNext.setOnClickListener {
            viewModel.nextDate()
        }

        val operationsViewAdapter = OperationsViewAdapter(this.requireContext(), viewModel, this)
        binding.operationsView.setAdapter(operationsViewAdapter)

        viewModel.operations.observe(context as AppCompatActivity) { ops ->
            operationsViewAdapter.setOperations(ops)
            ops.indices.forEach { binding.operationsView.expandGroup(it) }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun add() {
        val intent = Intent(activity, NewOperationActivity::class.java)
        intent.putExtra("code", MainActivity.NEWOPERATION)
        mActivityResultLauncher.launch(intent)
    }

    override fun modify(operationId: Long) {
        val opId = viewModel.findOperationId((operationId / 1000).toInt(), (operationId % 1000).toInt())
        val intent = Intent(activity, NewOperationActivity::class.java)
        intent.putExtra("operationId", opId)
        intent.putExtra("code", MainActivity.MODIFYOPERATION)
        mActivityResultLauncher.launch(intent)
    }

    override fun delete(operationId: Long) {
        MainActivity.confirm(requireActivity(), R.string.operation_delete_confirmation, realDelete(operationId))
    }

    private fun realDelete(operationId: Long): DialogInterface.OnClickListener {
        return DialogInterface.OnClickListener { _, _ ->
            val opId = viewModel.findOperationId((operationId / 1000).toInt(), (operationId % 1000).toInt())
            viewModel.deleteOperation(opId)
        }
    }

    override fun onClick(v: View?) {
    }
}