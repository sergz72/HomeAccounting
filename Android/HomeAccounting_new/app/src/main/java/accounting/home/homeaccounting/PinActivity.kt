package accounting.home.homeaccounting

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

const val RESET_SEQUENCE = "727348487273"
const val RESET_SEQUENCE2 = "487273"

class PinActivity: AppCompatActivity(), View.OnClickListener {
    private var mPin: String? = null
    private var mEnteredPin = ""
    private var mAllPin = ""
    private var mSavedPin = ""
    private var mTries = 3
    private lateinit var mSettings: SharedPreferences

    private fun updatePinLabel() {
        val pin_label = findViewById<TextView>(R.id.pin_label)
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

        val num0 = findViewById<Button>(R.id.num0)
        val num1 = findViewById<Button>(R.id.num1)
        val num2 = findViewById<Button>(R.id.num2)
        val num3 = findViewById<Button>(R.id.num3)
        val num4 = findViewById<Button>(R.id.num4)
        val num5 = findViewById<Button>(R.id.num5)
        val num6 = findViewById<Button>(R.id.num6)
        val num7 = findViewById<Button>(R.id.num7)
        val num8 = findViewById<Button>(R.id.num8)
        val num9 = findViewById<Button>(R.id.num9)
        val del = findViewById<Button>(R.id.del)
        val ok = findViewById<Button>(R.id.ok)

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
        val pin = findViewById<TextView>(R.id.pin)
        mEnteredPin += char
        mAllPin += char
        pin.text = pin.text.toString() + '*'
    }

    private fun clear() {
        val pin = findViewById<TextView>(R.id.pin)
        mAllPin = ""
        mEnteredPin = ""
        pin.text = ""
    }

    private fun del() {
        if (mEnteredPin.isNotEmpty())  {
            val pin = findViewById<TextView>(R.id.pin)
            mEnteredPin = mEnteredPin.substring(0, mEnteredPin.length - 1)
            pin.text = pin.text.toString().substring(0, pin.text.length - 1)
        }
    }

    override fun onClick(v: View) {
        val num0 = findViewById<Button>(R.id.num0)
        val num1 = findViewById<Button>(R.id.num1)
        val num2 = findViewById<Button>(R.id.num2)
        val num3 = findViewById<Button>(R.id.num3)
        val num4 = findViewById<Button>(R.id.num4)
        val num5 = findViewById<Button>(R.id.num5)
        val num6 = findViewById<Button>(R.id.num6)
        val num7 = findViewById<Button>(R.id.num7)
        val num8 = findViewById<Button>(R.id.num8)
        val num9 = findViewById<Button>(R.id.num9)
        val del = findViewById<Button>(R.id.del)
        val ok = findViewById<Button>(R.id.ok)

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
        val errorMessage = findViewById<TextView>(R.id.errorMessage)
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
                    editor.apply()
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