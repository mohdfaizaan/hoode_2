package com.example.hoode_app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.hoode_app.R
import com.example.hoode_app.data.model.User
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentProfileBinding
import com.example.hoode_app.ui.common.PostDiscussion
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.hoodeconnect.backend.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding?=null
    private val binding get()=_binding!!
    private val submissions=mutableListOf<OwnSubmission>()
    private var moreAvailable=false
    private var loadJob:Job?=null
    private var loading=false
    private var totals=org.json.JSONObject()
    private var errorMessage:String?=null
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    override fun onCreateView(inflater:LayoutInflater,container:ViewGroup?,state:Bundle?):View {
        _binding=FragmentProfileBinding.inflate(inflater,container,false);return binding.root
    }
    override fun onViewCreated(view:View,state:Bundle?) {
        binding.btnEditProfile.setOnClickListener { findNavController().navigate(R.id.editProfileFragment) }
        binding.btnCompleteProfile.setOnClickListener { findNavController().navigate(R.id.editProfileFragment) }
        binding.cardResidentDetails.setOnClickListener { findNavController().navigate(R.id.editProfileFragment) }
        binding.btnSettings.setOnClickListener { findNavController().navigate(R.id.settingsFragment) }
        binding.profileTabs.removeAllTabs()
        listOf("Posts","Community","Activity").forEach { binding.profileTabs.addTab(binding.profileTabs.newTab().setText(it)) }
        binding.profileTabs.addOnTabSelectedListener(object:TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab:TabLayout.Tab?) { load(false) }
            override fun onTabUnselected(tab:TabLayout.Tab?) {}
            override fun onTabReselected(tab:TabLayout.Tab?) { load(false) }
        })
        binding.profileTabs.getTabAt((state?.getInt("profile_tab") ?: arguments?.getInt("profile_tab",0) ?: 0).coerceIn(0,2))?.select()
        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.currentUser.collectLatest { user->bindUserProfile(user);updateProfileCompletion(user);load(false) }
        }
    }
    override fun onResume() { super.onResume();if(_binding!=null)load(false) }
    private fun load(append:Boolean) {
        loadJob?.cancel();loading=true;errorMessage=null
        if(!append)submissions.clear()
        renderFeed()
        loadJob=viewLifecycleOwner.lifecycleScope.launch {
            if(!append)CommunityApi.rpc("hoode_profile_summary").onSuccess { totals=org.json.JSONObject(it) }
            CommunityApi.mine(if(append)submissions.size else 0, listOf("posts","community","activity")[binding.profileTabs.selectedTabPosition.coerceAtLeast(0)]).onSuccess { rows->submissions.addAll(rows);moreAvailable=rows.size==30 }
                .onFailure { errorMessage=it.message ?: "Could not load submissions. Please retry." }
            loading=false;if(_binding!=null)renderFeed()
        }
    }
    private fun label(value:String,size:Float=14f)=TextView(requireContext()).apply {
        text=value;textSize=size;setTextColor(requireContext().getColor(R.color.profile_text_main));setPadding(0,dp(6),0,dp(6))
    }
    private fun action(value:String,block:()->Unit)=MaterialButton(requireContext()).apply { text=value;setOnClickListener { block() } }
    private fun renderFeed() {
        if(_binding==null)return
        val container=binding.feedContainer;container.removeAllViews()
        binding.tvStatPosts.text=totals.optInt("total").toString()
        binding.tvStatConnections.text=totals.optInt("pending").toString()
        binding.tvStatContributions.text=totals.optInt("approved").toString()
        if(loading)container.addView(label("Loading your submissions…").apply{accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE})
        errorMessage?.let { container.addView(label(it));container.addView(action("Retry"){load(false)}) }
        val tab=binding.profileTabs.selectedTabPosition
        val visible=submissions.filter { when(tab) {1->it.kind=="community_update";2->true;else->it.kind !in setOf("job_application","team_registration","correction","donor")} }
        if(tab==1)container.addView(action("New community post"){com.example.hoode_app.ui.common.CommunityPostForm.show(this){load(false)}})
        else container.addView(action(if(tab==2) "Create a request" else "Create a post"){findNavController().navigate(R.id.createFragment)})
        if(!loading && errorMessage==null && visible.isEmpty())container.addView(label(when(tab){1->"Your community posts will appear here, including posts waiting for approval.";2->"No activity yet. Your requests and review decisions will appear here.";else->"No posts yet. Add a listing, photo, event or report to get started."}))
        visible.forEach { post->
            val card=LinearLayout(requireContext()).apply { orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(12),dp(16),dp(12));setBackgroundResource(R.drawable.bg_profile_card) }
            card.addView(label("${CommunityApi.kindLabel(post.kind)} · ${post.createdAt.take(10)}",12f))
            card.addView(label(post.title,18f));card.addView(label(post.statusLabel,13f))
            if(post.status=="pending")card.addView(label(getString(if(post.kind in setOf("job_application","team_registration","correction"))R.string.submission_private_explanation else R.string.submission_pending_explanation),13f))
            if(post.reason.isNotBlank())card.addView(label("Review note: ${post.reason}"))
            card.addView(label(post.detail))
            if(post.imageUrl.isNotBlank())card.addView(ImageView(requireContext()).apply {contentDescription=post.title;scaleType=ImageView.ScaleType.CENTER_CROP;load(post.imageUrl)},LinearLayout.LayoutParams(-1,dp(180)))
            if(post.status=="published" && post.kind !in setOf("blood_requests","donor","job_application","team_registration","correction","reservation"))
                card.addView(action("Likes & comments"){PostDiscussion.show(this,post.table,post.id,post.title)})
            container.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(12)})
        }
        if(moreAvailable)container.addView(action("Load more"){load(true)}.apply { isEnabled=!loading })
        if(!loading)container.addView(action("Refresh status"){load(false)})
    }
    private fun bindUserProfile(user: User?) {
        val displayName = user?.displayName?.ifBlank { "Hoode Resident" } ?: "Hoode Resident"
        val username = user?.username?.ifBlank { displayName.lowercase().replace(" ", "") }
            ?: displayName.lowercase().replace(" ", "")
        val bio = user?.bio?.ifBlank { "Connecting with the Hoode community." }
            ?: "Connecting with the Hoode community."
        val locality = user?.locality?.ifBlank { "Hoode" } ?: "Hoode"

        binding.tvName.text = displayName
        binding.tvUsername.text = "@$username"
        binding.tvBio.text = bio
        binding.tvLocalityBadge.text = "$locality • Resident"

        // Avatar
        if (!user?.profilePicUri.isNullOrBlank()) {
            binding.ivProfile.load(user?.profilePicUri) {
                crossfade(true)
                placeholder(R.drawable.profile_placeholder)
                error(R.drawable.profile_placeholder)
            }
        } else {
            binding.ivProfile.setImageResource(R.drawable.profile_placeholder)
        }

        // Cover
        if (!user?.coverPicUri.isNullOrBlank()) {
            binding.ivCover.load(user?.coverPicUri) {
                crossfade(true)
                placeholder(R.drawable.profile_cover_placeholder)
                error(R.drawable.profile_cover_placeholder)
            }
        }

        if (user?.coverPicUri.isNullOrBlank()) binding.ivCover.setImageResource(R.drawable.profile_cover_placeholder)

        // Resident Details Card
        binding.tvDetailPhone.text = user?.phone?.takeIf { it.isNotBlank() } ?: "Not provided"
        binding.tvDetailEmail.text = user?.email?.takeIf { it.isNotBlank() } ?: "Not provided"
        binding.tvDetailLocality.text = user?.locality?.takeIf { it.isNotBlank() } ?: "Hoode"
        binding.tvDetailBloodGroup.text = user?.bloodGroup?.takeIf { it.isNotBlank() } ?: "Not provided"
        binding.tvDetailProfession.text = user?.profession?.takeIf { it.isNotBlank() } ?: "Not provided"
        binding.tvDetailFatherName.text = user?.fatherName?.takeIf { it.isNotBlank() } ?: "Not provided"
        binding.tvDetailStatus.text = if(HoodeRepository.isLoggedIn())"Signed in" else "Sign in required"

        // Dynamic Post Count from genuine items
        val genuinePosts = submissions
        binding.tvStatPosts.text = totals.optInt("total").toString()
    }
    private fun updateProfileCompletion(user: User?) {
        if (user == null) {
            binding.pbProfileCompletion.progress = 0
            binding.tvCompletionPercentage.text = "0%"
            binding.tvCompletionTip.text = "Please complete your resident details."
            return
        }

        var filledCount = 0
        val totalFields = 8
        val missingFields = mutableListOf<String>()

        if (!user.displayName.isNullOrBlank()) filledCount++ else missingFields.add("Full Name")
        if (!user.email.isNullOrBlank()) filledCount++ else missingFields.add("Email")
        if (!user.phone.isNullOrBlank()) filledCount++ else missingFields.add("Phone")
        if (!user.locality.isNullOrBlank()) filledCount++ else missingFields.add("Locality")
        if (!user.bloodGroup.isNullOrBlank()) filledCount++ else missingFields.add("Blood Group")
        if (!user.profession.isNullOrBlank()) filledCount++ else missingFields.add("Profession")
        if (!user.fatherName.isNullOrBlank()) filledCount++ else missingFields.add("Father's Name")
        if (!user.profilePicUri.isNullOrBlank()) filledCount++ else missingFields.add("Profile Picture")

        val percentage = (filledCount * 100) / totalFields
        binding.pbProfileCompletion.progress = percentage
        binding.tvCompletionPercentage.text = "$percentage%"

        if (percentage == 100) {
            binding.tvCompletionTip.text = "All set! Your profile is 100% complete."
            binding.btnCompleteProfile.text = "Edit Details"
        } else {
            val tip = when {
                missingFields.size == 1 -> "Add your ${missingFields[0]} to reach 100%!"
                missingFields.size >= 2 -> "Add your ${missingFields[0]} and ${missingFields[1]} to reach 100%!"
                else -> "Complete your resident details."
            }
            binding.tvCompletionTip.text = tip
            binding.btnCompleteProfile.text = "Complete Profile →"
        }
    }
    override fun onSaveInstanceState(outState:Bundle){outState.putInt("profile_tab",_binding?.profileTabs?.selectedTabPosition ?: 0);super.onSaveInstanceState(outState)}
    override fun onDestroyView(){loadJob?.cancel();_binding=null;super.onDestroyView()}
}
