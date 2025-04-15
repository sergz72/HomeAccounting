package com.sz.homeaccounting2

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
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
import com.sz.home_accounting.core.entities.FinanceRecord
import com.sz.homeaccounting2.databinding.ActivityMainBinding
import com.sz.homeaccounting2.ui.operations.OperationsViewModel
import com.sz.homeaccounting2.ui.operations.OperationsViewModelFactory
import com.sz.homeaccounting2.ui.pin.PinActivity
import com.sz.smart_home.common.NetworkService

class MainActivity : AppCompatActivity(), ActivityResultCallback<ActivityResult> {
    companion object {
        const val PREFS_NAME = "HomeAccounting2"
        private const val SETTINGS = 1
        internal const val NEWOPERATION = 2
        internal const val MODIFYOPERATION = 3
        internal const val PINCHECK = 4

        fun alert(activity: Activity, message: String?) {
            val builder = AlertDialog.Builder(activity)
            builder.setMessage(message)
                .setTitle("Error")
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
            } catch (e: Exception) {
            }

            return true
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

    private val mHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val timeout = if (isWifiConnected(this)) {2000} else {7000}

        fileService = FileService(resources.openRawResource(R.raw.serverkey).readBytes(),
            "127.0.0.1", 12345, timeout, "home_accounting")
        val service = HomeAccountingService(fileService, resources.openRawResource(R.raw.key).readBytes())
        db = DB(service)

        val operationsViewModelFactory = OperationsViewModelFactory(db)
        _operationsViewModel = ViewModelProvider(this, operationsViewModelFactory)[OperationsViewModel::class.java]

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

        showPinActivity()
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
        val settings = getSharedPreferences(PREFS_NAME, 0)
        var serverAddress = settings.getString("server_name", "127.0.0.1")!!

        var port = 59998
        val serverAddressParts = serverAddress.split(':')
        if (serverAddressParts.size == 2) {
            serverAddress = serverAddressParts[0]
            val mayBePort = serverAddressParts[1].toIntOrNull()
            if (mayBePort != null) {
                port = mayBePort
            } else {
                alert(this, "wrong serverAddress")
                return
            }
        }

        fileService.updateServer(serverAddress, port)
        db.init(object: NetworkService.Callback<FinanceRecord> {
            override fun onResponse(response: FinanceRecord) {
                _operationsViewModel.setFinanceRecord(response)
            }

            override fun onFailure(t: Throwable) {
                mHandler.post { alert(this@MainActivity, t.message) }
            }
        })
    }

    private fun refresh() {
        //todo
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
}
