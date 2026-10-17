package com.group3.lostandfound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView

class MapFragment : Fragment() {

    private lateinit var btnClose: FrameLayout
    private lateinit var btnListView: ImageView
    private lateinit var btnFilterSliders: ImageView
    private lateinit var inputSearchArea: EditText

    private lateinit var chipLost: TextView
    private lateinit var chipFound: TextView
    private lateinit var chipToday: TextView

    private lateinit var pinTopLeft: FrameLayout
    private lateinit var pinCenter: FrameLayout
    private lateinit var pinTopRight: FrameLayout
    private lateinit var pinBottomRight: FrameLayout
    private lateinit var btnGpsTarget: FrameLayout

    private lateinit var cardLaptopItem: MaterialCardView
    private lateinit var textItemTitle: TextView
    private lateinit var textItemLocation: TextView
    private lateinit var tagFoundStatus: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnClose = view.findViewById(R.id.btnClose)
        btnListView = view.findViewById(R.id.btnListView)
        btnFilterSliders = view.findViewById(R.id.btnFilterSliders)
        inputSearchArea = view.findViewById(R.id.inputSearchArea)

        chipLost = view.findViewById(R.id.chipLost)
        chipFound = view.findViewById(R.id.chipFound)
        chipToday = view.findViewById(R.id.chipToday)

        pinTopLeft = view.findViewById(R.id.pinTopLeft)
        pinCenter = view.findViewById(R.id.pinCenter)
        pinTopRight = view.findViewById(R.id.pinTopRight)
        pinBottomRight = view.findViewById(R.id.pinBottomRight)
        btnGpsTarget = view.findViewById(R.id.btnGpsTarget)

        cardLaptopItem = view.findViewById(R.id.cardLaptopItem)
        textItemTitle = view.findViewById(R.id.textItemTitle)
        textItemLocation = view.findViewById(R.id.textItemLocation)
        tagFoundStatus = view.findViewById(R.id.tagFoundStatus)

        // Setup persistent bottom nav for Explore tab
        setupBottomNav(view, "explore")

        btnClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnListView.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, ExploreFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        btnFilterSliders.setOnClickListener {
            Toast.makeText(requireContext(), "Filter by distance & category", Toast.LENGTH_SHORT).show()
        }

        chipLost.setOnClickListener {
            Toast.makeText(requireContext(), "Showing: Lost items", Toast.LENGTH_SHORT).show()
        }

        chipFound.setOnClickListener {
            Toast.makeText(requireContext(), "Showing: Found items", Toast.LENGTH_SHORT).show()
        }

        chipToday.setOnClickListener {
            Toast.makeText(requireContext(), "Showing: Today's items", Toast.LENGTH_SHORT).show()
        }

        btnGpsTarget.setOnClickListener {
            Toast.makeText(requireContext(), "Centered to current location", Toast.LENGTH_SHORT).show()
        }

        pinCenter.setOnClickListener {
            textItemTitle.text = "Red laptop sleeve"
            textItemLocation.text = "Independence Square"
            tagFoundStatus.text = "FOUND"
        }

        pinTopLeft.setOnClickListener {
            textItemTitle.text = "Black leather wallet"
            textItemLocation.text = "Kollupitiya"
            tagFoundStatus.text = "LOST"
        }

        pinTopRight.setOnClickListener {
            textItemTitle.text = "Sri Lankan passport"
            textItemLocation.text = "Fort Station"
            tagFoundStatus.text = "FOUND"
        }

        pinBottomRight.setOnClickListener {
            textItemTitle.text = "AirPods Pro Case"
            textItemLocation.text = "Bambalapitiya"
            tagFoundStatus.text = "FOUND"
        }

        cardLaptopItem.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, DetailsFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }
    }

    companion object {
        fun newInstance(): MapFragment {
            return MapFragment()
        }
    }
}
