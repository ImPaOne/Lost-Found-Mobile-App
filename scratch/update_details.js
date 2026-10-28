const fs = require('fs');

const path = 'app/src/main/java/com/group3/lostandfound/DetailsFragment.kt';
let raw = fs.readFileSync(path, 'utf8');
const isCrlf = raw.includes('\r\n');
let code = raw.replace(/\r\n/g, '\n');

// 1. Update Dialog theme and topBar in showEnlargedPhotoDialog
const oldDialogSnippet = `    private fun showEnlargedPhotoDialog(imageUrl: String, title: String) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar)`;

const newDialogSnippet = `    private fun showEnlargedPhotoDialog(imageUrl: String, title: String) {
        val dialog = Dialog(requireContext(), R.style.Theme_LostAndFound_FullScreenDialog)`;

if (!code.includes(oldDialogSnippet)) {
    console.error("Could not find oldDialogSnippet");
    process.exit(1);
}
code = code.replace(oldDialogSnippet, newDialogSnippet);

// 2. Update topBar padding logic
const oldTopBarSnippet = `        val topBarEnlarged = dialog.findViewById<View>(R.id.topBarEnlarged)
        if (topBarEnlarged != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(topBarEnlarged) { v, insets ->
                val statusBarHeight = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top
                val topPadding = if (statusBarHeight > 0) statusBarHeight + 10 else 36
                v.setPadding(v.paddingLeft, topPadding, v.paddingRight, v.paddingBottom)
                insets
            }
        }`;

const newTopBarSnippet = `        val topBarEnlarged = dialog.findViewById<View>(R.id.topBarEnlarged)
        topBarEnlarged?.bringToFront()
        if (topBarEnlarged != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(topBarEnlarged) { v, insets ->
                val statusBarHeight = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top
                val safeTop = if (statusBarHeight > 0) statusBarHeight + 12 else (v.resources.displayMetrics.density * 36).toInt()
                v.setPadding(v.paddingLeft, safeTop, v.paddingRight, v.paddingBottom)
                insets
            }
        }`;

if (!code.includes(oldTopBarSnippet)) {
    console.error("Could not find oldTopBarSnippet");
    process.exit(1);
}
code = code.replace(oldTopBarSnippet, newTopBarSnippet);

// 3. Update dialog.setOnShowListener
const oldShowSnippet = `        dialog.setOnShowListener {
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }`;

const newShowSnippet = `        dialog.setOnShowListener {
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            topBarEnlarged?.bringToFront()
        }`;

if (!code.includes(oldShowSnippet)) {
    console.error("Could not find oldShowSnippet");
    process.exit(1);
}
code = code.replace(oldShowSnippet, newShowSnippet);

// 4. Update PhotoSliderAdapter scaleType
const oldScaleSnippet = `scaleType = ImageView.ScaleType.CENTER_CROP`;
const newScaleSnippet = `scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true`;

if (!code.includes(oldScaleSnippet)) {
    console.error("Could not find oldScaleSnippet");
    process.exit(1);
}
code = code.replace(oldScaleSnippet, newScaleSnippet);

// 5. Add click listener to cardPhotoContainer in onViewCreated
const oldBadgeEnlargeSnippet = `        badgeEnlargeHint.setOnClickListener {
            showEnlargedPhotoDialog(activeImageUrl, tvItemTitle.text.toString())
        }`;

const newBadgeEnlargeSnippet = `        badgeEnlargeHint.setOnClickListener {
            showEnlargedPhotoDialog(activeImageUrl, tvItemTitle.text.toString())
        }
        view.findViewById<View>(R.id.cardPhotoContainer)?.setOnClickListener {
            showEnlargedPhotoDialog(activeImageUrl, tvItemTitle.text.toString())
        }`;

if (!code.includes(oldBadgeEnlargeSnippet)) {
    console.error("Could not find oldBadgeEnlargeSnippet");
    process.exit(1);
}
code = code.replace(oldBadgeEnlargeSnippet, newBadgeEnlargeSnippet);

if (isCrlf) {
    code = code.replace(/\n/g, '\r\n');
}

fs.writeFileSync(path, code, 'utf8');
console.log("Successfully updated DetailsFragment.kt");

