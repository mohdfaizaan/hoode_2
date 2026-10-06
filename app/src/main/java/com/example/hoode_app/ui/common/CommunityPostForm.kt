package com.example.hoode_app.ui.common

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.example.hoode_app.R
import com.example.hoode_app.data.repository.HoodeRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

/** One retained draft owns its upload/save operation across rotation and retry. */
class CommunityPostForm : DialogFragment() {
    private val model:CommunityPostViewModel by viewModels()
    private val gallery get()=arguments?.getBoolean("gallery")==true
    private val picker=registerForActivityResult(ActivityResultContracts.GetContent()) { uri->uri?.let(model::selectPhoto) }

    override fun onCreateDialog(state:Bundle?):Dialog {
        val context=requireContext()
        val pad=(20*resources.displayMetrics.density).toInt()
        val form=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;setPadding(pad,pad/2,pad,pad)}
        val titleWrap=TextInputLayout(context).apply{hint=if(gallery)"Photo title" else "Post title"}
        val titleField=TextInputEditText(titleWrap.context).apply{setText(model.title);doAfterTextChanged{model.title=it.toString()}}
        titleWrap.addView(titleField);form.addView(titleWrap)
        val contentWrap=TextInputLayout(context).apply{hint=if(gallery)"Caption (optional)" else "Message"}
        val messageField=TextInputEditText(contentWrap.context).apply{
            minLines=3;inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setText(model.body);doAfterTextChanged{model.body=it.toString()}
        }
        contentWrap.addView(messageField);form.addView(contentWrap)
        val image=ImageView(context).apply{contentDescription="Selected photo preview";scaleType=ImageView.ScaleType.CENTER_CROP;visibility=View.GONE}
        form.addView(image,LinearLayout.LayoutParams(-1,pad*8))
        val choosePhoto=MaterialButton(context).apply{text=if(gallery)"Choose photo" else "Add photo (optional)";setOnClickListener{picker.launch("image/*")}}
        form.addView(choosePhoto)
        val feedback=TextView(context).apply{setText(R.string.submission_pending_explanation);accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE}
        form.addView(feedback)
        val editor=MaterialAlertDialogBuilder(context).setTitle(if(gallery)"Share a Hoode photo" else "New community post")
            .setView(ScrollView(context).apply{addView(form)}).setNegativeButton("Cancel",null).setPositiveButton(R.string.submit,null).create()
        editor.setCanceledOnTouchOutside(false)
        editor.setOnShowListener {
            editor.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                if(model.state.value.busy){model.cancel();return@setOnClickListener}
                if(model.title.isBlank() && model.body.isBlank() && model.state.value.photo==null)dismiss()
                else MaterialAlertDialogBuilder(context).setTitle("Discard this draft?")
                    .setMessage("Your title, message and selected photo will be cleared.")
                    .setNegativeButton("Keep editing",null).setPositiveButton("Discard"){_,_->dismiss()}.show()
            }
            editor.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                titleWrap.error=null;contentWrap.error=null
                if(model.title.isBlank()||model.title.trim().length>200){titleWrap.error="Enter a title up to 200 characters";titleField.requestFocus();return@setOnClickListener}
                if(!gallery&&model.body.isBlank()){contentWrap.error="Write your message";messageField.requestFocus();return@setOnClickListener}
                if(gallery&&model.state.value.photo==null){feedback.text="Choose a photo to submit.";return@setOnClickListener}
                model.submit(gallery,HoodeRepository.currentUser.value?.displayName.orEmpty())
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                var displayedPhoto:String?=null
                model.state.collect { status->
                    titleField.isEnabled=!status.busy;messageField.isEnabled=!status.busy;choosePhoto.isEnabled=!status.busy
                    editor.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled=!status.busy
                    editor.getButton(AlertDialog.BUTTON_NEGATIVE)?.text=if(status.busy)"Stop" else "Cancel"
                    isCancelable=!status.busy
                    feedback.text=status.message.ifBlank{getString(R.string.submission_pending_explanation)}
                    if(status.photo!=displayedPhoto){displayedPhoto=status.photo;image.visibility=if(status.photo==null)View.GONE else View.VISIBLE;image.load(status.photo)}
                    if(status.completed){
                        parentFragmentManager.setFragmentResult("community_saved",Bundle())
                        parentFragment?.showSubmissionPending()
                        dismiss()
                    }
                }
            }
        }
        return editor
    }

    companion object {
        fun show(host:Fragment,onSaved:()->Unit)=open(host,false,onSaved)
        fun showGallery(host:Fragment,onSaved:()->Unit)=open(host,true,onSaved)
        private fun open(host:Fragment,gallery:Boolean,onSaved:()->Unit) {
            if(host.childFragmentManager.findFragmentByTag("community_form")!=null)return
            host.childFragmentManager.setFragmentResultListener("community_saved",host.viewLifecycleOwner){_,_->onSaved()}
            CommunityPostForm().apply{arguments=Bundle().apply{putBoolean("gallery",gallery)}}.show(host.childFragmentManager,"community_form")
        }
    }
}
