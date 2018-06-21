package com.mezcode.demo.roloscan.ocrreader;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.media.ExifInterface;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by emezias on 4/20/17.
 * got help from a Tensor Flow sample
 * https://github.com/GoogleCloudPlatform/cloud-vision/blob/master/android/CloudVision/app/src/main/java/com/google/sample/cloudvision/MainActivity.java
 */

public class StartActivity extends AppCompatActivity {

    public static final String TAG = StartActivity.class.getSimpleName();
    public static final int CAMERA_REQUEST = 9;
    public static final String FILE_NAME = "RoloScan.jpg";
    public static final String MIME_TYPE = "text/plain";
    private static final int GALLERY_REQUEST = 3;
    Uri mPhotoUri;
    String[] mContactFields;
    ConfirmTextDialog mDialog;
    boolean initComplete = false;

    //Permissions logic modeled on cloud vision, but nested
    //https://github.com/GoogleCloudPlatform/cloud-vision/blob/master/android/CloudVision/
    public static boolean requestPermission(
            Activity activity, int requestCode, String... permissions) {
        boolean granted = true;
        ArrayList<String> permissionsNeeded = new ArrayList<>();

        for (String s : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(activity, s);
            boolean hasPermission = (permissionCheck == PackageManager.PERMISSION_GRANTED);
            granted &= hasPermission;
            if (!hasPermission) {
                permissionsNeeded.add(s);
            }
        }

        if (granted) {
            return true;
        } else {
            ActivityCompat.requestPermissions(activity,
                    permissionsNeeded.toArray(new String[permissionsNeeded.size()]),
                    requestCode);
            return false;
        }
    }

    public static boolean permissionGranted(
            int requestCode, int permissionCode, int[] grantResults) {
        return requestCode == permissionCode && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    }

