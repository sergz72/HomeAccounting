package accounting.home.homeaccounting

import android.view.View

class ViewAutoSize(parameters: List<AutoSizeParameters>, layoutWidth: Int, density: Float) {
    companion object {
        const val FILL = Int.MAX_VALUE
    }

    data class AutoSizeParameters(val viewId: Int, val minSize: Int, val maxSize: Int)

    private var mParameters: List<AutoSizeParameters> = parameters
    private var mMinTotalWidthInDp = parameters.sumOf { it.minSize }
    private var mMinMaxTotalWidthInDp =
        parameters.sumOf { if (it.maxSize == FILL) it.minSize else it.maxSize }
    private var mMinSizeOfFillItems = parameters.filter { it.maxSize == FILL }.sumOf { it.minSize }
    private lateinit var mWidths: Map<Int, Int>
    private var mDensity = density

    init {
        calculateWidths(layoutWidth)
    }

    private fun calculateWidths(layoutWidth: Int) {
        val widthInDp = (layoutWidth / mDensity).toInt()
        if (widthInDp <= mMinTotalWidthInDp) {
            mWidths = mParameters.associate { it.viewId to it.minSize }
        } else if (widthInDp >= mMinMaxTotalWidthInDp) {
            val delta = widthInDp - mMinMaxTotalWidthInDp
            mWidths = mParameters.associate { it.viewId to getWidth(it.minSize, it.maxSize, delta) }
        } else {
            var totalSize = mMinTotalWidthInDp
            mWidths = mutableMapOf()
            for (param in mParameters) {
                if (param.maxSize != FILL && totalSize < widthInDp) {
                    val delta = param.maxSize - param.minSize
                    totalSize += delta
                    if (totalSize < widthInDp) {
                        (mWidths as MutableMap)[param.viewId] = param.maxSize
                    } else {
                        (mWidths as MutableMap)[param.viewId] = param.minSize + widthInDp - (totalSize - delta)
                    }
                } else {
                    (mWidths as MutableMap)[param.viewId] = param.minSize
                }
            }
        }
    }

    private fun getWidth(minSize: Int, maxSize: Int, delta: Int): Int {
        if (maxSize != FILL) {
            return maxSize
        }
        return minSize + minSize * delta / mMinSizeOfFillItems
    }

    fun updateWidths(v :View) {
        mWidths.forEach { updateWidth(v, it.key, it.value) }
    }

    private fun updateWidth(v: View, resId: Int, newWidth: Int) {
        val childView = v.findViewById<View>(resId)
        val lp = childView.layoutParams
        lp.width = (newWidth * mDensity).toInt()
        childView.layoutParams = lp
    }
}