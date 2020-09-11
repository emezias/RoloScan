package com.mezcode.demo.roloscan.ocrreader

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.mezcode.demo.roloscan.ocrreader.Utils.getPathForProvider
import com.mezcode.demo.roloscan.ocrreader.Utils.scanImage
import com.mezcode.demo.roloscan.ocrreader.Utils.showSnackbar
import kotlinx.android.synthetic.main.activity_start.*
import kotlinx.coroutines.launch
import java.io.File


/**
 * Created by emezias on 4/20/17 for DC DevFest code lab
 * Also found for sale on the Android Play Store
 * converting Java to Kotlin 7/2020
 * This is the main class of the demo project, it shows 2 buttons
 * The user can take a photo of some text or choose a photo on the device
 */
class StartActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback, Utils.RoloMLKit {
    companion object {
        val TAG = StartActivity::class.simpleName
        const val FILE_NAME = "RoloScan.jpg"
        const val IMAGE_TYPE = "image/*"
        private const val GALLERY_REQUEST = 3
        private const val CAMERA_REQUEST = 9
    }

    private val PERMISSIONS_REQUESTED by lazy {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA)
    }
    var mPhotoUri: Uri? = null
    lateinit var mDialog: ConfirmTextDialog
    private var galleryRequest = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        //FirebaseApp.initializeApp(applicationContext)
        supportActionBar?.apply {
            setDisplayShowHomeEnabled(true)
            setLogo(R.drawable.logo)
            setDisplayUseLogoEnabled(true)
        }
    }

    /**
     * This method returns photo from camera app or file from chooser
     * @param requestCode request code of calling activity
     * @param resultCode result of activity that was called
     * @param tnt Intent returned as a result
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, tnt: Intent?) {
        super.onActivityResult(requestCode, resultCode, tnt)
        Log.d(TAG, "on activity result? " + requestCode + " result " + resultCode + " data? " + (tnt != null))
        if (resultCode == Activity.RESULT_OK) {
            if (tnt != null && tnt.data != null && galleryRequest) {
                mPhotoUri = tnt.data
                //TODO better URI error handling
            }
            start_progress.visibility = View.VISIBLE
            //now call the image processing
            Log.v(TAG, "user selected ${mPhotoUri?.path}")
            //end the coroutine if the user leaves the activity
            lifecycleScope.launch { scanImage(this@StartActivity, mPhotoUri, galleryRequest) }
        } else {
            showSnackbar(snack_anchor, R.string.returnError)
        }
    }

    /**
     * Slightly deprecated manner of handling permissions
     * Changed this version so that only the camera button will request permissions
     * @param requestCode - standard parameters
     * @param permissions
     * @param grantResults - array of results determines if there's an error or if the camera is opened
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST && grantResults.isNotEmpty() && grantResults.all { it == PERMISSION_GRANTED }) {
            takePhotoWithPerms()
        } else {
            showSnackbar(snack_anchor, R.string.permissionHelp)
        }
    }

    //RoloTextImage interface implementation, get scan results in front of the user
    override val ctx: Context
        get() = applicationContext

    override fun handleError(resourceString: Int) {
        showSnackbar(snack_anchor, resourceString)
    }

    override fun handleScanText(resultText: Array<String>) {
        showConfirmDialog(resultText)
    }

    /**
     * This method gets a file URI from the Provider service and opens a camera app with it
     */
    private fun takePhotoWithPerms() {
        start_photo.tag = true
        mPhotoUri = getUriForScan()

        with(Intent(MediaStore.ACTION_IMAGE_CAPTURE), {
            putExtra(MediaStore.EXTRA_OUTPUT, mPhotoUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (resolveActivity(packageManager) != null) {
                startActivityForResult(this, CAMERA_REQUEST)
            } //else camera not found message to user
        })
    }

    /**
     * Read the button field that was pressed as input by the user
     * open the camera or the image chooser
     * this listener is set in the xml onClick tag
     */
    fun getPhoto(v: View) {
        when (v) {
            start_photo ->
                //check permissions every time
                when {
                    PERMISSIONS_REQUESTED.all { ContextCompat.checkSelfPermission(this, it) == PERMISSION_GRANTED } -> {
                        //all permissions to take a photo are granted
                        takePhotoWithPerms()
                    }
                    PERMISSIONS_REQUESTED.any { shouldShowRequestPermissionRationale(it) } -> {
                        showInContextUI() //dialog will call permissions request
                    }
                    else -> {
                        ActivityCompat.requestPermissions(this,
                                PERMISSIONS_REQUESTED,
                                CAMERA_REQUEST)
                        galleryRequest = false
                    }
                } //end when, end start photo
            start_gallery -> {
                galleryRequest = true
                Log.d(TAG, "on activity result? launch here")
                openImagePicker(this)
            }
        }
    }


    private fun showInContextUI() {
        Snackbar.make(start_photo, R.string.permissionHelp, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.makeRequest) {
                    // makes permission request
                    ActivityCompat.requestPermissions(this,
                            PERMISSIONS_REQUESTED,
                            CAMERA_REQUEST)
                }.show()
    }

    /**
     * This dialog will show the scanned text and allow the user to proceed or retry
     * @param displayText
     * @param mIsPhoto
     */
    private fun showConfirmDialog(displayText: Array<String>) {
        mDialog = ConfirmTextDialog.newInstance(displayText, galleryRequest)
        mDialog.show(supportFragmentManager, "showConfirmDialog")
        start_progress.visibility = View.GONE
    }

    /**
     * This method looks for a shared external directory
     * It uses the app's "file" directory if that doesn't exist
     * @return file URI to pass to the camera app
     */
    private fun getUriForScan(): Uri {
        val tmp = File(getPathForProvider(this@StartActivity), FILE_NAME + System.currentTimeMillis())
        //timestamp is setting a unique filename
        // Samsung fix https://gist.github.com/dirkvranckaert/70bc6812fe0388c8fe4f3bd5c56068c4
        val resInfoList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val fileUri = FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", tmp)
        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            grantUriPermission(packageName, fileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", tmp)
        //TODO test - does this URI create call work better?
        // contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, this)
    }

    /**
     * This method answers the click on the Gallery button
     * @param activity - Context needed to start another Activity
     */
    private fun openImagePicker(activity: Activity) {
        with(Intent(Intent.ACTION_GET_CONTENT)) {
            this.addCategory(Intent.CATEGORY_OPENABLE)
            this.type = IMAGE_TYPE
            activity.startActivityForResult(this, GALLERY_REQUEST)
        }
    }
}
