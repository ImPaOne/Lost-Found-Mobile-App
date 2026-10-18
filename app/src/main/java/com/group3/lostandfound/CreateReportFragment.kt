package com.group3.lostandfound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * CreateReportFragment allows users to report a lost or found item:
 * - 5-step progress indicator (Step 2 of 5: Category & details)
 * - Toggle selector between "Lost item" and "Found item"
 * - Category picker, item title, and description inputs
 * - "Next up" info roadmap
 * - Navigation buttons ("Back" and "Continue")
 * - Persistent bottom navigation bar
 */
class CreateReportFragment : Fragment() {

    private lateinit var btnBack: ImageView
    private lateinit var btnBackBottom: TextView
    private lateinit var btnContinue: TextView

    private lateinit var cardLostItem: View
    private lateinit var cardFoundItem: View
    private lateinit var iconLost: ImageView
    private lateinit var iconFound: ImageView

    private lateinit var boxCategory: View
    private lateinit var tvCategoryValue: TextView
    private lateinit var inputTitle: EditText
    private lateinit var inputDescription: EditText

    private var isLostSelected = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_report, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        
        val initialIsLost = arguments?.getBoolean(ARG_IS_LOST, true) ?: true
        setItemType(initialIsLost)

        setupListeners()
        setupBottomNav(view, "post")
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        btnBackBottom = view.findViewById(R.id.btnBackBottom)
        btnContinue = view.findViewById(R.id.btnContinue)

        cardLostItem = view.findViewById(R.id.cardLostItem)
        cardFoundItem = view.findViewById(R.id.cardFoundItem)
        iconLost = view.findViewById(R.id.iconLost)
        iconFound = view.findViewById(R.id.iconFound)

        boxCategory = view.findViewById(R.id.boxCategory)
        tvCategoryValue = view.findViewById(R.id.tvCategoryValue)
        inputTitle = view.findViewById(R.id.inputTitle)
        inputDescription = view.findViewById(R.id.inputDescription)
    }

    private fun setItemType(isLost: Boolean) {
        isLostSelected = isLost
        if (isLost) {
            cardLostItem.setBackgroundResource(R.drawable.bg_card_select_lost)
            iconLost.setColorFilter(ContextCompat.getColor(requireContext(), R.color.tag_lost_color))

            cardFoundItem.setBackgroundResource(R.drawable.bg_card_select_unselected)
            iconFound.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_primary))
        } else {
            cardFoundItem.setBackgroundResource(R.drawable.bg_card_select_lost)
            iconFound.setColorFilter(ContextCompat.getColor(requireContext(), R.color.tag_lost_color))

            cardLostItem.setBackgroundResource(R.drawable.bg_card_select_unselected)
            iconLost.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_primary))
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnBackBottom.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        cardLostItem.setOnClickListener {
            if (!isLostSelected) {
                setItemType(true)
            }
        }

        cardFoundItem.setOnClickListener {
            if (isLostSelected) {
                setItemType(false)
            }
        }

        boxCategory.setOnClickListener {
            val categories = arrayOf(
                "Electronics",
                "Documents & IDs",
                "Wallets & Bags",
                "Pets & Animals",
                "Keys",
                "Jewelry & Watches",
                "Other"
            )
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Select Category")
                .setItems(categories) { _, which ->
                    tvCategoryValue.text = categories[which]
                }
                .show()
        }

        // Direct redirect without blocking validations
        btnContinue.setOnClickListener {
            val title = inputTitle.text.toString().trim()
            val reportTitle = if (title.isNotEmpty()) title else if (isLostSelected) "Lost Item" else "Found Item"
            val itemType = if (isLostSelected) "Lost" else "Found"
            Toast.makeText(requireContext(), "$itemType report '$reportTitle' published successfully!", Toast.LENGTH_SHORT).show()

            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.screenBox, ReportsFragment.newInstance())
                .commit()
        }
    }

    companion object {
        private const val ARG_IS_LOST = "arg_is_lost"

        fun newInstance(isLost: Boolean = true): CreateReportFragment {
            val fragment = CreateReportFragment()
            val args = Bundle()
            args.putBoolean(ARG_IS_LOST, isLost)
            fragment.arguments = args
            return fragment
        }
    }
}
