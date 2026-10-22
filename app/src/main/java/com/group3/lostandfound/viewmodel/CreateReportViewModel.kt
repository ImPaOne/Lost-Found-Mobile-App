package com.group3.lostandfound.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group3.lostandfound.model.ItemReport
import com.group3.lostandfound.repository.ReportRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ReportUploadState {
    object Idle : ReportUploadState()
    data class Progress(val percentage: Int, val statusText: String) : ReportUploadState()
    data class Success(val report: ItemReport) : ReportUploadState()
    data class Error(val message: String) : ReportUploadState()
}

/**
 * ViewModel managing report creation, image compression, upload progress, and retries in MVVM.
 */
class CreateReportViewModel(
    private val reportRepository: ReportRepository = ReportRepository()
) : ViewModel() {

    private val _uploadState = MutableStateFlow<ReportUploadState>(ReportUploadState.Idle)
    val uploadState: StateFlow<ReportUploadState> = _uploadState.asStateFlow()

    private var uploadJob: Job? = null

    fun submitReport(context: Context, imageUri: Uri?, report: ItemReport) {
        _uploadState.value = ReportUploadState.Progress(0, "Compressing photo & preparing upload...")

        uploadJob?.cancel()
        uploadJob = viewModelScope.launch {
            val result = reportRepository.uploadImageAndCreateReport(
                context = context,
                imageUri = imageUri,
                report = report,
                maxRetries = 3
            ) { progress ->
                val statusText = when {
                    progress < 25 -> "Compressing image..."
                    progress < 90 -> "Uploading photo to Storage ($progress%)..."
                    progress < 100 -> "Saving report to Realtime Database..."
                    else -> "Done! Finalizing report..."
                }
                _uploadState.value = ReportUploadState.Progress(progress, statusText)
            }

            result.fold(
                onSuccess = { createdReport ->
                    _uploadState.value = ReportUploadState.Success(createdReport)
                },
                onFailure = { error ->
                    _uploadState.value = ReportUploadState.Error(error.localizedMessage ?: "Failed to upload report.")
                }
            )
        }
    }

    fun resetState() {
        _uploadState.value = ReportUploadState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        uploadJob?.cancel()
    }
}
