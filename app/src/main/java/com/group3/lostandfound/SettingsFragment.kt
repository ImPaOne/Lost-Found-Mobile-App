package com.group3.lostandfound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    private lateinit var btnBack: ImageView
    private lateinit var btnHeaderMenu: FrameLayout
    private lateinit var switchNotifications: SwitchCompat

    private lateinit var rowEditProfile: LinearLayout
    private lateinit var rowChangePassword: LinearLayout
    private lateinit var rowNotificationPrefs: LinearLayout

    private lateinit var rowProfileVisibility: LinearLayout
    private lateinit var rowBlockList: LinearLayout
    private lateinit var rowExportData: LinearLayout

    private lateinit var rowLanguage: LinearLayout
    private lateinit var rowTheme: LinearLayout
    private lateinit var rowAbout: LinearLayout
    private lateinit var rowRateApp: LinearLayout
    private lateinit var rowLogout: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnBack = view.findViewById(R.id.btnBack)
        btnHeaderMenu = view.findViewById(R.id.btnHeaderMenu)
        switchNotifications = view.findViewById(R.id.switchNotifications)

        rowEditProfile = view.findViewById(R.id.rowEditProfile)
        rowChangePassword = view.findViewById(R.id.rowChangePassword)
        rowNotificationPrefs = view.findViewById(R.id.rowNotificationPrefs)

        rowProfileVisibility = view.findViewById(R.id.rowProfileVisibility)
        rowBlockList = view.findViewById(R.id.rowBlockList)
        rowExportData = view.findViewById(R.id.rowExportData)

        rowLanguage = view.findViewById(R.id.rowLanguage)
        rowTheme = view.findViewById(R.id.rowTheme)
        rowAbout = view.findViewById(R.id.rowAbout)
        rowRateApp = view.findViewById(R.id.rowRateApp)
        rowLogout = view.findViewById(R.id.rowLogout)

        // Setup bottom navigation bar
        setupBottomNav(view, "profile")

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnHeaderMenu.setOnClickListener {
            Toast.makeText(requireContext(), "Settings options", Toast.LENGTH_SHORT).show()
        }

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            val status = if (isChecked) "enabled" else "disabled"
            Toast.makeText(requireContext(), "Notifications $status", Toast.LENGTH_SHORT).show()
        }

        rowEditProfile.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, TrustFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        rowChangePassword.setOnClickListener {
            Toast.makeText(requireContext(), "Change Password", Toast.LENGTH_SHORT).show()
        }

        rowNotificationPrefs.setOnClickListener {
            switchNotifications.toggle()
        }

        rowProfileVisibility.setOnClickListener {
            Toast.makeText(requireContext(), "Profile Visibility: Community", Toast.LENGTH_SHORT).show()
        }

        rowBlockList.setOnClickListener {
            Toast.makeText(requireContext(), "Block List (2 blocked users)", Toast.LENGTH_SHORT).show()
        }

        rowExportData.setOnClickListener {
            Toast.makeText(requireContext(), "Exporting user data...", Toast.LENGTH_SHORT).show()
        }

        rowLanguage.setOnClickListener {
            Toast.makeText(requireContext(), "Language: English", Toast.LENGTH_SHORT).show()
        }

        rowTheme.setOnClickListener {
            Toast.makeText(requireContext(), "Theme: System", Toast.LENGTH_SHORT).show()
        }

        rowAbout.setOnClickListener {
            Toast.makeText(requireContext(), "Lost & Found App v1.0", Toast.LENGTH_SHORT).show()
        }

        rowRateApp.setOnClickListener {
            Toast.makeText(requireContext(), "Rate on Google Play Store", Toast.LENGTH_SHORT).show()
        }

        rowLogout.setOnClickListener {
            Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }
}
