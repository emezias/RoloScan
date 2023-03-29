package com.mezcode.demo.roloscan.ocrreader

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mezcode.demo.roloscan.ocrreader.Utils.showSnackbar
import com.mezcode.demo.roloscan.ocrreader.databinding.ActivityStartBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

const val FILE_NAME = "RoloScan"
const val IMAGE_TYPE = "image/*"
/**
 * Created by emezias on 4/20/17 for DC DevFest code lab
 * Also found for sale on the Android Play Store
 * This is the main class of the demo project that shows 2 buttons
 * You can take a photo or choose a photo on the device
 * The logic will scan the image and extract text
 * This post guided the refactor to Kotlin ActivityResult callbacks
 * https://medium.com/codex/how-to-use-the-android-activity-result-api-for-selecting-and-taking-images-5dbcc3e6324b
 */
@AndroidEntryPoint
class StartActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    private val TAG = this::class.simpleName
    private val viewModel: OCRViewModel by viewModels()
    private var latestTmpUri: Uri? = null
    private lateinit var progressBar: ProgressBar
    private lateinit var anchor: View

    // these 2 static initializers will register for result with the Activity instantiation
    private val takeImageResult = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            latestTmpUri?.let {
                viewModel.scanImageForText(it)
            } ?: run {
                showSnackbar(anchor, R.string.returnError)
            }
        }
    }

    private val selectImageFromGalleryResult = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            viewModel.scanImageForText(it)
        } ?: run {
            showSnackbar(anchor, R.string.returnError)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        with (binding) {
            startPhoto.setOnClickListener { takePhoto() }
            startGallery.setOnClickListener { selectImageFromGalleryResult.launch(IMAGE_TYPE) }
        }
        setUpStateFlow()
        progressBar = binding.startProgress
        anchor = binding.snackAnchor
        supportActionBar?.apply {
            setDisplayShowHomeEnabled(true)
            setLogo(R.drawable.logo)
            setDisplayUseLogoEnabled(true)
        }
    }

    private fun takePhoto() {
        getTmpFileUri().let { uri ->
            latestTmpUri = uri
            takeImageResult.launch(uri)
        }
    }


    //RoloTextImage interface implementation, get scan results in front of the user
    private fun setUpStateFlow() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                Log.d(TAG, "call to collect viewmodel flow")
                viewModel.myUiState.collect {
                    when(it) {
                        is OCRViewState.Success -> {
                            showConfirmDialog(it.scannedTextFields)
                        }
                        is OCRViewState.Loading -> {
                            Log.d(TAG, "Loading state")
                        }
                        is OCRViewState.Error -> {
                            showSnackbar(anchor, it.errorString ?: R.string.retry)
                        }
                    }
                }
            }
        }
    }

    /**
     * This dialog will show the scanned text and allow the user to proceed or retry
     * @param displayText the strings read from the selected photo or gallery image
     */
    private fun showConfirmDialog(displayText: List<String>) {
        // TODO fix dialog replay
        val tmp = supportFragmentManager.findFragmentByTag("ConfirmTextDialog")
        if (tmp != null) {

            (tmp as ConfirmTextDialog).dismiss()
            supportFragmentManager.beginTransaction().remove(tmp).commitNowAllowingStateLoss()
        }
        progressBar.visibility = View.GONE
        ConfirmTextDialog().show(supportFragmentManager, "ConfirmTextDialog")
    }

    private fun getTmpFileUri(): Uri {
        val tmpFile =
            File.createTempFile(FILE_NAME + System.currentTimeMillis(), ".png", cacheDir).apply {
                createNewFile()
                deleteOnExit()
            }

        return FileProvider.getUriForFile(
            applicationContext,
            "${BuildConfig.APPLICATION_ID}.provider",
            tmpFile
        )
    }

}
