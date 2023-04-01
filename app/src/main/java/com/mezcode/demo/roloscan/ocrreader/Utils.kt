package com.mezcode.demo.roloscan.ocrreader

/*
 * Copyright 2020 Google LLC
 * package com.google.mlkit.md
 * https://github.com/googlesamples/mlkit/blob/de3ab489a5441bc15f80bddac3fdbded6f960f8f/android/material-showcase/app/src/main/java/com/google/mlkit/md/Utils.kt#L1
 */

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
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
            client.process(image)
                .addOnSuccessListener { visionText ->
                    // Task completed, mlKit Text tree returned
                    val textFields = mutableListOf<String>()
                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            textFields.add(line.text)
                        }
                    }
                    callbacks.handleScanText(textFields.toList())
                    client.close()
                }
                .addOnFailureListener { e ->
                    // Task failed with an exception
                    Log.e(this::class.simpleName, "exception scanning text ${e.message}")
                    client.close()
                    callbacks.handleError(R.string.no_text)
                }
        }
    }

    // do the contact matching things - let's use extensions
    private fun String.isName(): Boolean  = "^[a-zA-Z\\\\s]+".toRegex().matches(this)

    private fun String.isAddress(): Boolean = "^\\s*\\S+(?:\\s+\\S+){2}/".toRegex().matches(this)

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

    enum class SpinnerIndex(val dex: Int, val key: String, val drawableResource: Int) {
        IND_NAME(0, ContactsContract.Intents.Insert.NAME, R.drawable.ic_account_circle_white_24dp),
        IND_PHONE(1, ContactsContract.Intents.Insert.PHONE, R.drawable.ic_phone_white_24dp),
        IND_EMAIL(2, ContactsContract.Intents.Insert.EMAIL, R.drawable.ic_mail_outline_white_24dp),
        IND_TITLE(3, ContactsContract.Intents.Insert.JOB_TITLE, R.drawable.ic_title_white_24dp),
        IND_COMPANY(4, ContactsContract.Intents.Insert.COMPANY, R.drawable.ic_business_white_24dp),
        IND_ADDRESS(5, ContactsContract.Intents.Insert.POSTAL, R.drawable.ic_location_on_white_24dp),
        IND_IM(6, ContactsContract.Intents.Insert.IM_HANDLE, R.drawable.ic_chat_white_24dp),
        IND_PHONE2(7, ContactsContract.Intents.Insert.SECONDARY_PHONE, R.drawable.ic_phone_white_24dp),
        IND_EMAIL2(8, ContactsContract.Intents.Insert.SECONDARY_EMAIL, R.drawable.ic_mail_outline_white_24dp),
        IND_NOTES(9, ContactsContract.Intents.Insert.NOTES, R.drawable.ic_note_add_white_24dp)
    }

    fun guessSpinnerIndex(lineOfText: String) : SpinnerIndex? {
        val valueToSelect: SpinnerIndex?
        when {
            lineOfText.isEmpty() -> valueToSelect = null

            lineOfText.isName() -> {
                // fill in the contact name first, then the title, then the company name
                valueToSelect = SpinnerIndex.IND_NAME
            }

            lineOfText.isEmail() || Patterns.EMAIL_ADDRESS.matcher(lineOfText).matches() -> {
                valueToSelect = SpinnerIndex.IND_EMAIL
            }

            lineOfText.contains("@") -> {
                valueToSelect = SpinnerIndex.IND_IM
            }

            lineOfText.isPhone() || Patterns.PHONE.matcher(lineOfText).matches()
                    || lineOfText.contains("(") && lineOfText.contains(")") -> {
                valueToSelect = SpinnerIndex.IND_PHONE
            }

            lineOfText.isAddress() || //digit to begin and end?
                    (Character.isDigit(lineOfText[0]) && Character.isDigit(lineOfText[lineOfText.length - 1])) -> {
                valueToSelect = SpinnerIndex.IND_ADDRESS
            }
            else -> {
                valueToSelect = SpinnerIndex.IND_NOTES
            }
        }
        Log.d("Utils", "setting $lineOfText index $valueToSelect into map")
        return valueToSelect
    }

}