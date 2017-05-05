package com.mezcode.demo.roloscan.ocrreader;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by emezias on 4/20/17.
 * got help from a Tensor Flow sample
 * https://github.com/GoogleCloudPlatform/cloud-vision/blob/master/android/CloudVision/app/src/main/java/com/google/sample/cloudvision/MainActivity.java
 */

public class StartActivity extends AppCompatActivity {

    public static final String TAG = StartActivity.class.getSimpleName();
    private static final int GALLERY_REQUEST = 3;
    public static final int CAMERA_REQUEST = 9;
    public static final String FILE_NAME = "snapContact.jpg";
    Uri mPhotoUri;
    String[] mContactFields;
    ConfirmTextDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        getSupportActionBar().setTitle(R.string.label);
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

        switch (btn.getId()) {
            case R.id.dlg_confirm:
                //TODO
                /*final Intent tnt = new Intent(getApplicationContext(), SetContactFieldsActivity.class);
                tnt.putExtra(SetContactFieldsActivity.TAG, mContactFields);
                startActivity(tnt);*/
                break;
            case R.id.dlg_retry:
                if ((Boolean) btn.getTag()) {
                    getPhoto(findViewById(R.id.start_photo));
                } else {
                    getPhoto(findViewById(R.id.start_gallery));
                }
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
            if (requestCode == GALLERY_REQUEST && data.getData() != null) {
                mPhotoUri = data.getData();
            }
            readPhoto(requestCode);
            return;
        } //ok result can fall through to an error
        Toast.makeText(this, R.string.returnError, Toast.LENGTH_LONG).show();
    }

    /**
     * This method is going to create the TextRecognizer
     * It will put the scanned text output into a single string for the dialog
     * It will have build the mContactFields array of Strings to pass to the next activity
     * @param requestCode
     */
    void readPhoto(int requestCode) {
        try {
            final Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), mPhotoUri);
            //final Frame frame = TODO, create TextRecognizer
            //TODO show confirm dialog with each line of text
        } catch (IOException e) {
            e.printStackTrace();
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
        if (requestCode == permissionCode) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

}
