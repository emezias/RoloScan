package com.mezcode.demo.roloscan.ocrreader

/*
 * Copyright 2020 Google LLC
 * package com.google.mlkit.md
 * https://github.com/googlesamples/mlkit/blob/de3ab489a5441bc15f80bddac3fdbded6f960f8f/android/material-showcase/app/src/main/java/com/google/mlkit/md/Utils.kt#L1
 */

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Environment
import android.provider.ContactsContract
import android.util.Log
import android.util.Patterns
import android.view.View
import androidx.exifinterface.media.ExifInterface
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream

/** Utility class to provide helper methods.  */
object Utils {
    private const val MAX_IMAGE_DIMENSION = 1024
    private const val TAG = "Utils"

    /**
     * Interface for activity doing the scan
     * Send the scan result back to the user with these calls
     */
    interface RoloMLKit {
        val resolver: ContentResolver
        fun handleError(resourceString: Int)
        fun handleScanText(resultText: List<String>)
    }

    /**
     * This is the path with or without the SDCard and pictures directory from the system
     * @param ctx
     * @return
     */
    fun getPathForProvider(ctx: Context): File {
        return when (Environment.getExternalStorageState()) {
            Environment.MEDIA_MOUNTED -> ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
            else -> ctx.filesDir
        } //timestamp is setting a unique filename
    }

    @Throws(IOException::class)
    internal fun loadImage(contentResolver: ContentResolver, imageUri: Uri): InputImage? {
        var inputStreamForSize: InputStream? = null
        var inputStreamForImage: InputStream? = null
        try {
            inputStreamForSize = contentResolver.openInputStream(imageUri)
            var opts = BitmapFactory.Options()
            opts.inJustDecodeBounds = true
            BitmapFactory.decodeStream(inputStreamForSize, null, opts)

            opts = BitmapFactory.Options()
            opts.inSampleSize = Math.max(opts.outWidth / MAX_IMAGE_DIMENSION, opts.outHeight / MAX_IMAGE_DIMENSION)
            inputStreamForImage = contentResolver.openInputStream(imageUri)
            val decodedBitmap = BitmapFactory.decodeStream(inputStreamForImage, null, opts)/* outPadding= */
            return bitmapRotateWithExif(contentResolver, imageUri, decodedBitmap)
        } finally {
            inputStreamForSize?.close()
            inputStreamForImage?.close()
        }
    }

