package com.group3.lostandfound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView

/**
 * HomeFragment displays the main dashboard of the Foundly app:
 * - Search bar with filter options
 * - Quick action cards: "Report Lost" & "Report Found"
 * - Monthly community statistics in warm beige tone
 * - Category filter chips ("All", "Electronics", "Documents")
 * - Recent lost and found item listings
 */
class HomeFragment : Fragment() {

    // Header views
    private lateinit var buttonHeaderClose: ImageView
    private lateinit var buttonNotifications: FrameLayout

    // Search views
    private lateinit var inputSearchField: EditText
    private lateinit var buttonFilterSettings: ImageView

    // Action cards
    private lateinit var cardReportLostItem: LinearLayout
    private lateinit var cardReportFoundItem: LinearLayout

    // Category chips
    private lateinit var chipCategoryAll: TextView
    private lateinit var chipCategoryElectronics: TextView
    private lateinit var chipCategoryDocuments: TextView

    // Recent items section
    private lateinit var buttonSeeAllRecent: TextView
    private lateinit var cardWalletItem: MaterialCardView
    private lateinit var cardDogItem: MaterialCardView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners()
        NavigationHelper.setup(this, view, NavigationHelper.Tab.HOME)
    }

    private fun initViews(view: View) {
        buttonHeaderClose = view.findViewById(R.id.buttonHeaderClose)
        buttonNotifications = view.findViewById(R.id.buttonNotifications)

        inputSearchField = view.findViewById(R.id.inputSearchField)
        buttonFilterSettings = view.findViewById(R.id.buttonFilterSettings)

        cardReportLostItem = view.findViewById(R.id.cardReportLostItem)
        cardReportFoundItem = view.findViewById(R.id.cardReportFoundItem)

        chipCategoryAll = view.findViewById(R.id.chipCategoryAll)
        chipCategoryElectronics = view.findViewById(R.id.chipCategoryElectronics)
        chipCategoryDocuments = view.findViewById(R.id.chipCategoryDocuments)

        buttonSeeAllRecent = view.findViewById(R.id.buttonSeeAllRecent)
        cardWalletItem = view.findViewById(R.id.cardWalletItem)
        cardDogItem = view.findViewById(R.id.cardDogItem)
    }

    private fun setupClickListeners() {
        buttonHeaderClose.setOnClickListener {
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack()
            } else {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.screenBox, ProfileFragment.newInstance())
                    .addToBackStack(null)
                    .commit()
            }
        }

        buttonNotifications.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, NotificationsFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        buttonFilterSettings.setOnClickListener {
            Toast.makeText(requireContext(), "Filtering: All recent items", Toast.LENGTH_SHORT).show()
        }

        cardReportLostItem.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, CreateReportFragment.newInstance(isLost = true))
                .addToBackStack(null)
                .commit()
        }

        cardReportFoundItem.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, CreateReportFragment.newInstance(isLost = false))
                .addToBackStack(null)
                .commit()
        }

        chipCategoryAll.setOnClickListener { selectCategoryChip(chipCategoryAll) }
        chipCategoryElectronics.setOnClickListener { selectCategoryChip(chipCategoryElectronics) }
        chipCategoryDocuments.setOnClickListener { selectCategoryChip(chipCategoryDocuments) }

        buttonSeeAllRecent.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, ExploreFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        // Tapping black wallet card opens DetailsFragment
        cardWalletItem.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.screenBox,
                    DetailsFragment.newInstance(
                        title = "Black leather wallet",
                        desc = "Slim wallet with national ID and two bank cards. Small scratch near the clasp.",
                        status = "LOST",
                        location = "Colombo 03",
                        imageResId = R.drawable.img_wallet_details
                    )
                )
                .addToBackStack(null)
                .commit()
        }

        cardDogItem.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.screenBox,
                    DetailsFragment.newInstance(
                        title = "Golden retriever, Milo",
                        desc = "Friendly golden retriever with blue collar and brass tag. Missing since yesterday afternoon.",
                        status = "LOST",
                        location = "Cinnamon Gardens",
                        imageResId = R.drawable.img_tabby_cat
                    )
                )
                .addToBackStack(null)
                .commit()
        }

        view?.findViewById<View>(R.id.cardLaptopHome)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.screenBox,
                    DetailsFragment.newInstance(
                        title = "Red laptop sleeve",
                        desc = "Padded red neoprene laptop sleeve found on park bench near walking path.",
                        status = "FOUND",
                        location = "Independence Square",
                        imageResId = R.drawable.img_wallet_details
                    )
                )
                .addToBackStack(null)
                .commit()
        }
    }

    private fun selectCategoryChip(selectedChip: TextView) {
        val allChips = listOf(chipCategoryAll, chipCategoryElectronics, chipCategoryDocuments)
        for (chip in allChips) {
            if (chip == selectedChip) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            }
        }
    }

    companion object {
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }
}
