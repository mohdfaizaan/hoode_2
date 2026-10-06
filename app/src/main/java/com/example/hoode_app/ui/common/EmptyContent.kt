package com.example.hoode_app.ui.common

import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.hoode_app.R
import com.example.hoode_app.data.repository.HoodeRepository
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

/** Empty cached feeds offer a retry without claiming that a network request succeeded. */
fun LinearLayout.showEmptyContent(title: String, detail: String = "Refresh to check for the latest community updates.") {
    val dp = resources.displayMetrics.density
    val box = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding((22*dp).toInt(), (28*dp).toInt(), (22*dp).toInt(), (22*dp).toInt())
        setBackgroundResource(R.drawable.bg_profile_card)
    }
    box.addView(TextView(context).apply {
        text = title; textSize = 22f
        typeface = android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)
        setTextColor(context.getColor(R.color.text_primary))
    })
    box.addView(TextView(context).apply {
        text = detail; textSize = 15f
        setTextColor(context.getColor(R.color.text_secondary))
        setPadding(0, (10*dp).toInt(), 0, (12*dp).toInt())
    })
    box.addView(MaterialButton(context).apply {
        text = "Refresh"
        setOnClickListener {
            val owner = findViewTreeLifecycleOwner() ?: return@setOnClickListener
            isEnabled = false; text = "Refreshing…"
            owner.lifecycleScope.launch {
                try { HoodeRepository.syncWithCloud().join() }
                finally { isEnabled = true; text = "Refresh" }
            }
        }
    })
    addView(box, LinearLayout.LayoutParams(-1, -2))
}
