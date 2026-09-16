package com.example.hoode_app.ui.tournaments

import android.app.Dialog
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnRegisterTeam.setOnClickListener {
            showRegisterTeamDialog()
        }

        binding.btnHeroRegister?.setOnClickListener {
            showRegisterTeamDialog()
        }

        setupSportFilterChips()

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.standings.collectLatest { standingsList ->
                binding.llStandingsContainer.removeAllViews()
                standingsList.forEachIndexed { index, standing ->
                    val rowBinding = ItemStandingRowBinding.inflate(layoutInflater, binding.llStandingsContainer, false)
                    rowBinding.tvStandingRank.text = (index + 1).toString()
                    rowBinding.tvStandingTeam.text = standing.teamName
                    rowBinding.tvStandingPlayed.text = standing.played.toString()
                    rowBinding.tvStandingWon.text = standing.won.toString()
                    rowBinding.tvStandingLost.text = standing.lost.toString()
                    rowBinding.tvStandingStats.text = "${standing.points} pts"
                    rowBinding.tvStandingNrr.text = standing.netRunRate
                    binding.llStandingsContainer.addView(rowBinding.root)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.fixtures.collectLatest { fixturesList ->
                binding.llFixturesContainer.removeAllViews()
                for (fixture in fixturesList) {
                    val cardBinding = ItemFixtureCardBinding.inflate(layoutInflater, binding.llFixturesContainer, false)
                    cardBinding.tvFixtureRound.text = fixture.round
                    cardBinding.tvFixtureTime.text = fixture.time
                    cardBinding.tvFixtureTeamA.text = fixture.teamA
                    cardBinding.tvFixtureTeamB.text = fixture.teamB
                    cardBinding.tvFixtureVenue.text = fixture.venue
                    if (fixture.scoreA != null) {
                        cardBinding.tvFixtureScore.visibility = View.VISIBLE
                        cardBinding.tvFixtureScore.text = "${fixture.teamA}: ${fixture.scoreA} • ${fixture.teamB}: ${fixture.scoreB}"
                    } else {
                        cardBinding.tvFixtureScore.visibility = View.GONE
                    }
                    binding.llFixturesContainer.addView(cardBinding.root)
                }
            }
        }
    }

    private fun setupSportFilterChips() {
        val chips = listOf(
            binding.chipSportAll to "All",
            binding.chipSportCricket to "Cricket",
            binding.chipSportFootball to "Football",
            binding.chipSportBadminton to "Badminton"
        )

        for ((view, sport) in chips) {
            view.setOnClickListener {
                selectedSport = sport
                for ((v, s) in chips) {
                    if (s == selectedSport) {
                        v.setBackgroundResource(R.drawable.bg_chip_black_border)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                    } else {
                        v.setBackgroundResource(R.drawable.bg_chip_unselected)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    }
                }
                if (sport != "Cricket" && sport != "All") {
                    Toast.makeText(requireContext(), "$sport tournaments scheduled for next quarter!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showRegisterTeamDialog() {
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

            HoodeRepository.registerTournamentTeam(teamName)
            Toast.makeText(requireContext(), "Team '$teamName' registered for the Tournament!", Toast.LENGTH_LONG).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
