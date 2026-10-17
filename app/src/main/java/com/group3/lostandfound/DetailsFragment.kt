package com.group3.lostandfound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

/**
 * DetailsFragment displays full item report details:
 * - Photo with carousel indicator
 * - Outlined status tag (LOST)
 * - Item title, description, and location metadata
 * - Address banner and reporter verification info
 * - "Contact Reporter" action
 * - Similar items recommendations
 * - Persistent bottom navigation
 */
class DetailsFragment : Fragment() {

    private lateinit var btnBack: ImageView
    private lateinit var btnBookmark: ImageView
    private lateinit var btnContact: TextView
    private lateinit var bannerLocation: View
    private lateinit var cardSimilarItem: View

    private var isBookmarked = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupListeners()
        setupBottomNav(view)
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        btnBookmark = view.findViewById(R.id.btnBookmark)
        btnContact = view.findViewById(R.id.btnContact)
        bannerLocation = view.findViewById(R.id.bannerLocation)
        cardSimilarItem = view.findViewById(R.id.cardSimilarItem)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        btnBookmark.setOnClickListener {
            isBookmarked = !isBookmarked
            if (isBookmarked) {
                Toast.makeText(requireContext(), "Item saved to bookmarks", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Removed from bookmarks", Toast.LENGTH_SHORT).show()
            }
        }

        btnContact.setOnClickListener {
            Toast.makeText(requireContext(), "Contacting Tharindu Perera...", Toast.LENGTH_SHORT).show()
        }

        bannerLocation.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, MapFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        cardSimilarItem.setOnClickListener {
            Toast.makeText(requireContext(), "Viewing: Brown card holder", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun newInstance(): DetailsFragment {
            return DetailsFragment()
        }
    }
}
