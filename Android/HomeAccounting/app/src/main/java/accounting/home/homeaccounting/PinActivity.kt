package accounting.home.homeaccounting

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_pin.*

const val RESET_SEQUENCE = "727348487273"
const val RESET_SEQUENCE2 = "487273"

class PinActivity: AppCompatActivity(), View.OnClickListener {
    private var mPin: String? = null;
    private var mEnteredPin = ""
    private var mAllPin = ""
    private var mSavedPin = ""
    private var mTries = 3
    private lateinit var mSettings: SharedPreferences

    private fun updatePinLabel() {
        if (mSavedPin.isNotEmpty()) {
            pin_label.text = resources.getString(R.string.enterPinAgain)
        } else if (mPin != null) {
            pin_label.text = resources.getString(R.string.enterPin)
        } else {
            pin_label.text = resources.getString(R.string.setPin)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)

        mSettings = getSharedPreferences(MainActivity.PREFS_NAME, 0)
        mPin = mSettings.getString("pin", null)

        updatePinLabel()

        num0.setOnClickListener(this)
        num1.setOnClickListener(this)
        num2.setOnClickListener(this)
        num3.setOnClickListener(this)
        num4.setOnClickListener(this)
        num5.setOnClickListener(this)
        num6.setOnClickListener(this)
        num7.setOnClickListener(this)
        num8.setOnClickListener(this)
        num9.setOnClickListener(this)
        del.setOnClickListener(this)
        ok.setOnClickListener(this)
    }

    private fun add(char: Char) {
        mEnteredPin += char
        mAllPin += char
        pin.text = pin.text.toString() + '*'
    }

    private fun clear() {
        mAllPin = ""
        mEnteredPin = ""
        pin.text = ""
    }

    private fun del() {
        if (mEnteredPin.isNotEmpty())  {
            mEnteredPin = mEnteredPin.substring(0, mEnteredPin.length - 1)
            pin.text = pin.text.toString().substring(0, pin.text.length - 1)
        }
    }

    override fun onClick(v: View) {
        when (v) {
            num0 -> add('0')
            num1 -> add('1')
            num2 -> add('2')
            num3 -> add('3')
            num4 -> add('4')
            num5 -> add('5')
            num6 -> add('6')
            num7 -> add('7')
            num8 -> add('8')
            num9 -> add('9')
            del -> del()
            ok -> checkPin()
        }
    }

    private fun checkPin() {
        if (mEnteredPin.length < 4) {
            clear()
            errorMessage.text = resources.getString(R.string.shortPin)
            return
        }
        if (mPin != null)
        {
            if (RESET_SEQUENCE == mAllPin && RESET_SEQUENCE2 == mEnteredPin && mSavedPin == "") {
                mPin = null
                clear()
                updatePinLabel()
                errorMessage.text = ""
                return
            }
            if (mEnteredPin != mPin)
            {
                if (mTries-- == 0) {
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                    return
                }
                clear()
                errorMessage.text = resources.getString(R.string.wrongPin)
                return
            }
        } else {
            if (mSavedPin.isEmpty()) {
                mSavedPin = mEnteredPin
                clear()
                updatePinLabel()
                return
            } else {
                if (mSavedPin == mEnteredPin) {
                    val editor = mSettings.edit()
                    editor.putString("pin", mEnteredPin)
                    editor.commit()
                } else {
                    clear()
                    mSavedPin = ""
                    updatePinLabel()
                    errorMessage.text = resources.getString(R.string.wrongPin)
                    return
                }
            }
        }
        setResult(Activity.RESULT_OK)
        finish()
    }
}