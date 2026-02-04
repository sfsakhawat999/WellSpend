package com.h2.wellspend.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.h2.wellspend.data.model.GitHubRelease
import com.h2.wellspend.data.repository.UpdateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class UpdateState {
    data object Initial : UpdateState()
    data object Loading : UpdateState()
    data class Available(val release: GitHubRelease) : UpdateState()
    data object NoUpdate : UpdateState()
    data class Error(val message: String) : UpdateState()
}

class UpdateViewModel(
    private val repository: UpdateRepository
) : ViewModel() {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Initial)
    val updateState: StateFlow<UpdateState> = _updateState

    fun checkForUpdates(forceCheck: Boolean = false) {
        viewModelScope.launch {
            _updateState.value = UpdateState.Loading
            // Hardcoded for now, we can move these to build config or constants later
            val result = repository.checkForUpdate(
                owner = "sfsakhawat999",
                repo = "WellSpend",
                forceCheck = forceCheck
            )
            
            result.onSuccess { release ->
                if (release != null) {
                    _updateState.value = UpdateState.Available(release)
                } else {
                    _updateState.value = UpdateState.NoUpdate
                }
            }.onFailure { e ->
                _updateState.value = UpdateState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun downloadApk(context: Context, url: String, fileName: String) {
        val request = android.app.DownloadManager.Request(android.net.Uri.parse(url))
        request.setTitle("Downloading Update")
        request.setDescription("Downloading $fileName")
        request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName)
        request.setMimeType("application/vnd.android.package-archive")
        
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
        manager.enqueue(request)
        
        android.widget.Toast.makeText(context, "Download started...", android.widget.Toast.LENGTH_SHORT).show()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UpdateViewModel(UpdateRepository(context.applicationContext)) as T
        }
    }
}
