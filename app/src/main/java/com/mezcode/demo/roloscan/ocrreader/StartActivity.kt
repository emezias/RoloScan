package com.mezcode.demo.roloscan.ocrreader

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.*
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import com.google.android.material.snackbar.Snackbar
import android.text.TextUtils
import android.util.Log
import android.util.SparseArray
import android.view.Gravity
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.text.TextRecognizer
import java.io.File
import java.io.IOException

/**
 * Created by emezias on 4/20/17.
 * got help from a Tensor Flow sample
 * https://github.com/GoogleCloudPlatform/cloud-vision/blob/master/android/CloudVision/app/src/main/java/com/google/sample/cloudvision/MainActivity.java
 */
class StartActivity : AppCompatActivity() {
    val tag = javaClass.simpleName
    var mPhotoUri: Uri? = null
    lateinit var mContactFields: Array<String?>
    lateinit var mDialog: ConfirmTextDialog

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true)
            actionBar.setLogo(R.drawable.logo)
            actionBar.setDisplayUseLogoEnabled(true)
        } else {
            Log.w(tag, "null action bar")
        }
    }

    fun getPhoto(v: View) {
        when (v.id) {
            R.id.start_photo -> if (requestPermission(
                    this, CAMERA_REQUEST,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
                )
            ) {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                mPhotoUri = FileProvider.getUriForFile(
                    this, applicationContext.packageName + ".provider",
                    File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), FILE_NAME)
                )
                //Log.d(TAG, "path? " + mPhotoUri.getPath());
                intent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoUri)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivityForResult(intent, CAMERA_REQUEST)
            }
            R.id.start_gallery -> if (requestPermission(
                    this,
                    GALLERY_REQUEST,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(
                    Intent.createChooser(intent, getString(R.string.gallery_prompt)),
                    GALLERY_REQUEST
                )
            }
        }
    }

    fun dlg_button(btn: View) {
        //set in dialog layout xml
        when (btn.id) {
            R.id.dlg_confirm -> {
                val tnt = Intent(applicationContext, SetContactFieldsActivity::class.java)
                tnt.putExtra(SetContactFieldsActivity.Companion.TAG, mContactFields)
                startActivity(tnt)
            }
            R.id.dlg_retry -> if (btn.tag as Boolean) {
                getPhoto(findViewById(R.id.start_photo))
            } else {
                getPhoto(findViewById(R.id.start_gallery))
            }
            R.id.dlg_clipboard -> {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip: ClipData = ClipData.newPlainText(
                    getString(R.string.scan2label),
                    (mDialog.view?.findViewById(R.id.dlg_message) as TextView).text
                )
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
            }
            R.id.dlg_share -> {
                val sendIntent = Intent()
                sendIntent.action = Intent.ACTION_SEND
                sendIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    (mDialog.view?.findViewById(R.id.dlg_message) as TextView).text
                )
                sendIntent.type = MIME_TYPE
                startActivity(
                    Intent.createChooser(
                        sendIntent,
                        resources.getText(R.string.scan2label)
                    )
                )
            }
        }
        mDialog.dismiss()
    }

    /**
     * Dispatch incoming result to the correct fragment.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    protected override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(
            TAG,
            "on activity result? " + requestCode + " result " + resultCode + " data? " + (data != null)
        )
        //Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
        if (resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                mPhotoUri = data.getData()
            }
            ReadPhotoTask(requestCode).execute()
        } else {
            Toast.makeText(this, R.string.returnError, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * This dialog will show the scanned text and allow the user to proceed or retry the photo
     * @param displayText
     * @param mIsPhoto
     */
    fun showConfirmDialog(displayText: String?, mIsPhoto: Boolean) {
        mDialog = ConfirmTextDialog.Companion.newInstance(displayText, mIsPhoto)
        mDialog.show(supportFragmentManager, "show")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
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

    /**
     * This class is to create the TextRecognizer, called by onActivityResult and passing the requestCode
     * It will put the scanned text output into a single string for the confirm text dialog
     * It builds the mContactFields array of Strings to pass to the next activity
     */
    @SuppressLint("StaticFieldLeak")
    private inner class ReadPhotoTask(code: Int) : AsyncTask<Void?, Void?, String?>() {
        val mLoadingBar: Snackbar = Snackbar.make(
            this@StartActivity.findViewById(R.id.snack_anchor),
            R.string.load,
            Snackbar.LENGTH_INDEFINITE
        )
        var mCode: Int

        init {
            val snackView: Snackbar.SnackbarLayout = mLoadingBar.view as Snackbar.SnackbarLayout
            /*val tv: TextView =
                snackView.findViewById(android.R.id.snackbar_text) as TextView
            tv.gravity = Gravity.CENTER_HORIZONTAL*/
            val indicator = ProgressBar(this@StartActivity)
            indicator.indeterminateDrawable.colorFilter =
                PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY)
            indicator.scaleY = 0.5f
            indicator.scaleX = 0.5f
            snackView.addView(indicator, 1)
            mLoadingBar.show()
            mCode = code
        }

        override fun onPostExecute(s: String?) {
            super.onPostExecute(s)
            if (isCancelled) return
            mLoadingBar.dismiss()
            Snackbar.make(
                this@StartActivity.findViewById(R.id.snack_anchor),
                mCode,
                Snackbar.LENGTH_SHORT
            ).show()
            if (!TextUtils.isEmpty(s)) {
                showConfirmDialog(s, mCode == R.string.ocr_success)
            }
        }

        override fun doInBackground(vararg params: Void?): String? {
            try {
                var bitmap: Bitmap =
                    MediaStore.Images.Media.getBitmap(contentResolver, mPhotoUri)
                bitmap = rotateImageIfRequired(
                    bitmap, this@StartActivity,
                    checkUri(this@StartActivity, mPhotoUri)
                )
                val frame = Frame.Builder().setBitmap(bitmap).build()
                val detector = TextRecognizer.Builder(this@StartActivity).build()
                val blocks: SparseArray<TextBlock> = detector.detect(frame)
                detector.release()
                val sz: Int = resources.getTextArray(R.array.labels).size
                mContactFields = arrayOfNulls(sz)
                var blk: TextBlock
                val bull = StringBuilder()
                if (blocks.size() > 0) {
                    var contactDex = 0
                    for (dex in 0 until blocks.size()) {
                        blk = blocks.valueAt(dex)
                        bull.append(blk.value).append("\n")
                        for (line in blk.components) {
                            if (contactDex < sz) mContactFields[contactDex++] =
                                line.value else mContactFields[sz - 1] = """
     ${mContactFields[sz - 1]}
     ${line.value}
     """.trimIndent()
                            //bull.append(line.getValue() + "\n");
                        }
                    } //end for loop
                    //boolean will determine if the app returns to the gallery or camera on retry
                    mCode = R.string.ocr_success
                    //Log.d(TAG, "any text? " + bull.toString());
                    return bull.toString()
                } else {
                    Log.w(TAG, "empty result")
                    mCode = R.string.no_text
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }
    }

    companion object {
        val TAG = StartActivity::class.java.simpleName
        const val CAMERA_REQUEST = 9
        const val FILE_NAME = "RoloScan.jpg"
        const val MIME_TYPE = "text/plain"
        private const val GALLERY_REQUEST = 3

        //Permissions logic modeled on cloud vision, but nested
        //https://github.com/GoogleCloudPlatform/cloud-vision/blob/master/android/CloudVision/
        fun requestPermission(
            activity: Activity, requestCode: Int, vararg permissions: String
        ): Boolean {
            var granted = true
            val permissionsNeeded = ArrayList<String>()
            for (s in permissions) {
                val permissionCheck: Int = ContextCompat.checkSelfPermission(activity, s)
                val hasPermission = permissionCheck == PackageManager.PERMISSION_GRANTED
                granted = granted and hasPermission
                if (!hasPermission) {
                    permissionsNeeded.add(s)
                }
            }
            return if (granted) {
                true
            } else {
                ActivityCompat.requestPermissions(
                    activity,
                    permissionsNeeded.toTypedArray(),
                    requestCode
                )
                false
            }
        }

        fun permissionGranted(
            requestCode: Int, permissionCode: Int, grantResults: IntArray
        ): Boolean {
            return requestCode == permissionCode && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        }

        //https://teamtreehouse.com/community/how-to-rotate-images-to-the-correct-orientation-portrait-by-editing-the-exif-data-once-photo-has-been-taken
        @Throws(IOException::class)
        fun rotateImageIfRequired(img: Bitmap, context: Context, selectedImage: Uri?): Bitmap {
            if (selectedImage!!.scheme == "content") {
                val projection = arrayOf<String>(MediaStore.Images.ImageColumns.ORIENTATION)
                val c = context.contentResolver.query(selectedImage, projection, null, null, null)
                if (c!!.count > 0 && c.moveToFirst() && c.columnCount > 0) {
                    val rotation = c.getInt(0)
                    c.close()
                    return rotateImage(img, rotation)
                }
            }
            //ExifInterface ei = new ExifInterface(checkUri(context, selectedImage));
            val inStream = context.contentResolver.openInputStream(selectedImage)
            if (inStream != null) {
                val ei = ExifInterface(inStream)
                val orientation: Int =
                    ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                Log.d(TAG, "Orientation is: $orientation")
                return when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90)
                    ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180)
                    ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270)
                    else -> img
                }
            }
            return img
        }

        private fun rotateImage(img: Bitmap, degree: Int): Bitmap {
            val matrix = Matrix()
            matrix.postRotate(degree.toFloat())
            return Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        }

        fun checkUri(context: Context, contentUri: Uri?): Uri? {
            var cursor: Cursor? = null
            return try {
                val proj = arrayOf<String>(MediaStore.Images.Media.DATA)
                cursor = context.contentResolver.query(contentUri!!, proj, null, null, null)
                if (cursor!!.count > 0 && cursor.columnCount > 0) {
                    Log.d(TAG, "found in Media Store")
                    contentUri
                } else {
                    val path = contentUri.path
                    if (File(path).exists()) contentUri else FileProvider.getUriForFile(
                        context,
                        context.applicationContext.packageName + ".provider",
                        File(
                            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                            FILE_NAME
                        )
                    )
                }
            } finally {
                cursor?.close()
            }
        }
    }
}