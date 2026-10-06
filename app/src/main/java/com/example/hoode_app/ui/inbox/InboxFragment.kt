package com.example.hoode_app.ui.inbox

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.hoode_app.R
import com.example.hoode_app.databinding.FragmentInboxBinding
import com.example.hoode_app.databinding.ItemConversationCardBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hoodeconnect.backend.BackendSession
import com.hoodeconnect.backend.CommunityApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class InboxFragment:Fragment() {
    private var _binding:FragmentInboxBinding?=null
    private val binding get()=_binding!!
    private val threads=mutableListOf<JSONObject>()
    private var job:Job?=null
    private var more=false
    private var activeDialog:androidx.appcompat.app.AlertDialog?=null
    override fun onCreateView(inflater:LayoutInflater,container:ViewGroup?,state:Bundle?):View {
        _binding=FragmentInboxBinding.inflate(inflater,container,false);return binding.root
    }
    override fun onViewCreated(view:View,state:Bundle?) { binding.chipGroupInbox.visibility=View.GONE }
    override fun onResume(){super.onResume();load(false)}
    private fun button(title:String,action:()->Unit)=MaterialButton(requireContext()).apply{text=title;setOnClickListener{action()}}
    private fun load(append:Boolean) {
        job?.cancel();binding.tvUnreadCounter.text="Loading…"
        job=viewLifecycleOwner.lifecycleScope.launch {
            CommunityApi.rpc("hoode_inbox",JSONObject().put("page_offset",if(append)threads.size else 0)).onSuccess { raw->
                val rows=JSONArray(raw);if(!append)threads.clear();repeat(rows.length()){threads.add(rows.getJSONObject(it))};more=rows.length()==30
                render();binding.tvUnreadCounter.text="${threads.size} conversations${if(more)" +" else ""}"
            }.onFailure {
                render();binding.tvUnreadCounter.text="Could not refresh"
                binding.llConversations.addView(TextView(requireContext()).apply{text=it.message ?: "Unable to load messages";setTextColor(requireContext().getColor(R.color.text_primary))})
            }
        }
    }
    private fun render() {
        binding.llConversations.removeAllViews();binding.emptyState.visibility=if(threads.isEmpty())View.VISIBLE else View.GONE
        binding.llConversations.addView(button("Refresh messages"){load(false)})
        threads.forEach { thread->
            val card=ItemConversationCardBinding.inflate(layoutInflater,binding.llConversations,false)
            card.tvSenderName.text=thread.optString("peer_name");card.tvMessagePreview.text=thread.optString("body")
            card.tvMessageTime.text=thread.optString("created_at").take(10);card.tvContextTag.text=thread.optString("title")
            card.indicatorUnread.visibility=View.GONE;card.ivSenderIcon.setImageResource(R.drawable.ic_classified)
            card.root.setOnClickListener{openThread(thread)};binding.llConversations.addView(card.root)
        }
        if(more)binding.llConversations.addView(button("Load more conversations"){load(true)})
    }
    private fun openThread(thread:JSONObject) {
        val box=LinearLayout(requireContext()).apply{orientation=LinearLayout.VERTICAL;setPadding(32,16,32,16)}
        val status=TextView(requireContext()).apply{setTextColor(requireContext().getColor(R.color.text_secondary));accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE}
        val transcript=LinearLayout(requireContext()).apply{orientation=LinearLayout.VERTICAL}
        val scroll=ScrollView(requireContext()).apply{addView(transcript)}
        box.addView(status);box.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        val messages=mutableListOf<JSONObject>();var offset=0;var loading=false
        val older=button("Load earlier messages"){};box.addView(older)
        val input=EditText(requireContext()).apply{hint="Write a reply";maxLines=4;filters=arrayOf(android.text.InputFilter.LengthFilter(2000));inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE}
        box.addView(input)
        val send=button("Send reply"){};box.addView(send)
        var messageId=UUID.randomUUID().toString()
        fun loadMessages(append:Boolean) {
            if(loading)return
            loading=true;older.isEnabled=false;status.text="Loading messages…"
            viewLifecycleOwner.lifecycleScope.launch {
                CommunityApi.rpc("hoode_message_thread",JSONObject().put("listing_id",thread.getString("item_id")).put("peer_id",thread.getString("peer_id")).put("page_offset",if(append)offset else 0))
                    .onSuccess { raw->val rows=JSONArray(raw);if(!append){messages.clear();offset=0};repeat(rows.length()){messages.add(rows.getJSONObject(it))};offset+=rows.length()
                        transcript.removeAllViews();messages.asReversed().forEach { m->transcript.addView(TextView(requireContext()).apply {
                            text="${if(m.optBoolean("is_mine"))"You" else thread.optString("peer_name")} · ${m.optString("created_at").take(16).replace('T',' ')}\n${m.optString("body")}\n"
                            textSize=15f;setTextColor(requireContext().getColor(R.color.text_primary));setPadding(0,12,0,12)
                        }) };older.visibility=if(rows.length()==50)View.VISIBLE else View.GONE;status.text="Private conversation"
                    }.onFailure {status.text=it.message ?: "Could not load messages. Retry.";older.visibility=View.VISIBLE;older.text="Retry loading"}
                loading=false;older.isEnabled=true
            }
        }
        older.setOnClickListener {loadMessages(messages.isNotEmpty())}
        send.setOnClickListener {
            val body=input.text.toString().trim();if(body.isBlank()){input.error="Enter a message";return@setOnClickListener}
            send.isEnabled=false;input.isEnabled=false;status.text="Sending…"
            viewLifecycleOwner.lifecycleScope.launch {
                CommunityApi.safely {CommunityApi.request("marketplace_messages?on_conflict=id","POST",JSONObject().put("id",messageId)
                    .put("item_id",thread.getString("item_id")).put("sender_id",BackendSession.userId).put("recipient_id",thread.getString("peer_id")).put("body",body),"resolution=ignore-duplicates,return=representation")}
                    .onSuccess {input.text.clear();messageId=UUID.randomUUID().toString();loadMessages(false);load(false)}
                    .onFailure {status.text=it.message ?: "Could not send. Your reply is still here."}
                send.isEnabled=true;input.isEnabled=true
            }
        }
        activeDialog=MaterialAlertDialogBuilder(requireContext()).setTitle(thread.optString("title")).setView(box).setNegativeButton("Close",null).create()
        activeDialog?.show();activeDialog?.window?.setLayout((resources.displayMetrics.widthPixels*0.94).toInt(),(resources.displayMetrics.heightPixels*0.85).toInt())
        loadMessages(false)
    }
    override fun onDestroyView(){job?.cancel();activeDialog?.dismiss();activeDialog=null;super.onDestroyView();_binding=null}
}
