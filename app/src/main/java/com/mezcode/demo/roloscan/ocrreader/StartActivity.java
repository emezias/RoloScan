package com.mezcode.demo.roloscan.ocrreader;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
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
 * got help from a Tensor Flow sample
 * https://github.com/GoogleCloudPlatform/cloud-vision/blob/master/android/CloudVision/app/src/main/java/com/google/sample/cloudvision/MainActivity.java
 */

public class StartActivity extends AppCompatActivity {

    public static final String TAG = StartActivity.class.getSimpleName();
    private static final int GALLERY_REQUEST = 3;
    public static final int CAMERA_REQUEST = 9;
    public static final String FILE_NAME = "RoloScan.jpg";
    public static final String MIME_TYPE = "text/plain";
    Uri mPhotoUri;
    String[] mContactFields;
    ConfirmTextDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
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
            if (requestCode == GALLERY_REQUEST && data.getData() != null) {
                mPhotoUri = data.getData();
            }
            new ReadPhotoTask(requestCode).execute();
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
            return grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    /**
     * This class is to create the TextRecognizer, called by onActivityResult and passing the requestCode
     * It will put the scanned text output into a single string for the confirm text dialog
     * It builds the mContactFields array of Strings to pass to the next activity
     */
    private class ReadPhotoTask extends AsyncTask<Void, Void, String> {
        int mCode;
        Snackbar mLoadingBar;

        public ReadPhotoTask(int code) {
            mLoadingBar = Snackbar.make(StartActivity.this.findViewById(R.id.snack_anchor),
                    R.string.load,
                    Snackbar.LENGTH_INDEFINITE);
            mLoadingBar.show();
            mCode = code;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                final Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), mPhotoUri);
                final Frame frame = (new Frame.Builder()).setBitmap(bitmap).build();
                final TextRecognizer detector = new TextRecognizer.Builder(StartActivity.this).build();
                final SparseArray<TextBlock> blocks = detector.detect(frame);

                detector.release();
                final int sz = getResources().getTextArray(R.array.labels).length;
                mContactFields = new String[sz];
                TextBlock blk;
                final StringBuilder bull = new StringBuilder();
                if (blocks.size() > 0) {
                    int contactDex = 0;
                    for (int dex = 0; dex < blocks.size(); dex++) {
                        blk = blocks.valueAt(dex);
                        bull.append(blk.getValue()).append("\n");
                        for (Text line: blk.getComponents()) {
                            if (contactDex < sz) mContactFields[contactDex++] = line.getValue();
                            else mContactFields[sz-1] = mContactFields[sz-1] + "\n" + line.getValue();
                            //bull.append(line.getValue() + "\n");
                        }
                    } //end for loop
                    //boolean will determine if the app returns to the gallery or camera on retry

                    //Log.d(TAG, "any text? " + bull.toString());
                    return bull.toString();
                } else {
                    Log.w(TAG, "empty result");
                    mCode = R.string.no_text;
                }
            } catch (IOException e) {
                e.printStackTrace();
                mCode = R.string.returnError;
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (isCancelled()) return;
            mLoadingBar.dismiss();
            if (TextUtils.isEmpty(s)) {
                Snackbar.make(StartActivity.this.findViewById(R.id.snack_anchor), mCode, Snackbar.LENGTH_SHORT).show();
            } else {
                showConfirmDialog(s, mCode == CAMERA_REQUEST);
            }
        }
    }
    
}
