package com.group3.lostandfound

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class SavedItemsFragment : Fragment() {

    private lateinit var btnBack: View
    private lateinit var btnMore: View
    private lateinit var searchField: EditText

    private lateinit var chipAll: TextView
    private lateinit var chipLost: TextView
    private lateinit var chipFound: TextView

    private lateinit var tvSavedCount: TextView
    private lateinit var btnSort: TextView

    private lateinit var cardWallet: LinearLayout
    private lateinit var cardDog: LinearLayout
    private lateinit var cardCardHolder: LinearLayout

    private lateinit var btnBookmarkWallet: ImageView
    private lateinit var btnBookmarkDog: ImageView
    private lateinit var btnBookmarkCardHolder: ImageView

    private var selectedCategory = "ALL" // "ALL", "LOST", "FOUND"
    private var searchQuery = ""

    private var isWalletSaved = true
    private var isDogSaved = true
    private var isCardHolderSaved = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_saved_items, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        btnBack = view.findViewById(R.id.btnBack)
        btnMore = view.findViewById(R.id.btnMore)
        searchField = view.findViewById(R.id.searchField)

        chipAll = view.findViewById(R.id.chipAll)
        chipLost = view.findViewById(R.id.chipLost)
        chipFound = view.findViewById(R.id.chipFound)

        tvSavedCount = view.findViewById(R.id.tvSavedCount)
        btnSort = view.findViewById(R.id.btnSort)

        cardWallet = view.findViewById(R.id.cardWallet)
        cardDog = view.findViewById(R.id.cardDog)
        cardCardHolder = view.findViewById(R.id.cardCardHolder)

        btnBookmarkWallet = view.findViewById(R.id.btnBookmarkWallet)
        btnBookmarkDog = view.findViewById(R.id.btnBookmarkDog)
        btnBookmarkCardHolder = view.findViewById(R.id.btnBookmarkCardHolder)

        // Navigation
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnMore.setOnClickListener {
            Toast.makeText(requireContext(), "More options", Toast.LENGTH_SHORT).show()
        }

        btnSort.setOnClickListener {
            Toast.makeText(requireContext(), "Sorted by recently saved", Toast.LENGTH_SHORT).show()
        }

        // Category Filter Chips
        chipAll.setOnClickListener {
            selectedCategory = "ALL"
            updateChipStyles()
            filterItems()
        }

        chipLost.setOnClickListener {
            selectedCategory = "LOST"
            updateChipStyles()
            filterItems()
        }

        chipFound.setOnClickListener {
            selectedCategory = "FOUND"
            updateChipStyles()
            filterItems()
        }     // Search text watcher filters saved items whenever the search text changes.
        searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Convert the search text to lowercase and remove extra spaces for matching.
                searchQuery = s?.toString()?.trim()?.lowercase() ?: ""
                filterItems()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Toggle the wallet bookmark and refresh its visibility, notification, and saved count.
        btnBookmarkWallet.setOnClickListener {
            isWalletSaved = !isWalletSaved
            cardWallet.visibility = if (isWalletSaved) View.VISIBLE else View.GONE
            Toast.makeText(requireContext(), if (isWalletSaved) "Wallet saved" else "Wallet removed from saved", Toast.LENGTH_SHORT).show()
            updateCount()
        }

        // Toggle the dog's saved status and update the displayed saved items.
        btnBookmarkDog.setOnClickListener {
            isDogSaved = !isDogSaved
            cardDog.visibility = if (isDogSaved) View.VISIBLE else View.GONE
            Toast.makeText(requireContext(), if (isDogSaved) "Dog saved" else "Dog removed from saved", Toast.LENGTH_SHORT).show()
            updateCount()
        }

        // Toggle the card holder's saved status and refresh the saved list.
        btnBookmarkCardHolder.setOnClickListener {
            isCardHolderSaved = !isCardHolderSaved
            cardCardHolder.visibility = if (isCardHolderSaved) View.VISIBLE else View.GONE
            Toast.makeText(requireContext(), if (isCardHolderSaved) "Card holder saved" else "Card holder removed from saved", Toast.LENGTH_SHORT).show()
            updateCount()
        }

        // Configure bottom navigation and mark the Profile tab as active.
        setupBottomNav(view, "profile")
    }

    // Apply the appropriate background and text color to each category filter chip.
    private fun updateChipStyles() {
        val darkBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_pill_dark)
        val outlineBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_pill_outline)

        val whiteColor = ContextCompat.getColor(requireContext(), R.color.white)
        val lostColor = 0xFFDC2626.toInt()
        val foundColor = 0xFF059669.toInt()
        val darkNavy = 0xFF0B2238.toInt()

        // Highlight the All chip when every category is selected.
        if (selectedCategory == "ALL") {
            chipAll.background = darkBg
            chipAll.setTextColor(whiteColor)
        } else {
            chipAll.background = outlineBg
            chipAll.setTextColor(darkNavy)
        }

        // Highlight the Lost chip and use red text when it is not selected.
        if (selectedCategory == "LOST") {
            chipLost.background = darkBg
            chipLost.setTextColor(whiteColor)
        } else {
            chipLost.background = outlineBg
            chipLost.setTextColor(lostColor)
        }

        // Highlight the Found chip and use green text when it is not selected.
        if (selectedCategory == "FOUND") {
            chipFound.background = darkBg
            chipFound.setTextColor(whiteColor)
        } else {
            chipFound.background = outlineBg
            chipFound.setTextColor(foundColor)
        }
    }

    // Filter saved items by category and search text, then update their visibility and total count.
    private fun filterItems() {
        var visibleCount = 0

        // Show the wallet only when it is saved and matches the selected category and search.
        // Wallet details: LOST, "Black leather wallet", "Colombo 03".
        val walletMatchesCategory = (selectedCategory == "ALL" || selectedCategory == "LOST")
        val walletMatchesSearch = searchQuery.isEmpty() ||
                "black leather wallet".contains(searchQuery) ||
                "colombo 03".contains(searchQuery)
        val showWallet = isWalletSaved && walletMatchesCategory && walletMatchesSearch
        cardWallet.visibility = if (showWallet) View.VISIBLE else View.GONE
        if (showWallet) visibleCount++

        // Show the dog only when it is saved and matches the selected category and search.
        // Dog details: FOUND, "Golden retriever, Milo", "Viharamahadevi Park".
        val dogMatchesCategory = (selectedCategory == "ALL" || selectedCategory == "FOUND")
        val dogMatchesSearch = searchQuery.isEmpty() ||
                "golden retriever, milo".contains(searchQuery) ||
                "viharamahadevi park".contains(searchQuery)
        val showDog = isDogSaved && dogMatchesCategory && dogMatchesSearch
        cardDog.visibility = if (showDog) View.VISIBLE else View.GONE
        if (showDog) visibleCount++

        // Show the card holder only when it is saved and matches the selected category and search.
        // Card holder details: FOUND, "Brown card holder", "Kollupitiya".
        val cardHolderMatchesCategory = (selectedCategory == "ALL" || selectedCategory == "FOUND")
        val cardHolderMatchesSearch = searchQuery.isEmpty() ||
                "brown card holder".contains(searchQuery) ||
                "kollupitiya".contains(searchQuery)
        val showCardHolder = isCardHolderSaved && cardHolderMatchesCategory && cardHolderMatchesSearch
        cardCardHolder.visibility = if (showCardHolder) View.VISIBLE else View.GONE
        if (showCardHolder) visibleCount++

        // Display the number of matching saved items with the correct singular or plural wording.
        tvSavedCount.text = "$visibleCount saved item${if (visibleCount == 1) "" else "s"}"
    }

    // Reapply the filters to refresh the saved item list and count after bookmark changes.
    private fun updateCount() {
        filterItems()
    }

    companion object {
        // Provide a standard way to create a new SavedItemsFragment instance.
        fun newInstance(): SavedItemsFragment = SavedItemsFragment()
    }
}