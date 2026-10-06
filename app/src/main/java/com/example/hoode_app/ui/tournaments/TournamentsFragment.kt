package com.example.hoode_app.ui.tournaments

import android.app.Dialog
import com.example.hoode_app.ui.common.submitForReview
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.R
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.DialogFormRegisterTeamBinding
import com.example.hoode_app.databinding.FragmentTournamentsBinding
import com.example.hoode_app.databinding.ItemFixtureCardBinding
import com.example.hoode_app.databinding.ItemStandingRowBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TournamentsFragment : Fragment() {

    private var _binding: FragmentTournamentsBinding? = null
    private val binding get() = _binding!!
    private var selectedSport = "All"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTournamentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var tournamentId:String?=null
    private lateinit var selector:android.widget.Spinner
    private fun visibleTournaments()=HoodeRepository.tournaments.value.filter{selectedSport=="All"||it.sport.equals(selectedSport,true)}
    private fun selectedTournament()=visibleTournaments().find{it.id==tournamentId}
    override fun onViewCreated(view:View,savedInstanceState:Bundle?) {
        binding.btnBack.setOnClickListener{findNavController().navigateUp()}
        binding.btnRegisterTeam.setOnClickListener{showRegisterTeamDialog()}
        binding.btnHeroRegister.setOnClickListener{showRegisterTeamDialog()}
        selector=android.widget.Spinner(requireContext()).apply{contentDescription="Choose a tournament"}
        (binding.tvTournamentTitle.parent as android.widget.LinearLayout).addView(selector,0)
        selector.onItemSelectedListener=object:android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent:android.widget.AdapterView<*>?,v:View?,position:Int,id:Long){tournamentId=visibleTournaments().getOrNull(position)?.id;render()}
            override fun onNothingSelected(parent:android.widget.AdapterView<*>?){}
        }
        listOf(binding.chipSportAll to "All",binding.chipSportCricket to "Cricket",binding.chipSportFootball to "Football",binding.chipSportBadminton to "Badminton").forEach{(chip,sport)->
            chip.setOnClickListener{selectedSport=sport;updateSelector()}
        }
        viewLifecycleOwner.lifecycleScope.launch{HoodeRepository.tournaments.collectLatest{updateSelector()}}
        viewLifecycleOwner.lifecycleScope.launch{HoodeRepository.fixtures.collectLatest{render()}}
        viewLifecycleOwner.lifecycleScope.launch{HoodeRepository.standings.collectLatest{render()}}
    }
    private fun updateSelector() {
        val choices=visibleTournaments();if(choices.none{it.id==tournamentId})tournamentId=choices.firstOrNull()?.id
        selector.adapter=android.widget.ArrayAdapter(requireContext(),android.R.layout.simple_spinner_dropdown_item,choices.map{it.title})
        selector.setSelection(choices.indexOfFirst{it.id==tournamentId}.coerceAtLeast(0));render()
    }
    private fun render() {
        if(_binding==null)return
        val tournament=selectedTournament()
        binding.tvTournamentTitle.text=tournament?.title ?: "No tournament announced"
        binding.tvTournamentVenue.text=tournament?.let{"${it.venue} · ${it.dates} · ${it.format}"} ?: "The community team will publish upcoming tournaments here."
        binding.btnRegisterTeam.isEnabled=tournament!=null;binding.btnHeroRegister.isEnabled=tournament!=null
        binding.llStandingsContainer.removeAllViews();binding.llFixturesContainer.removeAllViews()
        HoodeRepository.standings.value.filter{it.tournamentId==tournamentId}.forEachIndexed{index,standing->
            val row=ItemStandingRowBinding.inflate(layoutInflater,binding.llStandingsContainer,false)
            row.tvStandingRank.text=(index+1).toString();row.tvStandingTeam.text=standing.teamName;row.tvStandingPlayed.text=standing.played.toString()
            row.tvStandingWon.text=standing.won.toString();row.tvStandingLost.text=standing.lost.toString();row.tvStandingStats.text="${standing.points} pts";row.tvStandingNrr.text=standing.netRunRate
            binding.llStandingsContainer.addView(row.root)
        }
        HoodeRepository.fixtures.value.filter{it.tournamentId==tournamentId}.forEach{fixture->
            val card=ItemFixtureCardBinding.inflate(layoutInflater,binding.llFixturesContainer,false)
            card.tvFixtureRound.text=fixture.round;card.tvFixtureTime.text=fixture.time;card.tvFixtureTeamA.text=fixture.teamA;card.tvFixtureTeamB.text=fixture.teamB;card.tvFixtureVenue.text=fixture.venue
            card.tvFixtureScore.visibility=if(fixture.scoreA==null)View.GONE else View.VISIBLE;card.tvFixtureScore.text="${fixture.teamA}: ${fixture.scoreA} · ${fixture.teamB}: ${fixture.scoreB}"
            binding.llFixturesContainer.addView(card.root)
        }
    }

    private fun showRegisterTeamDialog() {
        val tournament=selectedTournament() ?: return
        val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog_NoActionBar)
        val dialogBinding = DialogFormRegisterTeamBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val currentUser = HoodeRepository.currentUser.value
        currentUser?.let {
            dialogBinding.etCaptainName.setText(it.displayName)
            dialogBinding.etCaptainPhone.setText(it.phone ?: "")
            if (!it.locality.isNullOrBlank()) {
                dialogBinding.etTeamLocality.setText(it.locality)
            }
        }

        dialogBinding.btnCloseRegister.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSubmitTeam.setOnClickListener {
            val teamName = dialogBinding.etTeamName.text.toString().trim()
            val captainName = dialogBinding.etCaptainName.text.toString().trim()
            val phone = dialogBinding.etCaptainPhone.text.toString().trim()

            if (teamName.isBlank()) {
                dialogBinding.tilTeamName.error = "Please enter your team name"
                return@setOnClickListener
            }
            dialogBinding.tilTeamName.error = null

            if (captainName.isBlank()) {
                dialogBinding.tilCaptainName.error = "Please enter captain name"
                return@setOnClickListener
            }
            dialogBinding.tilCaptainName.error = null

            if (phone.isBlank()) {
                dialogBinding.tilCaptainPhone.error = "Please enter contact phone"
                return@setOnClickListener
            }
            dialogBinding.tilCaptainPhone.error = null

            submitForReview(dialogBinding.btnSubmitTeam,dialog,privateRequest=true) { HoodeRepository.registerTournamentTeam(teamName,captainName,phone,tournament.id) }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
