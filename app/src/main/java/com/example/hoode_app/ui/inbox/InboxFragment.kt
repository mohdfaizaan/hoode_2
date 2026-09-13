package com.example.hoode_app.ui.inbox

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.hoode_app.R
import com.example.hoode_app.databinding.FragmentInboxBinding
import com.example.hoode_app.databinding.ItemConversationCardBinding

class InboxFragment : Fragment() {

    private var _binding: FragmentInboxBinding? = null
    private val binding get() = _binding!!

    data class CommunityChat(
        val id: String,
        val senderName: String,
        val itemTitle: String,
        var lastMessage: String,
        val timestamp: String,
        val category: String, // "Marketplace", "Blood Network", "Jobs", "Support"
        var isUnread: Boolean,
        val messages: MutableList<Pair<String, String>> // (sender, text)
    )

    private val conversations = mutableListOf(
        CommunityChat(
            id = "chat_01",
            senderName = "Ahmed K.",
            itemTitle = "Teakwood Study Table",
            lastMessage = "Is the price negotiable? Can I inspect it near Hoode Beach road?",
            timestamp = "10:45 AM",
            category = "Marketplace",
            isUnread = true,
            messages = mutableListOf(
                "Ahmed K." to "Assalamu Alaikum, is this study table still available?",
                "You" to "Wa Alaikum Assalam, yes it is in excellent condition.",
                "Ahmed K." to "Is the price negotiable? Can I inspect it near Hoode Beach road?"
            )
        ),
        CommunityChat(
            id = "chat_02",
            senderName = "Dr. Farhan",
            itemTitle = "Manipal Hospital Blood Need",
            lastMessage = "Two B+ volunteer donors confirmed. Thank you for coordinating!",
            timestamp = "Yesterday",
            category = "Blood Network",
            isUnread = true,
            messages = mutableListOf(
                "You" to "Sent broadcast alert to 14 verified B+ donors in Hoode.",
                "Dr. Farhan" to "Two B+ volunteer donors confirmed. Thank you for coordinating!"
            )
        ),
        CommunityChat(
            id = "chat_03",
            senderName = "Coastal Fisheries",
            itemTitle = "Fresh Catch Order #HF-209",
            lastMessage = "Your seafood parcel is dispatched. Estimated arrival in 20 minutes.",
            timestamp = "Sep 09",
            category = "Marketplace",
            isUnread = false,
            messages = mutableListOf(
                "Coastal Fisheries" to "Order #HF-209 received: 2kg Kingfish & 1kg Prawns.",
                "Coastal Fisheries" to "Your seafood parcel is dispatched. Estimated arrival in 20 minutes."
            )
        ),
        CommunityChat(
            id = "chat_04",
            senderName = "HPL Tournament Desk",
            itemTitle = "Bengre Strikers Roster",
            lastMessage = "Team registration accepted for Group B fixtures.",
            timestamp = "Sep 07",
            category = "Jobs",
            isUnread = false,
            messages = mutableListOf(
                "You" to "Submitted team roster for 11 players + 3 reserves.",
                "HPL Tournament Desk" to "Team registration accepted for Group B fixtures."
            )
        ),
        CommunityChat(
            id = "chat_05",
            senderName = "Civic Help Desk",
            itemTitle = "Bengre Jetty Streetlights",
            lastMessage = "MESCOM engineer assigned. Replacement lamps installed.",
            timestamp = "Sep 04",
            category = "Support",
            isUnread = false,
            messages = mutableListOf(
                "You" to "Reported 3 non-functioning sodium vapor streetlights on Jetty Road.",
                "Civic Help Desk" to "MESCOM engineer assigned. Replacement lamps installed."
            )
        )
    )

    private var activeCategoryFilter = "All"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInboxBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFilterChips()
        renderConversations()
    }

    private fun setupFilterChips() {
        binding.chipGroupInbox.setOnCheckedStateChangeListener { _, checkedIds ->
            activeCategoryFilter = when {
                checkedIds.contains(R.id.chip_marketplace) -> "Marketplace"
                checkedIds.contains(R.id.chip_blood) -> "Blood Network"
                checkedIds.contains(R.id.chip_jobs) -> "Jobs"
                checkedIds.contains(R.id.chip_support) -> "Support"
                else -> "All"
            }
            renderConversations()
        }
    }

    private fun renderConversations() {
        binding.llConversations.removeAllViews()

        val filtered = if (activeCategoryFilter == "All") {
            conversations
        } else {
            conversations.filter { it.category.equals(activeCategoryFilter, ignoreCase = true) }
        }

        // Update unread counter badge
        val unreadCount = conversations.count { it.isUnread }
        binding.tvUnreadCounter.text = if (unreadCount > 0) "$unreadCount Unread" else "All Caught Up"

        if (filtered.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
        } else {
            binding.emptyState.visibility = View.GONE
            for (chat in filtered) {
                val cardBinding = ItemConversationCardBinding.inflate(layoutInflater, binding.llConversations, false)

                cardBinding.tvSenderName.text = "${chat.senderName} • ${chat.itemTitle}"
                cardBinding.tvMessagePreview.text = chat.lastMessage
                cardBinding.tvMessageTime.text = chat.timestamp
                cardBinding.tvContextTag.text = chat.category
                cardBinding.indicatorUnread.visibility = if (chat.isUnread) View.VISIBLE else View.GONE

                // Icon selection based on category
                when (chat.category) {
                    "Marketplace" -> {
                        cardBinding.ivSenderIcon.setImageResource(R.drawable.ic_classified)
                    }
                    "Blood Network" -> {
                        cardBinding.ivSenderIcon.setImageResource(R.drawable.ic_blood)
                        cardBinding.flIconContainer.setBackgroundResource(R.drawable.bg_pill_danger)
                    }
                    "Jobs" -> {
                        cardBinding.ivSenderIcon.setImageResource(R.drawable.ic_jobs)
                    }
                    else -> {
                        cardBinding.ivSenderIcon.setImageResource(R.drawable.ic_poll)
                    }
                }

                cardBinding.root.setOnClickListener {
                    openChatDialog(chat)
                }

                binding.llConversations.addView(cardBinding.root)
            }
        }
    }

    private fun openChatDialog(chat: CommunityChat) {
        chat.isUnread = false
        renderConversations()

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_inbox, null)
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 36, 48, 24)
        }

        // Messages transcript
        val tvTranscript = TextView(requireContext()).apply {
            val sb = StringBuilder()
            chat.messages.forEach { (sender, text) ->
                sb.append("• $sender:\n  $text\n\n")
            }
            text = sb.toString()
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            textSize = 14f
            setLineSpacing(4f, 1f)
        }
        container.addView(tvTranscript)

        // Reply input
        val etReply = EditText(requireContext()).apply {
            hint = "Write a reply to ${chat.senderName}..."
            setBackgroundResource(R.drawable.bg_search_bar)
            setPadding(36, 28, 36, 28)
            textSize = 14f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
        }
        container.addView(etReply)

        AlertDialog.Builder(requireContext())
            .setTitle("${chat.senderName} (${chat.itemTitle})")
            .setView(container)
            .setPositiveButton("Send Reply") { _, _ ->
                val replyText = etReply.text.toString().trim()
                if (replyText.isNotBlank()) {
                    chat.messages.add("You" to replyText)
                    chat.lastMessage = replyText
                    renderConversations()
                    Toast.makeText(requireContext(), "Reply sent to ${chat.senderName}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
