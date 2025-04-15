package com.sz.homeaccounting2.ui.pin

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.sz.homeaccounting2.MainActivity
import com.sz.homeaccounting2.R
import androidx.core.content.edit

const val RESET_SEQUENCE = "(727348)"

class PinActivity: AppCompatActivity(), View.OnClickListener {
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

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                setResult(RESULT_CANCELED, intent)
                finish()
            }
        })
    }

    override fun onClick(v: View) {
        checkPin()
    }

    private fun checkPin() {
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
                    setResult(RESULT_CANCELED, intent)
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
                    mSettings.edit {
                        putString("pin", mKeyboard.value)
                    }
                } else {
                    mKeyboard.clear()
                    mSavedPin = ""
                    updatePinLabel()
                    errorMessage.text = resources.getString(R.string.wrongPin)
                    return
                }
            }
        }
        setResult(RESULT_OK, intent)
        finish()
    }
}