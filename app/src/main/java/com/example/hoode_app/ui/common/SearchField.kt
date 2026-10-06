package com.example.hoode_app.ui.common

import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.widget.doAfterTextChanged
import com.example.hoode_app.R

/** Clear local search immediately and return focus to the input. */
fun EditText.attachClearAction() {
    val row = parent as? LinearLayout ?: return
    val size = (48 * resources.displayMetrics.density).toInt()
    layoutParams = (layoutParams as LinearLayout.LayoutParams).apply { width = 0; weight = 1f }
    val clear = ImageButton(context).apply {
        setImageResource(R.drawable.ic_close)
        setColorFilter(context.getColor(R.color.text_secondary))
        contentDescription = "Clear search"
        background = null
        val inset = (14 * resources.displayMetrics.density).toInt()
        setPadding(inset, inset, inset, inset)
        visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
        setOnClickListener { setText(""); requestFocus() }
    }
    row.addView(clear, LinearLayout.LayoutParams(size, size))
    doAfterTextChanged { clear.visibility = if (it.isNullOrEmpty()) View.GONE else View.VISIBLE }
}
