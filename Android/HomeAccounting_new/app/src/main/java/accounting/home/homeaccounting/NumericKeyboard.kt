package accounting.home.homeaccounting

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.TextView
import kotlin.math.max

class NumericKeyboard(ctx: Context, attrs: AttributeSet): GridLayout(ctx, attrs),
    View.OnClickListener {
    var textControl: TextView? = null
    var isMasked = false
    var value = ""

    init {
        val inflater = LayoutInflater.from(ctx)
        inflater.inflate(R.layout.numeric_keyboard, this)

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
        val dot = findViewById<Button>(R.id.dot)
        val plus = findViewById<Button>(R.id.plus)
        val minus = findViewById<Button>(R.id.minus)
        val mul = findViewById<Button>(R.id.mul)
        val div = findViewById<Button>(R.id.div)
        val lbrac = findViewById<Button>(R.id.lbrac)
        val rbrac = findViewById<Button>(R.id.rbrac)

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
        dot.setOnClickListener(this)
        plus.setOnClickListener(this)
        minus.setOnClickListener(this)
        mul.setOnClickListener(this)
        div.setOnClickListener(this)
        lbrac.setOnClickListener(this)
        rbrac.setOnClickListener(this)
    }

    fun clear() {
        value = ""
        textControl?.text = ""
    }

    override fun onClick(v: View?) {
        val text = (v as Button).text
        if (text == "<") {
            removeSymbol()
        } else {
            addSymbol(text)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun addSymbol(c: CharSequence) {
        if (textControl != null) {
            if (isMasked) {
                value += c
                textControl!!.text = textControl!!.text.toString() + '*'
            } else if (textControl is EditText) {
                val start = max(textControl!!.selectionStart, 0)
                val end = max(textControl!!.selectionEnd, 0)
                (textControl as EditText).text.replace(
                    start, end, c, 0, 1
                )
            }
        }
    }

    private fun removeSymbol() {
        if (textControl != null) {
            if (isMasked) {
                val l = value.length
                if (l > 0) {
                    value = value.substring(0, l - 1)
                    textControl!!.text = textControl!!.text.toString().substring(0, l - 1)
                }
            } else if (textControl is EditText) {
                val start = max(textControl!!.selectionStart, 0)
                val end = max(textControl!!.selectionEnd, 0)
                if (start != end) {
                    (textControl as EditText).text.replace(
                        start, end,
                        "", 0, 0
                    )
                } else if (start > 0) {
                    (textControl as EditText).text.replace(
                        start - 1, start,
                        "", 0, 0
                    )
                }
            }
        }
    }
}