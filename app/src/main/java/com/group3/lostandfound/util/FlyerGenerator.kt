package com.group3.lostandfound.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.group3.lostandfound.R
import com.group3.lostandfound.model.ItemReport
import java.io.File
import java.io.FileOutputStream

/**
 * Utility for generating high-resolution Lost & Found community flyers
 * featuring an embedded scannable QR Code that deep links directly into the app.
 */
object FlyerGenerator {

    /**
     * Generates a standard ISO QR code bitmap for any string payload using ZXing.
     */
    fun generateQrBitmap(content: String, size: Int = 400): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    /**
     * Renders an item report into a printable, shareable poster flyer bitmap.
     */
    fun generateFlyerBitmap(context: Context, report: ItemReport, itemPhotoBitmap: Bitmap? = null): Bitmap {
        val inflater = LayoutInflater.from(context)
        val flyerView = inflater.inflate(R.layout.layout_item_flyer, null, false)

        val banner = flyerView.findViewById<View>(R.id.flyerStatusBanner)
        val tvStatusAlert = flyerView.findViewById<TextView>(R.id.tvFlyerStatusAlert)
        val imgPhoto = flyerView.findViewById<ImageView>(R.id.imgFlyerPhoto)
        val tvTitle = flyerView.findViewById<TextView>(R.id.tvFlyerItemTitle)
        val tvLocation = flyerView.findViewById<TextView>(R.id.tvFlyerLocation)
        val tvCategory = flyerView.findViewById<TextView>(R.id.tvFlyerCategory)
        val tvDescription = flyerView.findViewById<TextView>(R.id.tvFlyerDescription)
        val imgQr = flyerView.findViewById<ImageView>(R.id.imgFlyerQrCode)
        val tvDirectLink = flyerView.findViewById<TextView>(R.id.tvFlyerDirectLink)
        val tvReporterTag = flyerView.findViewById<TextView>(R.id.tvFlyerReporterTag)

        val isLost = report.status.equals("LOST", ignoreCase = true)
        val isFound = report.status.equals("FOUND", ignoreCase = true)

        if (isLost) {
            banner.setBackgroundColor(Color.parseColor("#D43838"))
            tvStatusAlert.text = "⚠️ LOST ITEM ALERT"
        } else if (isFound) {
            banner.setBackgroundColor(Color.parseColor("#3B7A57"))
            tvStatusAlert.text = "✅ FOUND ITEM NOTICE"
        } else {
            banner.setBackgroundColor(Color.parseColor("#624E43"))
            tvStatusAlert.text = "📌 ${report.status.uppercase()} REPORT"
        }

        if (itemPhotoBitmap != null) {
            imgPhoto.setImageBitmap(itemPhotoBitmap)
        } else {
            imgPhoto.setImageResource(R.drawable.ic_item_placeholder)
        }

        tvTitle.text = report.title.ifEmpty { "Reported Item" }
        tvLocation.text = report.location.ifEmpty { "Colombo, Western Province" }
        tvCategory.text = (report.category.ifEmpty { "GENERAL" }).uppercase()
        tvDescription.text = report.description.ifEmpty { "No extra details specified. Scan QR code to verify or contact the reporter." }

        val deepLinkUri = "foundly://item/${report.itemId}"
        val qrBitmap = generateQrBitmap(deepLinkUri, size = 360)
        imgQr.setImageBitmap(qrBitmap)

        tvDirectLink.text = deepLinkUri
        tvReporterTag.text = "Reported by ${report.authorName.ifEmpty { "Community Member" }}"

        // Measure & render to Bitmap
        val targetWidthPx = 720
        val widthSpec = View.MeasureSpec.makeMeasureSpec(targetWidthPx, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        flyerView.measure(widthSpec, heightSpec)
        flyerView.layout(0, 0, flyerView.measuredWidth, flyerView.measuredHeight)

        val outputBitmap = Bitmap.createBitmap(flyerView.measuredWidth, flyerView.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)
        flyerView.draw(canvas)

        return outputBitmap
    }

    /**
     * Saves a flyer bitmap to the app cache directory under /flyers/ for FileProvider sharing.
     */
    fun saveFlyerToCache(context: Context, bitmap: Bitmap, itemId: String): Uri? {
        return try {
            val cacheFolder = File(context.cacheDir, "flyers")
            if (!cacheFolder.exists()) {
                cacheFolder.mkdirs()
            }
            val sanitizedId = itemId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val flyerFile = File(cacheFolder, "flyer_${sanitizedId}.png")
            FileOutputStream(flyerFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                flyerFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Displays a modern preview dialog of the generated flyer with share & copy actions.
     */
    fun showFlyerShareDialog(activity: FragmentActivity, report: ItemReport, itemPhotoBitmap: Bitmap? = null) {
        val dialog = BottomSheetDialog(activity)
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_share_flyer, null)
        dialog.setContentView(dialogView)

        val imgPreview = dialogView.findViewById<ImageView>(R.id.imgFlyerPreview)
        val btnClose = dialogView.findViewById<View>(R.id.btnCloseFlyerDialog)
        val btnCopy = dialogView.findViewById<View>(R.id.btnCopyFlyerLink)
        val btnShare = dialogView.findViewById<View>(R.id.btnShareFlyerAction)

        val flyerBitmap = generateFlyerBitmap(activity, report, itemPhotoBitmap)
        imgPreview.setImageBitmap(flyerBitmap)

        val deepLink = "foundly://item/${report.itemId}"
        val webLink = "https://foundly.lk/item/${report.itemId}"

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        btnCopy.setOnClickListener {
            val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Foundly Item Link", deepLink)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(activity, "Link copied to clipboard: $deepLink", Toast.LENGTH_SHORT).show()
        }

        btnShare.setOnClickListener {
            val flyerUri = saveFlyerToCache(activity, flyerBitmap, report.itemId)
            val shareText = "📢 ${report.status}: ${report.title}\n📍 Location: ${report.location}\n\nScan the QR code on the flyer or open in Foundly: $deepLink\n\nShared via Foundly App - Reconnecting Sri Lanka"

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                if (flyerUri != null) {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, flyerUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    type = "text/plain"
                }
                putExtra(Intent.EXTRA_SUBJECT, "${report.status}: ${report.title} - Foundly LK")
                putExtra(Intent.EXTRA_TEXT, shareText)
            }

            activity.startActivity(Intent.createChooser(shareIntent, "Share Item Flyer via"))
            dialog.dismiss()
        }

        dialog.show()
    }
}
