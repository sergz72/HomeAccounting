package accounting.home.homeaccounting

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

const val RESET_SEQUENCE = "(727348)"

class PinActivity: AppCompatActivity(), View.OnClickListener, View.OnLongClickListener {
    private var mPin: String? = null
    private var mSavedPin = ""
    private var mTries = 3
    private lateinit var mSettings: SharedPreferences
    private lateinit var mKeyboard: NumericKeyboard

    private fun updatePinLabel() {
        val pinLabel = findViewById<TextView>(R.id.pin_label)
        if (mSavedPin.isNotEmpty()) {
            pinLabel.text = resources.getString(R.string.enterPinAgain)
        } else if (mPin != null) {
            pinLabel.text = resources.getString(R.string.enterPin)
        } else {
            pinLabel.text = resources.getString(R.string.setPin)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)

        mSettings = getSharedPreferences(MainActivity.PREFS_NAME, 0)
        mPin = mSettings.getString("pin", null)

        updatePinLabel()

        mKeyboard = findViewById(R.id.keyboard)
        mKeyboard.textControl = findViewById(R.id.pin)
        mKeyboard.isMasked = true

        val ok = findViewById<Button>(R.id.ok)

        ok.setOnClickListener(this)
        ok.isLongClickable = true
        ok.setOnLongClickListener(this)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                setResult(Activity.RESULT_CANCELED, intent)
                finish()
            }
        })
    }

    override fun onClick(v: View) {
        checkPin(Activity.RESULT_OK)
    }

    override fun onLongClick(v: View?): Boolean {
        val ok = findViewById<Button>(R.id.ok)
        if (v == ok) {
            SharedResources.confirm(this, R.string.operation_update_dicts) { _, _ ->
                checkPin(Activity.RESULT_FIRST_USER)
            }
            return true
        }
        return false
    }

    private fun checkPin(resultCode: Int) {
        val errorMessage = findViewById<TextView>(R.id.errorMessage)
        if (mKeyboard.value.length < 4) {
            mKeyboard.clear()
            errorMessage.text = resources.getString(R.string.shortPin)
            return
        }
        if (mPin != null)
        {
            if (RESET_SEQUENCE == mKeyboard.value) {
                mPin = null
                mKeyboard.clear()
                updatePinLabel()
                errorMessage.text = ""
                return
            }
            if (mKeyboard.value != mPin)
            {
                if (mTries-- == 0) {
                    setResult(Activity.RESULT_CANCELED, intent)
                    finish()
                    return
                }
                mKeyboard.clear()
                errorMessage.text = resources.getString(R.string.wrongPin)
                return
            }
        } else {
            if (mSavedPin.isEmpty()) {
                mSavedPin = mKeyboard.value
                mKeyboard.clear()
                updatePinLabel()
                return
            } else {
                if (mSavedPin == mKeyboard.value) {
                    val editor = mSettings.edit()
                    editor.putString("pin", mKeyboard.value)
                    editor.apply()
                } else {
                    mKeyboard.clear()
                    mSavedPin = ""
                    updatePinLabel()
                    errorMessage.text = resources.getString(R.string.wrongPin)
                    return
                }
            }
        }
        setResult(resultCode, intent)
        finish()
    }
}