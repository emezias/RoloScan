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
import android.util.Log
import android.view.View
import androidx.exifinterface.media.ExifInterface
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
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
        val ctx: Context
        fun handleError(resourceString: Int)
        fun handleScanText(resultText: Array<String>)
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
    internal fun loadImage(context: Context, imageUri: Uri): InputImage? {
        var inputStreamForSize: InputStream? = null
        var inputStreamForImage: InputStream? = null
        try {
            inputStreamForSize = context.contentResolver.openInputStream(imageUri)
            var opts = BitmapFactory.Options()
            opts.inJustDecodeBounds = true
            BitmapFactory.decodeStream(inputStreamForSize, null, opts)

            opts = BitmapFactory.Options()
            opts.inSampleSize = Math.max(opts.outWidth / MAX_IMAGE_DIMENSION, opts.outHeight / MAX_IMAGE_DIMENSION)
            inputStreamForImage = context.contentResolver.openInputStream(imageUri)
            val decodedBitmap = BitmapFactory.decodeStream(inputStreamForImage, null, opts)/* outPadding= */
            return bitmapRotateWithExif(context.contentResolver, imageUri, decodedBitmap)
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
        return bitmap?.let { InputImage.fromBitmap(it, 0) };

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


    suspend fun scanImage(handler: RoloMLKit, imageToScan: Uri?, galleryRequest: Boolean) = withContext(Dispatchers.Default) {
        if (imageToScan == null) return@withContext
        val image = loadImage(handler.ctx, imageToScan)
        if (image == null) {
            //call error and try to get another image
            handler.handleError(R.string.returnError)
        } else {
            val client = TextRecognition.getClient()
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
                        handler.handleScanText(textFields.toTypedArray())
                        client.close()
                    }
                    .addOnFailureListener { e ->
                        // Task failed with an exception
                        Log.e(StartActivity.TAG, "exception scanning text ${e.message}")
                        client.close()
                        handler.handleError(R.string.no_text)
                    }
        }
    }
}