    //https://teamtreehouse.com/community/how-to-rotate-images-to-the-correct-orientation-portrait-by-editing-the-exif-data-once-photo-has-been-taken
    public static Bitmap rotateImageIfRequired(Bitmap img, Context context, Uri selectedImage) throws IOException {

        if (selectedImage.getScheme().equals("content")) {
            String[] projection = { MediaStore.Images.ImageColumns.ORIENTATION };
            Cursor c = context.getContentResolver().query(selectedImage, projection, null, null, null);
            if (c.getCount() > 0 && c.moveToFirst() && c.getColumnCount() > 0) {
                final int rotation = c.getInt(0);
                c.close();
                return rotateImage(img, rotation);
            }
        }
        //ExifInterface ei = new ExifInterface(checkUri(context, selectedImage));
        InputStream in = context.getContentResolver().openInputStream(selectedImage);
        ExifInterface ei = new ExifInterface(in);

        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        Log.d(TAG, "Orientation is: " + orientation);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }

    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
    }

    public static Uri checkUri(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            if (cursor.getCount() > 0 && cursor.getColumnCount() > 0) {
                Log.d(TAG, "found in Media Store");
                return contentUri;
            } else {
                final String path = contentUri.getPath();
                if ((new File(path)).exists()) return contentUri;
                else return FileProvider.getUriForFile(context,
                        context.getApplicationContext().getPackageName() + ".provider",
                        new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), FILE_NAME));
            }

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        if (!initComplete) {
            FirebaseApp.initializeApp(getApplicationContext());
            initComplete = true;
        }
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setLogo(R.drawable.logo);
            actionBar.setDisplayUseLogoEnabled(true);
        }
    }

    public void getPhoto(View v) {
        switch (v.getId()) {
            case R.id.start_photo:
                if (requestPermission(this, CAMERA_REQUEST,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA)) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    mPhotoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider",
                            new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), FILE_NAME));
                    //Log.d(TAG, "path? " + mPhotoUri.getPath());
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoUri);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivityForResult(intent, CAMERA_REQUEST);
                }
                break;
            case R.id.start_gallery:
                if (requestPermission(this, GALLERY_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent, getString(R.string.gallery_prompt)),
                            GALLERY_REQUEST);
                }
                break;
        }
    }

    public void dlg_button(View btn) {
        //set in dialog layout xml
        switch (btn.getId()) {
            case R.id.dlg_confirm:
                final Intent tnt = new Intent(getApplicationContext(), SetContactFieldsActivity.class);
                tnt.putExtra(SetContactFieldsActivity.TAG, mContactFields);
                startActivity(tnt);
                break;
            case R.id.dlg_retry:
                if ((Boolean) btn.getTag()) {
                    getPhoto(findViewById(R.id.start_photo));
                } else {
                    getPhoto(findViewById(R.id.start_gallery));
                }
                break;
            case R.id.dlg_clipboard:
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(getString(R.string.scan2label),
                        ((TextView) mDialog.getView().findViewById(R.id.dlg_message)).getText());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show();
                break;
            case R.id.dlg_share:
                final Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT,
                        ((TextView) mDialog.getView().findViewById(R.id.dlg_message)).getText());
                sendIntent.setType(MIME_TYPE);
                startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.scan2label)));
                break;
        }
        mDialog.dismiss();
    }

    /**
     * Dispatch incoming result to the correct fragment.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "on activity result? " + requestCode + " result " + resultCode + " data? " + (data != null));
        //Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
        if (resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                mPhotoUri = data.getData();
            }
            try {
                firebaseInBackground(requestCode);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Exception reading text from photo: " + e.getMessage());
            }
        } else {
            Toast.makeText(this, R.string.returnError, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * This dialog will show the scanned text and allow the user to proceed or retry the photo
     * @param displayText
     * @param mIsPhoto
     */
    void showConfirmDialog(String displayText, boolean mIsPhoto) {
        mDialog = ConfirmTextDialog.newInstance(displayText, mIsPhoto);
        mDialog.show(getSupportFragmentManager(), "show");
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_REQUEST:
                if (permissionGranted(requestCode, CAMERA_REQUEST, grantResults)) {
                    getPhoto(findViewById(R.id.start_photo));
                } else {
                    Toast.makeText(this, R.string.permissionHelp, Toast.LENGTH_LONG).show();
                }
                break;
            case GALLERY_REQUEST:
                if (permissionGranted(requestCode, GALLERY_REQUEST, grantResults)) {
                    getPhoto(findViewById(R.id.start_gallery));
                } else {
                    Toast.makeText(this, R.string.permissionHelp, Toast.LENGTH_LONG).show();
                }
                break;
        }

    }

    /**
     * Called by onActivityResult with the requestCode to create the TextDetector
     * It will put the scanned text output into a single string for the confirm text dialog
     * It builds the mContactFields array of Strings to pass to the next activity
     * @param mCode - Camera or Gallery Request
     * @throws IOException
     */
    void firebaseInBackground(final int mCode) throws IOException {
        final Snackbar mLoadingBar = Snackbar.make(StartActivity.this.findViewById(R.id.snack_anchor),
                R.string.load,
                Snackbar.LENGTH_INDEFINITE);
        Snackbar.SnackbarLayout snack_view = (Snackbar.SnackbarLayout) mLoadingBar.getView();
        TextView tv = (TextView) snack_view.findViewById(android.support.design.R.id.snackbar_text);
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        final ProgressBar indicator = new ProgressBar(StartActivity.this);
        indicator.getIndeterminateDrawable().setColorFilter(
                new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY));
        indicator.setScaleY(0.5f);
        indicator.setScaleX(0.5f);
        snack_view.addView(indicator, 1);
        mLoadingBar.show();

        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), mPhotoUri);
        bitmap = rotateImageIfRequired(bitmap, StartActivity.this,
                checkUri(StartActivity.this, mPhotoUri));
        final FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        Task<FirebaseVisionText> result =
                FirebaseVision.getInstance().getVisionTextDetector().detectInImage(image)
                        .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                // Task completed successfully
                                mLoadingBar.dismiss();
                                final StringBuilder bull = new StringBuilder();
                                ArrayList<String> textArray = new ArrayList();
                                for (FirebaseVisionText.Block block : firebaseVisionText.getBlocks()) {
                                    String text = block.getText();
                                    Log.d(TAG, "block text: " + text);
                                    if (!TextUtils.isEmpty(text)) {
                                        bull.append(text).append("\n");
                                        textArray.add(text);
                                    }
                                }
                                Snackbar.make(StartActivity.this.findViewById(R.id.snack_anchor), R.string.ocr_success, Snackbar.LENGTH_SHORT).show();
                                if (!TextUtils.isEmpty(bull.toString())) {
                                    mContactFields = new String[textArray.size()];
                                    mContactFields = textArray.toArray(mContactFields);
                                    showConfirmDialog(bull.toString(), mCode == R.string.ocr_success);
                                }
                                bull.setLength(0);
                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        Log.w(TAG, "empty result");
                                        Snackbar.make(StartActivity.this.findViewById(R.id.snack_anchor), R.string.no_text, Snackbar.LENGTH_SHORT).show();
                                    }
                                });
    }

}
