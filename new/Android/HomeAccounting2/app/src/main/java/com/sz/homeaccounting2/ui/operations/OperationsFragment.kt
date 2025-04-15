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
import com.sz.homeaccounting2.IData
import com.sz.homeaccounting2.MainActivity
import com.sz.homeaccounting2.databinding.FragmentOperationsBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class OperationsFragment : Fragment(), IData, View.OnClickListener, OperationsViewAdapter.OpExecutor {
    companion object {
        val UI_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    }

    private var _binding: FragmentOperationsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var mActivityResultLauncher: ActivityResultLauncher<Intent>

    private lateinit var mOperationsViewAdapter: OperationsViewAdapter
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
        //viewModel = ViewModelProvider(this)[OperationsViewModel::class.java]

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

        mOperationsViewAdapter = OperationsViewAdapter(this.requireContext(), viewModel, this)
        binding.operationsView.setAdapter(mOperationsViewAdapter)

        refresh()

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun refresh() {
    }

    override fun add() {
    }

    override fun modify(operationId: Int) {
    }

    override fun delete(operationId: Int) {
    }

    override fun onClick(v: View?) {
    }
}