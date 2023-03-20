package com.mezcode.demo.roloscan.ocrreader

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * OCR view model
 * Shared ViewModel for Start and SetContact activities
 * Also used in the ConfirmTextDialog fragment
 * This class replaced the AsyncTask with coroutines
 * It holds any scanned text
 */
class OCRViewModel(private val contentResolver: ContentResolver) : ViewModel(), Utils.RoloMLKit {
    private val _myUiState = MutableStateFlow<OCRViewState>(OCRViewState.Loading)
    val myUiState: StateFlow<OCRViewState> = _myUiState

    override val resolver: ContentResolver
        get() = contentResolver

    override fun handleError(resourceString: Int) {
        _myUiState.value = OCRViewState.Error(resourceString)
    }

    override fun handleScanText(resultText: List<String>) {
        _myUiState.value = OCRViewState.Success(resultText)
    }
     fun scanImageForText(imageToScan: Uri?, galleryRequest: Boolean) {
         viewModelScope.launch {
             Utils.scanImage(this@OCRViewModel, imageToScan, galleryRequest)
         }
     }

}

sealed class OCRViewState {
    object Loading : OCRViewState()
    data class Success(val scannedTextFields: List<String>): OCRViewState()
    data class Error(val errorString: Int?): OCRViewState()
}