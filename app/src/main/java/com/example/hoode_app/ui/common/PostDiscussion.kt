package com.example.hoode_app.ui.common

import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.hoodeconnect.backend.CommunityApi
import kotlinx.coroutines.launch
import org.json.JSONObject

/** One persistent discussion flow for profile posts and approved gallery photos. */
object PostDiscussion {
    fun show(fragment:Fragment,table:String,id:String,title:String) {
        val context=fragment.requireContext()
        val pad=(20*context.resources.displayMetrics.density).toInt()
        val root=LinearLayout(context).apply { orientation=LinearLayout.VERTICAL; setPadding(pad,pad,pad,pad) }
        val status=TextView(context).apply { accessibilityLiveRegion=android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE }
        val comments=LinearLayout(context).apply { orientation=LinearLayout.VERTICAL }
        val like=MaterialButton(context).apply { text="Like"; isEnabled=false }
        val field=TextInputLayout(context).apply { hint="Write a comment" }
        val input=TextInputEditText(context).apply { minLines=2; maxLines=5; inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE }
        field.addView(input)
        val send=MaterialButton(context).apply { text="Post comment"; isEnabled=false }
        root.addView(status);root.addView(like)
        root.addView(ScrollView(context).apply { addView(comments) },LinearLayout.LayoutParams(-1,(220*context.resources.displayMetrics.density).toInt()))
        root.addView(field);root.addView(send)
        val dialog=AlertDialog.Builder(context).setTitle(title).setView(root).setNegativeButton("Close",null).create()
        var liked=false
        fun refresh() {
            status.text="Loading discussion…"
            fragment.viewLifecycleOwner.lifecycleScope.launch {
                CommunityApi.rpc("hoode_engagement",JSONObject().put("post_type",table).put("post_id",id)).onSuccess { raw ->
                    if(!dialog.isShowing) return@onSuccess
                    val data=JSONObject(raw); liked=data.optBoolean("liked")
                    like.text="${if(liked) "Unlike" else "Like"} · ${data.optInt("likes")}"
                    like.isEnabled=true;send.isEnabled=true;comments.removeAllViews()
                    val rows=data.optJSONArray("comments")
                    status.text=if(rows==null || rows.length()==0) "No comments yet. Start the conversation." else "Latest ${rows.length()} comments"
                    if(rows!=null) for(i in 0 until rows.length()) {
                        val row=rows.getJSONObject(i)
                        comments.addView(TextView(context).apply {
                            text="${row.optString("author_name")} · ${row.optString("created_at").take(10)}\n${row.optString("body")}"
                            setPadding(0,pad/2,0,pad/2);textSize=14f
                        })
                    }
                }.onFailure { status.text=it.message ?: "Unable to load. Close and retry." }
            }
        }
        like.setOnClickListener {
            like.isEnabled=false
            fragment.viewLifecycleOwner.lifecycleScope.launch {
                CommunityApi.like(table,id,!liked).onSuccess { refresh() }.onFailure { status.text=it.message;like.isEnabled=true }
            }
        }
        send.setOnClickListener {
            val body=input.text.toString().trim()
            if(body.isBlank() || body.length>2000) { field.error="Write between 1 and 2,000 characters";return@setOnClickListener }
            field.error=null;send.isEnabled=false
            fragment.viewLifecycleOwner.lifecycleScope.launch {
                CommunityApi.comment(table,id,body).onSuccess { input.text?.clear();refresh() }
                    .onFailure { field.error=it.message;send.isEnabled=true }
            }
        }
        dialog.show();refresh()
    }
}
