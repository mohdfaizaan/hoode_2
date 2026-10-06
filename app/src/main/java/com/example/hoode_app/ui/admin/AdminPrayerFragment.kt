package com.example.hoode_app.ui.admin
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
class AdminPrayerFragment:Fragment() {
 override fun onCreateView(inflater:LayoutInflater,parent:ViewGroup?,state:Bundle?):View = LinearLayout(requireContext()).apply {
  orientation=LinearLayout.VERTICAL;setPadding(32,48,32,32)
  addView(TextView(context).apply{text="Community administration";textSize=24f})
  addView(TextView(context).apply{text="Use the Hoode Admin app to manage shared content, sponsorships and requests.";textSize=16f;setPadding(0,24,0,24)})
  addView(com.google.android.material.button.MaterialButton(context).apply{text="Back";setOnClickListener{findNavController().navigateUp()}})
 }
}
