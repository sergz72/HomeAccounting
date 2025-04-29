package com.sz.homeaccounting2

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Base64
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
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.google.gson.Gson
import com.sz.file_server.lib.FileService
import com.sz.file_server.lib.FileServiceConfig
import com.sz.home_accounting.core.DB
import com.sz.home_accounting.core.HomeAccountingService
import com.sz.homeaccounting2.databinding.ActivityMainBinding
import com.sz.homeaccounting2.ui.operations.OperationsFragment
import com.sz.homeaccounting2.ui.operations.OperationsViewModel
import com.sz.homeaccounting2.ui.operations.OperationsViewModelFactory
import com.sz.homeaccounting2.ui.pin.PinActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs

private val Context.dataStore by preferencesDataStore(name = "settings")

data class Hints(val hints: Map<String, List<String>>)

class MainActivity : AppCompatActivity(), ActivityResultCallback<ActivityResult> {
    companion object {
        const val PREFS_NAME = "HomeAccounting2"
        private const val SETTINGS = 1
        internal const val NEWOPERATION = 2
        internal const val MODIFYOPERATION = 3
        internal const val PINCHECK = 4

        private var db: DB? = null

        private val mHintsKey = stringPreferencesKey("hints")

        private var _hints: Hints? = null

        val hints
            get() = _hints

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

        fun getFileServiceConfig(activity: AppCompatActivity): FileServiceConfig? {
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
            var userId = settings.getString("user_id", "")!!.toIntOrNull()
            if (userId == null) {
                alert(activity, "wrong User ID")
                return null
            }
            var dbName = settings.getString("db_name", "")!!
            if (dbName.isEmpty()) {
                alert(activity, "empty Database name")
                return null
            }
            val timeout = if (isWifiConnected(activity)) {2000} else {7000}
            return FileServiceConfig(
                userId = userId,
                key = activity.resources.openRawResource(R.raw.serverkey).readBytes(),
                hostName = serverAddress,
                port = port,
                timeoutMs = timeout,
                dbName = dbName
            )
        }

        fun buildOperationsViewModel(activity: AppCompatActivity, config: FileServiceConfig):
                Pair<FileService, OperationsViewModel> {
            val fileService = FileService(config)
            val service = HomeAccountingService(fileService, activity.resources.openRawResource(R.raw.key).readBytes())
            if (db == null) {
                db = DB(service)
            }

            val operationsViewModelFactory = OperationsViewModelFactory(db!!)
            val operationsViewModel = ViewModelProvider(activity, operationsViewModelFactory)[OperationsViewModel::class.java]

            return Pair(fileService, operationsViewModel)
        }
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var mActivityResultLauncher: ActivityResultLauncher<Intent>

    private lateinit var fileService: FileService
    private lateinit var _operationsViewModel: OperationsViewModel

    val operationsViewModel: OperationsViewModel
        get() = _operationsViewModel

    private lateinit var mAlert: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mAlert = statusAlert(this, "Loading...")

        val config = FileServiceConfig(
            userId = 1,
            key = resources.openRawResource(R.raw.serverkey).readBytes(),
            hostName = "127.0.0.1",
            port = 12345,
            timeoutMs = 1000,
            dbName = "db"
        )
        val (fileService, viewModel) = buildOperationsViewModel(this, config)
        this.fileService = fileService
        this._operationsViewModel = viewModel

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController
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

        binding.appBarMain.bnAdd.setOnClickListener {
            val fragment = navHostFragment.getChildFragmentManager().fragments[0]
            if (fragment is OperationsFragment) {
                fragment.add()
            }
        }

        loadHints()

        showPinActivity()
    }

    fun loadHints() {
        lifecycleScope.launch {
            val hintsString = dataStore.data
                .map { preferences ->
                    // No type safety.
                    preferences[mHintsKey] ?: ""
                }.first()
            _hints = if (hintsString.isNotEmpty()) {
                val decoded = Base64.decode(hintsString, Base64.DEFAULT).toString(Charsets.UTF_8)
                Gson().fromJson(decoded, Hints::class.java)
            } else {
                Hints(mapOf())
            }
        }
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
        val config = getFileServiceConfig(this) ?: return
        try {
            fileService.updateConfig(config)
            db?.resetDicts()
            refresh()
        } catch (e: Exception) {
            alert(this, e.message)
        }
    }

    private fun refresh() {
        operationsViewModel.refresh()
    }

    override fun onActivityResult(result: ActivityResult) {
        val requestCode = result.data!!.getIntExtra("code", -2)
        if (requestCode == SETTINGS && result.resultCode == RESULT_OK) {
            updateServer()
        } else if ((requestCode == NEWOPERATION || requestCode == MODIFYOPERATION) && result.resultCode == RESULT_OK) {
            Thread.sleep(1000)
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