    private fun bitmapRotateWithExif(resolver: ContentResolver, uri: Uri, bitmap: Bitmap?): InputImage? {
        val matrix: Matrix? = when (getExifOrientationTag(resolver, uri)) {
            ExifInterface.ORIENTATION_UNDEFINED, ExifInterface.ORIENTATION_NORMAL -> null
            // Set the matrix to be null to skip the image transform.

            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> Matrix().apply { postScale(-1.0f, 1.0f) }

            ExifInterface.ORIENTATION_ROTATE_90 -> Matrix().apply { postRotate(90f) }
            ExifInterface.ORIENTATION_TRANSPOSE -> Matrix().apply { postScale(-1.0f, 1.0f) }
            ExifInterface.ORIENTATION_ROTATE_180 -> Matrix().apply { postRotate(180.0f) }
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> Matrix().apply { postScale(1.0f, -1.0f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> Matrix().apply { postRotate(-90.0f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> Matrix().apply {
                postRotate(-90.0f)
                postScale(-1.0f, 1.0f)
            }
            else -> null
            // Set the matrix to be null to skip the image transform
        }

        if (matrix != null) {
            return InputImage.fromBitmap(Bitmap.createBitmap(bitmap!!, 0, 0, bitmap.width, bitmap.height, matrix, true), 0)
        }
        return bitmap?.let { InputImage.fromBitmap(it, 0) }

    }

    private fun getExifOrientationTag(resolver: ContentResolver, imageUri: Uri): Int {
        if (ContentResolver.SCHEME_CONTENT != imageUri.scheme && ContentResolver.SCHEME_FILE != imageUri.scheme) {
            Log.w(TAG, "exif not read, scheme is not a match")
            return 0
        }

        var exif: ExifInterface? = null
        try {
            resolver.openInputStream(imageUri)?.use { inputStream -> exif = ExifInterface(inputStream) }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to open file to read rotation meta data: $imageUri", e)
        }

        return exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            ?: ExifInterface.ORIENTATION_UNDEFINED
    }


    fun showSnackbar(v: View, stringID: Int) {
        Snackbar.make(v, stringID, Snackbar.LENGTH_LONG).show()
    }


    suspend fun scanImage(callbacks: RoloMLKit, imageToScan: Uri?) = withContext(Dispatchers.Default) {
        if (imageToScan == null) return@withContext
        val image = loadImage(callbacks.resolver, imageToScan)
        if (image == null) {
            //call error and try to get another image
            callbacks.handleError(R.string.returnError)
        } else {
            val client = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            // It's probably okay to throw away the (result) task and not bother to declare
            val result = client.process(image)
                .addOnSuccessListener { visionText ->
                    // Task completed, mlKit Text tree returned
                    val textFields = mutableListOf<String>()
                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            Log.v(StartActivity.TAG, "line by line ${line.text}")
                            textFields.add(line.text)
                        }
                    }
                    callbacks.handleScanText(textFields.toList())
                    client.close()
                }
                .addOnFailureListener { e ->
                    // Task failed with an exception
                    Log.e(StartActivity.TAG, "exception scanning text ${e.message}")
                    client.close()
                    callbacks.handleError(R.string.no_text)
                }
        }
    }

    // do the contact matching things - let's use extensions
    private fun String.isName(): Boolean  = "^[a-zA-Z\\\\s]+".toRegex().matches(this)

    private fun String.isAddress(): Boolean = "/^\\s*\\S+(?:\\s+\\S+){2}/".toRegex().matches(this)

    //almost perfect email regex http://emailregex.com/
    private fun String.isEmail(): Boolean =
        "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}\\b".toRegex().matches(this)

    //from StackOverflow https://stackoverflow.com/questions/3868753/find-phone-numbers-in-python-script
    private fun String.isPhone(): Boolean =
        ("(?:(?:\\+?([1-9]|[0-9][0-9]|[0-9][0-9][0-9])\\s*(?:[.-]\\s*)?)?(?:\\(\\s*([2-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9])" +
                "\\s*\\)|([0-9][1-9]|[0-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9]))\\s*(?:[.-]\\s*)?)?" +
                "([2-9]1[02-9]|[2-9][02-9]1|[2-9][02-9]{2})\\s*(?:[.-]\\s*)?([0-9]{4})(?:\\s*(?:#|x\\.?|ext\\.?|extension)\\s*(\\d+))?\n")
            .toRegex().matches(this)
    //ouch... this regex is kind of a wild one

    enum class SpinnerIndex(val dex: Int, val key: String) {
        IND_NAME(0, ContactsContract.Intents.Insert.NAME),
        IND_PHONE(1, ContactsContract.Intents.Insert.PHONE_ISPRIMARY),
        IND_EMAIL(2, ContactsContract.Intents.Insert.EMAIL_ISPRIMARY),
        IND_TITLE(3, ContactsContract.Intents.Insert.JOB_TITLE),
        IND_COMPANY(4, ContactsContract.Intents.Insert.COMPANY),
        IND_ADDRESS(5, ContactsContract.Intents.Insert.POSTAL),
        IND_IM(6, ContactsContract.Intents.Insert.IM_HANDLE),
        IND_PHONE2(7, ContactsContract.Intents.Insert.SECONDARY_PHONE),
        IND_EMAIL2(8, ContactsContract.Intents.Insert.SECONDARY_EMAIL),
        IND_NOTES(9, ContactsContract.Intents.Insert.NOTES)
    }

    /* This method returns the spinner indices for the ScannedCardToContactActivity
    * if there is no pattern match, contact activity has to handle the default -1 index value
    * @param values - the text blocks read in by the TextDetector
    * @return - the indices for the spinners that have a value
    */
    fun guessIndices(values: List<String>): HashMap<String, SpinnerIndex?> {
        Log.d(TAG, "get Indices")
        val selected = ArrayList<SpinnerIndex>()
        //try to match each string value to a contact field label and put them into a map
        val map = HashMap<String, SpinnerIndex?>()
        var valueToSelect: SpinnerIndex?
        for (valueShown in values) {
            when {
                valueShown.isEmpty() -> valueToSelect = null

                valueShown.isName() -> {
                    // fill in the contact name first, then the title, then the company name
                    valueToSelect = SpinnerIndex.IND_NAME
                    if (map.values.contains(valueToSelect)) {
                        valueToSelect = SpinnerIndex.IND_TITLE
                    }
                    if (map.values.contains(valueToSelect)) {
                        valueToSelect = SpinnerIndex.IND_COMPANY
                    }
                }

                valueShown.isEmail() || Patterns.EMAIL_ADDRESS.matcher(valueShown).matches() -> {
                    valueToSelect = SpinnerIndex.IND_EMAIL
                    if (map.values.contains(valueToSelect)) {
                        valueToSelect = SpinnerIndex.IND_EMAIL2
                    }
                }

                valueShown.contains("@") -> {
                    valueToSelect = SpinnerIndex.IND_IM
                }

                valueShown.isPhone() || Patterns.PHONE.matcher(valueShown).matches()
                        || valueShown.contains("(") && valueShown.contains(")") -> {
                    valueToSelect = SpinnerIndex.IND_PHONE
                    if (map.values.contains(valueToSelect)) {
                        valueToSelect = SpinnerIndex.IND_PHONE2
                    }
                }

                valueShown.isAddress() || //digit to begin and end?
                        (Character.isDigit(valueShown[0]) && Character.isDigit(valueShown[valueShown.length - 1])) -> {
                    valueToSelect = SpinnerIndex.IND_ADDRESS
                }
                else -> {
                    valueToSelect = SpinnerIndex.IND_NOTES
                }
            }
            valueToSelect?.let {
                selected.add(valueToSelect)
                map[valueShown] = valueToSelect
                // The same SpinnerIndex can be associated with multiple keys (input strings)
                // there could be many SpinnerIndex.IND_NOTES
                // this logic maps one company, one job title
                // A 'notes' field of extra strings can go into the SetContactFieldsActivity form
            }

            Log.d("Utils", "setting $valueShown index $valueToSelect into map")
        } //end for loop string values
        selected.clear()
        return map
    }

}