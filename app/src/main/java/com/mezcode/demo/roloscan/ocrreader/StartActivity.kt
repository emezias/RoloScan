package com.mezcode.demo.roloscan.ocrreader

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import com.google.firebase.FirebaseApp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import kotlinx.android.synthetic.main.activity_start.*
import kotlinx.android.synthetic.main.dialog_confirm.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.*

/**
 * Created by emezias on 4/20/17 for DC DevFest code lab
 * Also found for sale on the Android Play Store
 * converting Java to Kotlin 7/2020
 */
class StartActivity : AppCompatActivity() {
    companion object {
        val TAG = StartActivity::class.simpleName
        const val FILE_NAME = "RoloScan.jpg"
        const val MIME_TYPE = "text/plain"
        const val IMAGE_TYPE = "image/*"
        private const val GALLERY_REQUEST = 3
        private const val CAMERA_REQUEST = 9
        private const val PERMISSION_REQUESTS = 1
    }

    private val PERMISSIONS_REQUESTED by lazy {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA)
    }
    var mPhotoUri: Uri? = null
    var mContactFields: ArrayList<String?> = ArrayList()
    var mDialog: ConfirmTextDialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        FirebaseApp.initializeApp(applicationContext)
        supportActionBar?.apply {
            setDisplayShowHomeEnabled(true)
            setLogo(R.drawable.logo)
            setDisplayUseLogoEnabled(true)
        }
    }

    /**
     * Read the button field that was pressed as input by the user
     * open the camera or the image chooser
     * this listener is set in the xml onClick tag
     */
    fun getPhoto(v: View) {
        when (v) {
            start_photo ->
                //API says no call to show the explanation dialog
                when {
                PERMISSIONS_REQUESTED.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED } -> {
                    //have the permissions needed to take a photo
                    mPhotoUri = getUriForScan()
                    with(Intent(MediaStore.ACTION_IMAGE_CAPTURE), {
                        putExtra(MediaStore.EXTRA_OUTPUT, mPhotoUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        startActivityForResult(this, CAMERA_REQUEST)
                    })
                } //end permission granted
                PERMISSIONS_REQUESTED.none { shouldShowRequestPermissionRationale(it) } -> requestMultiplePermissions.launch(PERMISSIONS_REQUESTED)
                else -> {
                    showInContextUI() //dialog will call permissions request
                }
            } //end start photo
            start_gallery -> {
                Log.d(TAG, "on activity result? launch here")
                requestExistingFile.launch(arrayOf(IMAGE_TYPE))
                //Returns file or content URI
            }
        }
    }


    private fun showInContextUI() {
        Snackbar.make(start_photo, R.string.permissionHelp, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.makeRequest) {
                    // makes permission request
                    requestMultiplePermissions.launch(PERMISSIONS_REQUESTED)
                }.show()
    }

    /**
     * this OnClickListener is set in xml
     * Toast response in case of copy, otherwise startActivity
     */
    fun dialogResponseButton(btn: View) {
        //set in dialog layout xml, mDialog cannot be null because its views are on the screen
        when (btn) {
            dlg_confirm -> {
                val tnt = Intent(applicationContext, SetContactFieldsActivity::class.java)
                tnt.putExtra(SetContactFieldsActivity.TAG, mContactFields)
                startActivity(tnt)
            }
            dlg_retry -> if (btn.tag as Boolean) {
                getPhoto(start_photo)
            } else {
                getPhoto(start_gallery)
            }
            dlg_clipboard -> {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(getString(R.string.scan2label), dlg_message.text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
            }
            dlg_share -> {
                val sendIntent = Intent()
                sendIntent.action = Intent.ACTION_SEND
                sendIntent.putExtra(Intent.EXTRA_TEXT, dlg_message.text)
                sendIntent.type = MIME_TYPE
                startActivity(Intent.createChooser(sendIntent, resources.getText(R.string.scan2label)))
            }
        }
        mDialog?.dismiss()
    }

    /**
     * Dispatch incoming result to the correct fragment.
     * This method returns photo from camera app or file from chooser
     * @param requestCode request code of calling activity
     * @param resultCode result of activity that was called
     * @param tnt Intent returned as a result
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, tnt: Intent?) {
        super.onActivityResult(requestCode, resultCode, tnt)
        Log.d(TAG, "on activity result? " + requestCode + " result " + resultCode + " data? " + (tnt != null))
        if (resultCode == Activity.RESULT_OK) {
            if (tnt != null && tnt.data != null) {
                mPhotoUri = tnt.data
                //TODO better URI error handling
            }
            try {
                firebaseInBackground(requestCode)
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e(TAG, "Exception reading text from photo: " + e.message)
            }
        } else {
            Toast.makeText(this, R.string.returnError, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * This dialog will show the scanned text and allow the user to proceed or retry the photo
     * @param displayText
     * @param mIsPhoto
     */
    private fun showConfirmDialog(displayText: String, mIsPhoto: Boolean) {
        mDialog = ConfirmTextDialog.newInstance(displayText, mIsPhoto)
        mDialog?.show(supportFragmentManager, "showConfirmDialog")
    }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_REQUEST -> if (permissionGranted(requestCode, CAMERA_REQUEST, grantResults)) {
                getPhoto(findViewById(R.id.start_photo))
            } else {
                Toast.makeText(this, R.string.permissionHelp, Toast.LENGTH_LONG).show()
            }
            GALLERY_REQUEST -> if (permissionGranted(requestCode, GALLERY_REQUEST, grantResults)) {
                getPhoto(findViewById(R.id.start_gallery))
            } else {
                Toast.makeText(this, R.string.permissionHelp, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun permissionGranted(
            requestCode: Int, permissionCode: Int, grantResults: IntArray): Boolean {
        return requestCode == permissionCode && grantResults.isNotEmpty() && grantResults.all {it == PackageManager.PERMISSION_GRANTED}
    }
    /**
     * Called by onActivityResult with the requestCode to create the TextDetector
     * It will put the scanned text output into a single string for the confirm text dialog
     * It builds the mContactFields array of Strings to pass to the next activity
     * @param mCode - Camera or Gallery Request
     * @throws IOException
     */
    @Throws(IOException::class)
    fun firebaseInBackground(mCode: Int) {
        val mLoadingBar = Snackbar.make(snack_anchor,
                R.string.load,
                Snackbar.LENGTH_INDEFINITE)
        val snack_view = mLoadingBar.view as SnackbarLayout
        val tv = snack_view.findViewById<View>(com.google.android.material.R.id.snackbar_text) as TextView
        tv.gravity = Gravity.CENTER_HORIZONTAL
        val indicator = ProgressBar(this@StartActivity)
        indicator.indeterminateDrawable.colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY)
        indicator.scaleY = 0.5f
        indicator.scaleX = 0.5f
        snack_view.addView(indicator, 1)
        mLoadingBar.show()
        var bitmap = MediaStore.Images.Media.getBitmap(contentResolver, mPhotoUri)
        /*bitmap = rotateImageIfRequired(bitmap, this@StartActivity,
                checkUri(this@StartActivity, mPhotoUri))
        //TODO use rotation in fromBitmap instead of rotateImage method
        val image = RoloTextImage.imageFromBitmap(bitmap)*/
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = TextRecognition.getClient().process(image)
                .addOnSuccessListener { firebaseVisionText -> // Task completed successfully
                    mLoadingBar.dismiss()
                    val displayText = StringBuilder()
                    val textArray: List<Text.TextBlock> = firebaseVisionText.textBlocks.filterNotNull()
                    if (textArray.isNotEmpty()) {
                        mContactFields = ArrayList<String?>(textArray.size)
                        for (block in textArray) {
                            mContactFields.add(block.toString())
                            displayText.append(block.toString()).append("/n")
                        }
                        Snackbar.make(findViewById(R.id.snack_anchor), R.string.ocr_success, Snackbar.LENGTH_SHORT).show()
                        showConfirmDialog(displayText.toString(), mCode == R.string.ocr_success)
                    }
                }
                .addOnFailureListener { // Task failed with an exception
                    Log.w(TAG, "empty result")
                    Snackbar.make(findViewById(R.id.snack_anchor), R.string.no_text, Snackbar.LENGTH_SHORT).show()
                }
    }


    /**
     * This method looks for a shared external directory
     * It uses the app's "file" directory if that doesn't exist
     * @return file URI to pass to the camera app
     */
    private fun getUriForScan(): Uri {
        val tmp =
                if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
                    File(filesDir, (FILE_NAME + System.currentTimeMillis()))
                } else {
                    File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), (FILE_NAME + System.currentTimeMillis()))
                } //timestamp is setting a unique filename
        return FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", tmp)
    }

    private suspend fun scanImage(imageToScan: Uri) = withContext(Dispatchers.Default) {

        val image = imageFromPhotoURI(this@StartActivity, imageToScan)
        if (image == null) {
            Toast.makeText(this@StartActivity, R.string.returnError, Toast.LENGTH_LONG).show()
            return@withContext
        }
        // It's probably okay to throw away the (result) task, TBD
        val result = TextRecognition.getClient().process(image)
                .addOnSuccessListener { visionText ->
                    // Task completed, mlKit Text tree returned
                    var text: String = ""
                    for (block in visionText.textBlocks) {
                        text = block.text
                        Log.v(TAG, text.toString())
                        for (line in block.lines) {
                            Log.v(TAG, "line by line $line")
                            for (element in line.elements) {
                                Log.v(TAG, "elements $element")
                            }
                        }
                        mDialog = ConfirmTextDialog.newInstance(text, true)
                        mDialog?.show(supportFragmentManager, "showConfirmDialog")
                    }
                }
                .addOnFailureListener { e ->
                    // Task failed with an exception
                    Log.e(TAG, "exception scanning text ${e.message}")
                }
    }

    // Register the permissions callback to get results from the system permissions dialog.
    private val requestMultiplePermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                permissions.entries.forEach {
                    Log.e("DEBUG", "${it.key} = ${it.value}")
                }
            } //READ_EXTERNAL_STORAGE permission helps when reading from another app, the camera app


    private val requestExistingFile =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { document ->
            Log.d(TAG, "on activity result? register here")
            if (document == null) {
                Toast.makeText(this, R.string.returnError, Toast.LENGTH_LONG).show()
            } else {
                //now call the image processing
                Log.v(TAG, "user selected ${document.path}")
                //end the coroutine if the user leaves the activity
                lifecycleScope.launch { scanImage(document) }
            }
        }
}