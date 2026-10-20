package com.group3.lostandfound.repository

import android.net.Uri
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.group3.lostandfound.model.ItemReport
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ReportRepository {

    private val db = FirebaseDatabase.getInstance().getReference("items")
    private val storage = FirebaseStorage.getInstance().getReference("item_images")

    suspend fun uploadImageAndCreateReport(
        imageUri: Uri?,
        report: ItemReport,
        onProgress: (Int) -> Unit
    ): Result<String> {
        return try {
            var imageUrl = ""
            val itemId = db.push().key ?: UUID.randomUUID().toString()
            
            if (imageUri != null) {
                val imageRef = storage.child("$itemId.jpg")
                val uploadTask = imageRef.putFile(imageUri)
                
                uploadTask.addOnProgressListener { snapshot ->
                    val progress = (100.0 * snapshot.bytesTransferred / snapshot.totalByteCount).toInt()
                    onProgress(progress)
                }.await()
                
                imageUrl = imageRef.downloadUrl.await().toString()
            }
            
            val finalReport = report.copy(itemId = itemId, imageUrl = imageUrl, timestamp = System.currentTimeMillis())
            db.child(itemId).setValue(finalReport).await()
            
            Result.success(itemId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
