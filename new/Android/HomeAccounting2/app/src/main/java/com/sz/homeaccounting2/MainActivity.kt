package com.sz.homeaccounting2

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.sz.file_server.lib.FileService
import com.sz.home_accounting.core.DB
import com.sz.home_accounting.core.HomeAccountingService
import com.sz.homeaccounting2.databinding.ActivityMainBinding
import com.sz.homeaccounting2.ui.operations.OperationsViewModel
import com.sz.homeaccounting2.ui.operations.OperationsViewModelFactory
import com.sz.homeaccounting2.ui.pin.PinActivity
import java.time.LocalDate
import kotlin.math.abs

class MainActivity : AppCompatActivity(), ActivityResultCallback<ActivityResult> {
    companion object {
        const val PREFS_NAME = "HomeAccounting2"
        private const val SETTINGS = 1
        internal const val NEWOPERATION = 2
        internal const val MODIFYOPERATION = 3
        internal const val PINCHECK = 4

        fun statusAlert(activity: Activity, message: String?): AlertDialog {
            val builder = AlertDialog.Builder(activity)
            builder.setMessage(message)
                .setTitle(R.string.status)
            return builder.create()
        }

        fun alert(activity: Activity, message: String?) {
            val builder = AlertDialog.Builder(activity)
            builder.setMessage(message)
                .setTitle(R.string.error)
            val dialog = builder.create()
            dialog.show()
        }

        fun alert(activity: Activity, messageId: Int) {
            val builder = AlertDialog.Builder(activity)
            builder.setMessage(messageId)
                .setTitle(R.string.error)
            val dialog = builder.create()
            dialog.show()
        }

        private fun isWifiConnected(context: Activity): Boolean {
            try {
                val cm = context.getSystemService(CONNECTIVITY_SERVICE)
                if (cm is ConnectivityManager) {
                    val n = cm.activeNetwork ?: return false
                    val cp = cm.getNetworkCapabilities(n)
                    return cp != null && cp.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                }
            } catch (_: Exception) {
            }

            return true
        }

        fun getIntDate(date: LocalDate): Int {
            return date.year * 10000 + date.monthValue * 100 + date.dayOfMonth
        }

        @SuppressLint("DefaultLocale")
        fun formatMoney(value: Long): String {
            return String.format("%d.%02d", value / 100, abs(value % 100))
        }

        @SuppressLint("DefaultLocale")
        fun formatMoney3(value: Long?): String {
            return if (value == null) {
                ""
            } else String.format("%d.%03d", value / 1000, abs(value % 1000))
        }

        fun confirm(activity: Activity, messageId: Int, listener: DialogInterface.OnClickListener) {
            val builder = AlertDialog.Builder(activity)
            builder.setMessage(messageId)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.confirmation)
                .setPositiveButton(android.R.string.ok, listener)
                .setNegativeButton(android.R.string.cancel, null)
            val dialog = builder.create()
            dialog.show()
        }

        fun getServerAndPort(activity: AppCompatActivity): Pair<String, Int>? {
            val settings = activity.getSharedPreferences(PREFS_NAME, 0)
            var serverAddress = settings.getString("server_name", "127.0.0.1")!!

            var port = 59998
            val serverAddressParts = serverAddress.split(':')
            if (serverAddressParts.size == 2) {
                serverAddress = serverAddressParts[0]
                val mayBePort = serverAddressParts[1].toIntOrNull()
                if (mayBePort != null) {
                    port = mayBePort
                } else {
                    alert(activity, "wrong serverAddress")
                    return null
                }
            }
            return Pair(serverAddress, port)
        }

        fun buildOperationsViewModel(activity: AppCompatActivity, serverAddress: String, port: Int):
                Triple<FileService, DB, OperationsViewModel> {
            val timeout = if (isWifiConnected(activity)) {2000} else {7000}

            val fileService = FileService(activity.resources.openRawResource(R.raw.serverkey).readBytes(),
                serverAddress, port, timeout, "home_accounting")
            val service = HomeAccountingService(fileService, activity.resources.openRawResource(R.raw.key).readBytes())
            val db = DB(service)

            val operationsViewModelFactory = OperationsViewModelFactory(db)
            val operationsViewModel = ViewModelProvider(activity, operationsViewModelFactory)[OperationsViewModel::class.java]

            return Triple(fileService, db, operationsViewModel)
        }
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var mActivityResultLauncher: ActivityResultLauncher<Intent>

    private lateinit var fileService: FileService
    private lateinit var db: DB
    private lateinit var _operationsViewModel: OperationsViewModel

    val operationsViewModel: OperationsViewModel
        get() = _operationsViewModel

    private lateinit var mAlert: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mAlert = statusAlert(this, "Loading...")

        val (fileService, db, viewModel) = buildOperationsViewModel(this, "127.0.0.1", 12345)
        this.fileService = fileService
        this.db = db
        this._operationsViewModel = viewModel

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_operations
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        mActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(), this)

        _operationsViewModel.uiState.observe(this) { state ->
            when (state) {
                is OperationsViewModel.UiState.Loading -> showLoading()
                is OperationsViewModel.UiState.Success -> hideLoading()
                is OperationsViewModel.UiState.Error -> showError(state.message)
            }
        }

        showPinActivity()
    }

    private fun showLoading() {
        mAlert.setMessage(getString(R.string.loading))
        mAlert.show()
    }

    private fun hideLoading() {
        mAlert.hide()
    }

    private fun showError(message: String) {
        mAlert.setMessage(message)
        mAlert.show()
    }

    private fun showPinActivity() {
        val intent = Intent(this, PinActivity::class.java)
        intent.putExtra("code", PINCHECK)
        mActivityResultLauncher.launch(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun updateServer() {
        val (serverAddress, port) = getServerAndPort(this) ?: return
        fileService.updateServer(serverAddress, port)
        db.dicts = null
        refresh()
    }

    private fun refresh() {
        operationsViewModel.refresh()
    }

    override fun onActivityResult(result: ActivityResult) {
        val requestCode = result.data!!.getIntExtra("code", -2)
        if (requestCode == SETTINGS && result.resultCode == RESULT_OK) {
            updateServer()
        } else if ((requestCode == NEWOPERATION || requestCode == MODIFYOPERATION) && result.resultCode == RESULT_OK) {
            refresh()
        } else if (requestCode == PINCHECK) {
            when (result.resultCode) {
                RESULT_OK -> {
                    updateServer()
                }
                else -> {
                    finish()
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                intent.putExtra("code", SETTINGS)
                mActivityResultLauncher.launch(intent)
                return true
            }
            R.id.action_refresh -> {
                refresh()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
