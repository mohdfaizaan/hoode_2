package com.example.hoode_app.ui.news

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.R
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentNewsBinding
import com.example.hoode_app.databinding.ItemNewsCardBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NewsFragment : Fragment() {

    private var _binding: FragmentNewsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.newsArticles.collectLatest { articles ->
                binding.llNewsContainer.removeAllViews()
                for (article in articles) {
                    val cardBinding = ItemNewsCardBinding.inflate(layoutInflater, binding.llNewsContainer, false)
                    if (article.isRumorClarification) {
                        cardBinding.tvNewsType.text = "RUMOR CLARIFIED"
                        cardBinding.tvNewsType.setBackgroundResource(R.drawable.bg_pill_danger)
                        cardBinding.tvNewsType.setTextColor(ContextCompat.getColor(requireContext(), R.color.danger))
                    } else {
                        cardBinding.tvNewsType.text = "OFFICIAL UPDATE"
                        cardBinding.tvNewsType.setBackgroundResource(R.drawable.bg_pill_accent)
                        cardBinding.tvNewsType.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_ink))
                    }
                    cardBinding.tvNewsTitle.text = article.title
                    cardBinding.tvNewsSummary.text = article.summary
                    cardBinding.tvNewsBody.text = "${article.body}\n\nSources:\n• " + article.sources.joinToString("\n• ")
                    cardBinding.tvNewsDate.text = article.verifiedDate
                    cardBinding.tvNewsVerifier.text = "Verified by: ${article.verifier}"

                    var isExpanded = false
                    cardBinding.btnExpandNews.setOnClickListener {
                        isExpanded = !isExpanded
                        cardBinding.tvNewsBody.visibility = if (isExpanded) View.VISIBLE else View.GONE
                        cardBinding.btnExpandNews.text = if (isExpanded) "Show Less ↑" else "Read Story →"
                    }

                    binding.llNewsContainer.addView(cardBinding.root)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
