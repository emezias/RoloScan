package com.google.android.gms.samples.vision.ocrreader;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
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
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by emezias on 4/20/17.
 */

public class StartActivity extends AppCompatActivity {

    public static final String TAG = StartActivity.class.getSimpleName();
    public static final int GET_PHOTO = 111;
    public static final int CAMERA_PERMISSIONS_REQUEST = 2;
    public static final String FILE_NAME = "temp.jpg";
    Uri mPhotoUri;
    String[] mContactFields;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
    }

    /*public void getPhoto(View v) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "JPEG_" + System.currentTimeMillis() + "_";
            File image = new File(getFilesDir(), imageFileName + ".jpg");
            if (image != null) {
                Log.d(TAG, "image file ok");
                mPhotoUri = Uri.fromFile(image);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoUri);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivityForResult(takePictureIntent, GET_PHOTO);
            } else {
                Toast.makeText(this, "Problem creating a file to save the photo", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Problem, a Camera App is not found", Toast.LENGTH_LONG).show();
        }
    }*/

    public void getPhoto(View v) {
        if (requestPermission(
                this,
                CAMERA_PERMISSIONS_REQUEST,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA)) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            mPhotoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            Log.d(TAG, "path? " + mPhotoUri.getPath());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, GET_PHOTO);
        }
    }

    public File getCameraFile() {
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File f = new File(dir, FILE_NAME);
        Log.d(TAG, "file? " + f.exists());
        return f;
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
        final Bitmap bitmap;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), mPhotoUri);
            final Frame frame = (new Frame.Builder()).setBitmap(bitmap).build();
            final TextRecognizer detector = new TextRecognizer.Builder(this).build();
            final SparseArray<TextBlock> blocks = detector.detect(frame);
            Log.d(TAG, "any blocks? " + blocks.size());
            detector.release();
            final int sz = getResources().getTextArray(R.array.labels).length-1;
            TextBlock blk;
            TextView tv = (TextView) findViewById(R.id.start_message);
            StringBuilder bull = new StringBuilder();
            if (blocks.size() > 0) {
                mContactFields = new String[blocks.size()];
                for (int dex = 0; dex < blocks.size(); dex++) {
                    blk = blocks.valueAt(dex);
                    if (dex > sz) mContactFields[sz] = mContactFields[sz] + "\n" + blk.getValue();
                    else mContactFields[dex] = blk.getValue();
                    Log.d(TAG, "block value? " + blk.getValue());
                    bull.append(blk.getValue() + "\n");
                } //end for loop
                //tv.setText(bull.toString());
                showConfirmDialog(bull.toString(), mContactFields);
                Log.d(TAG, "any text? " + bull.toString());
                bull.setLength(0);
                bull.trimToSize();
            } else {
                Log.w(TAG, "empty result");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void showConfirmDialog(String displayText, String[] contactText) {
        (new AlertDialog.Builder(StartActivity.this))
                .setMessage(displayText)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //startGalleryChooser();
                        final Intent tnt = new Intent(getApplicationContext(), ConfirmContactActivity.class);
                        tnt.putExtra(ConfirmContactActivity.TAG, mContactFields);
                        startActivity(tnt);
                    }
                })
                .setNegativeButton(R.string.retry, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getPhoto(null);
                    }
                }).create().show();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissionGranted(requestCode, CAMERA_PERMISSIONS_REQUEST, grantResults)) {
            getPhoto(null);
        } else {
            Toast.makeText(this, "Permissions are needed to use this app", Toast.LENGTH_LONG).show();
        }
    }

    //Permissions logic borrowed from cloud vision
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
