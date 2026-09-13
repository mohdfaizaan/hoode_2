package com.example.hoode_app.ui.tournaments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.R
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentTournamentsBinding
import com.example.hoode_app.databinding.ItemFixtureCardBinding
import com.example.hoode_app.databinding.ItemStandingRowBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TournamentsFragment : Fragment() {

    private var _binding: FragmentTournamentsBinding? = null
    private val binding get() = _binding!!

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
            val input = EditText(requireContext()).apply { hint = "Team Name (e.g. Hoode Lions)" }
            AlertDialog.Builder(requireContext())
                .setTitle("Register Tournament Team")
                .setMessage("Enter your team name and captain details. Minimum 11 squad members.")
                .setView(input)
                .setPositiveButton("Register") { _, _ ->
                    val team = input.text.toString().trim()
                    if (team.isNotBlank()) {
                        HoodeRepository.registerTournamentTeam(team)
                        Toast.makeText(requireContext(), "Team '$team' registered for HPL Season 7!", Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.standings.collectLatest { standingsList ->
                binding.llStandingsContainer.removeAllViews()
                for (standing in standingsList) {
                    val rowBinding = ItemStandingRowBinding.inflate(layoutInflater, binding.llStandingsContainer, false)
                    rowBinding.tvStandingTeam.text = standing.teamName
                    rowBinding.tvStandingStats.text = "P:${standing.played}  W:${standing.won}  Pts:${standing.points} (${standing.netRunRate})"
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
                    cardBinding.tvFixtureTime.text = "${fixture.time} • ${fixture.venue}"
                    cardBinding.tvFixtureMatch.text = "${fixture.teamA} vs ${fixture.teamB}"
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
