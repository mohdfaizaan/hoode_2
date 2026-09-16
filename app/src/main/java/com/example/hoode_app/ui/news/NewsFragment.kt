package com.example.hoode_app.ui.news

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.hoode_app.R
import com.example.hoode_app.data.model.NewsArticle
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.DialogNewsArticleReaderBinding
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
                    cardBinding.tvNewsDate.text = article.verifiedDate
                    cardBinding.tvNewsVerifier.text = "Verified by: ${article.verifier}"

                    cardBinding.ivNewsThumbnail.load(article.imageUrl) {
                        crossfade(true)
                        placeholder(R.drawable.bg_gallery_luxury_gradient)
                        error(R.drawable.bg_gallery_luxury_gradient)
                    }

                    // Tapping the card or "Read Story" opens full news article reader
                    cardBinding.root.setOnClickListener {
                        showArticleReaderDialog(article)
                    }
                    cardBinding.btnExpandNews.setOnClickListener {
                        showArticleReaderDialog(article)
                    }

                    binding.llNewsContainer.addView(cardBinding.root)
                }
            }
        }
    }

    private fun showArticleReaderDialog(article: NewsArticle) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogNewsArticleReaderBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialogBinding.ivArticleImage.load(article.imageUrl) {
            crossfade(true)
            placeholder(R.drawable.bg_gallery_luxury_gradient)
            error(R.drawable.bg_gallery_luxury_gradient)
        }

        if (article.isRumorClarification) {
            dialogBinding.tvArticleType.text = "RUMOR CLARIFIED"
            dialogBinding.tvArticleType.setBackgroundResource(R.drawable.bg_pill_danger)
            dialogBinding.tvArticleType.setTextColor(ContextCompat.getColor(requireContext(), R.color.danger))
        } else {
            dialogBinding.tvArticleType.text = "OFFICIAL UPDATE"
            dialogBinding.tvArticleType.setBackgroundResource(R.drawable.bg_pill_accent)
            dialogBinding.tvArticleType.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_ink))
        }

        dialogBinding.tvArticleHeadline.text = article.title
        dialogBinding.tvArticleDate.text = "${article.verifiedDate} • Hoode News Desk"
        dialogBinding.tvArticleVerifier.text = "Verified by: ${article.verifier}"
        dialogBinding.tvArticleBody.text = "${article.summary}\n\n${article.body}"
        dialogBinding.tvArticleSources.text = "• " + article.sources.joinToString("\n• ")

        dialogBinding.btnCloseArticle.setOnClickListener {
            dialog.dismiss()
        }
        dialogBinding.btnDoneArticle.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnShareArticle.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, article.title)
                putExtra(Intent.EXTRA_TEXT, "${article.title}\n\nRead more on Hoode Connect:\n${article.summary}")
            }
            startActivity(Intent.createChooser(shareIntent, "Share News Article"))
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
