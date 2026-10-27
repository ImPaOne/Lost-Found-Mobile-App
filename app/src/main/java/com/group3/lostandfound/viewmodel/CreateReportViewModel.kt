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


class CreateReportViewModel(
    private val reportRepository: ReportRepository = ReportRepository()
) : ViewModel() {

    private val _uploadState = MutableStateFlow<ReportUploadState>(ReportUploadState.Idle)
    val uploadState: StateFlow<ReportUploadState> = _uploadState.asStateFlow()

    private var uploadJob: Job? = null

    //	Starts the report submission process and sends the report and image to ReportRepository.
    fun submitReport(context: Context, imageUri: Uri?, report: ItemReport) {
        _uploadState.value = ReportUploadState.Progress(0, "Compressing photo & preparing upload...")

        uploadJob?.cancel()
        uploadJob = viewModelScope.launch {
            val result = reportRepository.uploadImageAndCreateReport(
                context = context,
                imageUri = imageUri,
                report = report,
                maxRetries = 3
            ) { progress ->//Receives upload progress and updates the status message and percentage.
                val statusText = when {
                    progress < 25 -> "Compressing image..."
                    progress < 90 -> "Uploading photo to Storage ($progress%)..."
                    progress < 100 -> "Saving report to Realtime Database..."
                    else -> "Done! Finalizing report..."
                }
                _uploadState.value = ReportUploadState.Progress(progress, statusText)
            }

            //	Handles the repository's success or failure result.
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

    //Resets the upload state to Idle.
    fun resetState() {
        _uploadState.value = ReportUploadState.Idle
    }

    //Performs cleanup when the ViewModel is permanently cleared and cancels the tracked upload job.
    override fun onCleared() {
        super.onCleared()
        uploadJob?.cancel()
    }
}
