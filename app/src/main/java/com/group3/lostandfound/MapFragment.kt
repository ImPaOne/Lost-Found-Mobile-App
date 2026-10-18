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

        var currentTitle = "Red laptop sleeve"
        var currentLocation = "Independence Square"
        var currentStatus = "FOUND"
        var currentImage = R.drawable.img_wallet_details
        var currentDesc = "Padded red neoprene laptop sleeve found on park bench."

        pinCenter.setOnClickListener {
            currentTitle = "Red laptop sleeve"
            currentLocation = "Independence Square"
            currentStatus = "FOUND"
            currentImage = R.drawable.img_wallet_details
            currentDesc = "Padded red neoprene laptop sleeve found on park bench near walking path."

            textItemTitle.text = currentTitle
            textItemLocation.text = currentLocation
            tagFoundStatus.text = currentStatus
        }

        pinTopLeft.setOnClickListener {
            currentTitle = "Black leather wallet"
            currentLocation = "Kollupitiya"
            currentStatus = "LOST"
            currentImage = R.drawable.img_wallet
            currentDesc = "Slim bifold wallet with national ID and driving license."

            textItemTitle.text = currentTitle
            textItemLocation.text = currentLocation
            tagFoundStatus.text = currentStatus
        }

        pinTopRight.setOnClickListener {
            currentTitle = "Sri Lankan passport"
            currentLocation = "Fort Station"
            currentStatus = "FOUND"
            currentImage = R.drawable.img_passport
            currentDesc = "Sri Lankan passport found on waiting hall chair at Fort Railway Station."

            textItemTitle.text = currentTitle
            textItemLocation.text = currentLocation
            tagFoundStatus.text = currentStatus
        }

        pinBottomRight.setOnClickListener {
            currentTitle = "AirPods Pro Case"
            currentLocation = "Bambalapitiya"
            currentStatus = "FOUND"
            currentImage = R.drawable.img_wallet_details
            currentDesc = "White AirPods Pro case left behind at cafe table."

            textItemTitle.text = currentTitle
            textItemLocation.text = currentLocation
            tagFoundStatus.text = currentStatus
        }

        cardLaptopItem.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.screenBox,
                    DetailsFragment.newInstance(
                        title = currentTitle,
                        desc = currentDesc,
                        status = currentStatus,
                        location = currentLocation,
                        imageResId = currentImage
                    )
                )
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
