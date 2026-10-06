package com.example.hoode_app.ui.common

import android.app.AlertDialog
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.hoode_app.R
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.DialogDonateContributionBinding
import com.example.hoode_app.databinding.DialogOurWorkBinding

object CommunityFeaturesHelper {

    fun showDonationDialog(context:Context,layoutInflater:LayoutInflater) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
            .setTitle("Community contributions")
            .setMessage("Online contributions are not available yet. The community team will share verified payment details when this service opens.")
            .setPositiveButton("Close",null).show()
    }

    fun showOurWorkDialog(context: Context, layoutInflater: LayoutInflater) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = DialogOurWorkBinding.inflate(layoutInflater)
        dialog.setContentView(binding.root)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.94).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        binding.btnCloseOurWork.setOnClickListener {
            dialog.dismiss()
        }

        fun openUrl(url: String) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open browser for $url", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnVisitFlowersofquran.setOnClickListener { openUrl("https://flowersofquran.com") }
        binding.btnVisitPearlsofquran.setOnClickListener { openUrl("https://pearlsofquran.com") }
        binding.btnVisitAayaatulquran.setOnClickListener { openUrl("https://aayaatulquran.com") }
        binding.btnVisitIllmbee.setOnClickListener { openUrl("https://illmbee.com") }
        binding.btnVisitCharacterbee.setOnClickListener { openUrl("https://characterbee.com") }
        binding.btnVisitAayaat.setOnClickListener { openUrl("https://aayaat.com") }
        binding.btnVisitIwillinshallah.setOnClickListener { openUrl("https://iwillinshallah.com") }

        dialog.show()
    }
}
