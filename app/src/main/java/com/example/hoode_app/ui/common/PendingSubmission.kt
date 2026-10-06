package com.example.hoode_app.ui.common

import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.core.os.bundleOf
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.hoode_app.R
import com.hoodeconnect.backend.SubmissionAttempt
import com.hoodeconnect.backend.SubmissionErrors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

/** Keep the form and its draft open on errors; acknowledge only a committed server write. */
fun Fragment.submitForReview(button:View,dialog:Dialog,privateRequest:Boolean=false,save:suspend ()->Result<*>) {
    if(!button.isEnabled) return
    val attempt=(button.getTag(R.id.submission_attempt) as? SubmissionAttempt) ?: SubmissionAttempt().also { button.setTag(R.id.submission_attempt,it) }
    fun descendants(view:View):List<View> = listOf(view)+(if(view is ViewGroup)(0 until view.childCount).flatMap{descendants(view.getChildAt(it))}else emptyList())
    val controls=descendants(dialog.window?.decorView ?: button).associateWith{it.isEnabled}
    controls.keys.forEach{it.isEnabled=false}
    val oldLabel=(button as? TextView)?.text
    (button as? TextView)?.setText(R.string.submitting)
    dialog.setCancelable(false)
    viewLifecycleOwner.lifecycleScope.launch {
        val result=try { withContext(attempt){save()} }
            catch(cancelled:CancellationException){throw cancelled}
            catch(error:Exception){Result.failure<Any>(error)}
            finally {
                controls.forEach{(control,enabled)->control.isEnabled=enabled}
                (button as? TextView)?.text=oldLabel
                dialog.setCancelable(true)
            }
        if(result.isSuccess) {
            dialog.dismiss()
            showSubmissionPending(privateRequest)
        } else {
            MaterialAlertDialogBuilder(requireContext()).setTitle("Could not submit")
                .setMessage(SubmissionErrors.message(result.exceptionOrNull() ?: IllegalStateException("Please try again. Your details are still here.")))
                .setPositiveButton("Back to form",null).show()
        }
    }
}

fun Fragment.showSubmissionPending(privateRequest:Boolean=false) {
    MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.submission_pending)
        .setMessage(if(privateRequest)R.string.submission_private_explanation else R.string.submission_pending_explanation)
        .setPositiveButton("View status") { _,_->
            val navigation=findNavController()
            if(navigation.currentDestination?.id!=R.id.profileFragment)navigation.navigate(R.id.profileFragment,bundleOf("profile_tab" to 2))
        }.setNegativeButton("Done",null).show()
}

fun Fragment.saveAction(button:View,message:String,save:suspend ()->Result<*>) {
    if(!button.isEnabled)return
    button.isEnabled=false
    viewLifecycleOwner.lifecycleScope.launch {
        val result=save();button.isEnabled=true
        if(result.isSuccess)Toast.makeText(requireContext(),message,Toast.LENGTH_SHORT).show()
        else MaterialAlertDialogBuilder(requireContext()).setTitle("Could not save").setMessage(result.exceptionOrNull()?.message ?: "Please retry.").setPositiveButton("Close",null).show()
    }
}
