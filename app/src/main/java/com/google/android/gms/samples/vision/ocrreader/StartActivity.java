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
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
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

    public void getPhoto(View v) {
        if (requestPermission(
                this,
                CAMERA_PERMISSIONS_REQUEST,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA)) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            mPhotoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider",
                    new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), FILE_NAME));
            Log.d(TAG, "path? " + mPhotoUri.getPath());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, GET_PHOTO);
        }
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
        if (requestCode == GET_PHOTO && resultCode == RESULT_OK) {
            Snackbar.make(getWindow().getDecorView(), "Reading photo", Snackbar.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No photo", Toast.LENGTH_LONG).show();
        }
        final Bitmap bitmap;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), mPhotoUri);
            final Frame frame = (new Frame.Builder()).setBitmap(bitmap).build();
            final TextRecognizer detector = new TextRecognizer.Builder(this).build();
            final SparseArray<TextBlock> blocks = detector.detect(frame);

            detector.release();
            final int sz = getResources().getTextArray(R.array.labels).length;
            mContactFields = new String[sz];
            TextBlock blk;
            TextView tv = (TextView) findViewById(R.id.start_message);
            StringBuilder bull = new StringBuilder();
            if (blocks.size() > 0) {
                int contactDex = 0;
                for (int dex = 0; dex < blocks.size(); dex++) {
                    blk = blocks.valueAt(dex);
                    for (Text line: blk.getComponents()) {
                        Log.d(TAG, "line value? " + line.getValue());
                        if (contactDex < sz) mContactFields[contactDex++] = line.getValue();
                        else mContactFields[sz-1] = mContactFields[sz-1] + "\n" + line.getValue();
                        bull.append(line.getValue() + "\n");
                    }
                } //end for loop
                //tv.setText(bull.toString());
                showConfirmDialog(bull.toString(), mContactFields);
                Log.d(TAG, "any text? " + bull.toString());
                bull.setLength(0);
                bull.trimToSize();
            } else {
                Log.w(TAG, "empty result");
                Toast.makeText(this, "No text was found", Toast.LENGTH_LONG).show();
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
