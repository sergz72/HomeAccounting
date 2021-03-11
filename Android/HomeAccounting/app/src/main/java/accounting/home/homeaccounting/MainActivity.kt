package accounting.home.homeaccounting

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem

import accounting.home.homeaccounting.entities.Dicts
import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Handler
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import java.util.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, OperationsFragment.OnFragmentInteractionListener {
    companion object {
        const val PREFS_NAME = "HomeAccounting"
        private const val SETTINGS = 1
        internal const val NEWOPERATION = 2
        internal const val MODIFYOPERATION = 3
        internal const val PINCHECK = 4
    }

    val mHandler = Handler()

    val mHistory = ArrayDeque<android.support.v4.app.Fragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(this.toolbar)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        HomeAccountingService.setupKey(resources.openRawResource(R.raw.key))

        showPinActivity()
    }

    private fun showPinActivity() {
        val intent = Intent(this, PinActivity::class.java)
        startActivityForResult(intent, PINCHECK)
    }

    private fun updateOrientation(fragment: android.support.v4.app.Fragment) {
        if (fragment is OperationsFragment) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
        } else if (resources.configuration.orientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    private fun showOperationsFragment() {
        if (SharedResources.db == null)
            updateDicts()
        if (SharedResources.db != null) {
            val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (fragment != null) {
                mHistory.push(fragment)
            }
            val newFragment = OperationsFragment()
            updateOrientation(newFragment)
            supportFragmentManager.beginTransaction().replace(R.id.fragment_container, newFragment).commit()
        }
    }

    fun showReportsFragment(parameters: ReportParameters) {
        if (SharedResources.db == null)
            updateDicts()
        if (SharedResources.db != null) {
            val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (fragment != null) {
                mHistory.push(fragment)
            }
            val fragment2 = ReportsFragment()
            fragment2.setParameters(parameters)
            updateOrientation(fragment2)
            supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment2).commit()
        }
    }

    private fun updateDicts() {
        val call = SharedResources.buildDictsService()
        call.execute(object : HomeAccountingService.Callback<Dicts> {
            override fun deserialize(response: String): Dicts {
                return Gson().fromJson(response, Dicts::class.java)
            }

            override fun isString(): Boolean {
                return false
            }

            override fun onResponse(response: Dicts) {
                SharedResources.buildDB(response)
                mHandler.post { showOperationsFragment() }
            }

            override fun onFailure(t: Throwable?, response: String?) {
                mHandler.post {
                    if (t != null) {
                        SharedResources.alert(this@MainActivity, t.message)
                    } else {
                        SharedResources.alert(this@MainActivity, response)
                    }
                }
            }
        })
    }

    private fun updateServer() {
        val settings = getSharedPreferences(PREFS_NAME, 0)
        val name = settings.getString("server_name", "localhost")!!

        SharedResources.setupServer(name)

        updateDicts()
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else if (!mHistory.isEmpty()) {
            val fragment = mHistory.pop()
            updateOrientation(fragment)
            supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    private fun refresh() {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment != null && fragment is IData) {
            (fragment as IData).refresh()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        val fragment: Any?

        when (id) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivityForResult(intent, SETTINGS)
                return true
            }
            R.id.action_refresh -> {
                refresh()
                return true
            }
            R.id.action_modify -> {
                fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (fragment != null && fragment is IData) {
                    (fragment as IData).modify()
                }
                return true
            }
            R.id.action_delete -> {
                fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (fragment != null && fragment is IData) {
                    (fragment as IData).delete()
                }
                return true
            }
            R.id.action_add -> {
                fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (fragment != null && fragment is IData) {
                    (fragment as IData).add()
                }
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val id = item.itemId

        when (id) {
            R.id.nav_operations -> showOperationsFragment()
            R.id.nav_reports -> showReportsFragment(ReportParameters.defaultParameters())
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onFragmentInteraction(uri: Uri) {

    }

    override fun onActivityResult(requestCodeIn: Int, resultCode: Int, data: Intent?) {
        var requestCode = requestCodeIn
        requestCode = requestCode and 0xFFFF
        if (requestCode == SETTINGS && resultCode == Activity.RESULT_OK) {
            updateServer()
        } else if ((requestCode == NEWOPERATION || requestCode == MODIFYOPERATION) && resultCode == Activity.RESULT_OK) {
            refresh()
        } else if (requestCode == PINCHECK) {
            if (resultCode == Activity.RESULT_OK) {
                updateServer()
            } else {
                finish()
            }
        }
    }
}
