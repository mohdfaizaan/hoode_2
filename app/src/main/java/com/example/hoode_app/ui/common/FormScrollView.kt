package com.example.hoode_app.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ScrollView

/** Leaves space for a dialog's title and actions; unlike ScrollView, honors a height cap. */
class FormScrollView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ScrollView(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val screenLimit = (resources.displayMetrics.heightPixels * 0.58f).toInt()
        val parentLimit = if (View.MeasureSpec.getMode(heightMeasureSpec) == View.MeasureSpec.UNSPECIFIED)
            screenLimit else View.MeasureSpec.getSize(heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(minOf(screenLimit, parentLimit), View.MeasureSpec.AT_MOST))
    }
}
