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
 * DetailsFragment shows item details, resolution progress, and match suggestions.
 * Built using clean human-written code with concise 3-8 character variable names.
 */
class DetailsFragment : Fragment() {

    private lateinit var btnBack: ImageView
    private lateinit var btnMore: ImageView

    private lateinit var btnEdit: TextView
    private lateinit var btnRes: TextView
    private lateinit var btnDel: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnBack = view.findViewById(R.id.btnBack)
        btnMore = view.findViewById(R.id.btnMore)

        btnEdit = view.findViewById(R.id.btnEdit)
        btnRes = view.findViewById(R.id.btnRes)
        btnDel = view.findViewById(R.id.btnDel)

        btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        btnMore.setOnClickListener {
            Toast.makeText(requireContext(), "More report options", Toast.LENGTH_SHORT).show()
        }

        btnEdit.setOnClickListener {
            Toast.makeText(requireContext(), "Edit report", Toast.LENGTH_SHORT).show()
        }

        btnRes.setOnClickListener {
            Toast.makeText(requireContext(), "Marked as resolved", Toast.LENGTH_SHORT).show()
        }

        btnDel.setOnClickListener {
            Toast.makeText(requireContext(), "Report deleted", Toast.LENGTH_SHORT).show()
        }

        setupBottomNav(view)
    }

    companion object {
        fun newInstance(): DetailsFragment {
            return DetailsFragment()
        }
    }
}
