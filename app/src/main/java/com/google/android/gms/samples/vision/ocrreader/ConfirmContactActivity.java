package com.google.android.gms.samples.vision.ocrreader;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * Created by emezias on 4/20/17.
 */

public class ConfirmContactActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    public static final String TAG = ConfirmContactActivity.class.getSimpleName();
    String[] mContactFields;
    int[] editFields = new int[] { R.id.cc_edit1, R.id.cc_edit2, R.id.cc_edit3, R.id.cc_edit4, R.id.cc_edit5,
            R.id.cc_edit6, R.id.cc_edit7, R.id.cc_edit8, R.id.cc_edit9, R.id.cc_edit10 };
    int[] spinners = new int[] { R.id.cc_spinner1, R.id.cc_spinner2, R.id.cc_spinner3, R.id.cc_spinner4, R.id.cc_spinner5,
            R.id.cc_spinner6, R.id.cc_spinner7, R.id.cc_spinner8, R.id.cc_spinner9, R.id.cc_spinner10 };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_contact);
    }

    /**
     * Dispatch onResume() to fragments.  Note that for better inter-operation
     * with older versions of the platform, at the point of this call the
     * fragments attached to the activity are <em>not</em> resumed.  This means
     * that in some cases the previous state may still be saved, not allowing
     * fragment transactions that modify the state.  To correctly interact
     * with fragments in their proper state, you should instead override
     * {@link #onResumeFragments()}.
     */
    @Override
    protected void onResume() {
        super.onResume();
        mContactFields = getIntent().getExtras().getStringArray(TAG);
        int dex = 0;
        ContactSpinnerAdapter adapter;
        Spinner spinner;
        EditText text;
        for (String s: mContactFields) {
            adapter = new ContactSpinnerAdapter(this);
            text = (EditText) findViewById(editFields[dex]);
            Log.d(TAG, "text to set: " + s);
            if (!TextUtils.isEmpty(s)) {
                text.setText(s);
                adapter.setDefault(dex);
            }
            spinner = (Spinner)findViewById(spinners[dex]);
            spinner.setAdapter(adapter);
            spinner.setSelection(dex);
            spinner.setTag(dex++);
            //important to set listener after
            spinner.setOnItemSelectedListener(this);
        } //end for loop
    }

    public void saveContact(View v) {
        Log.d(TAG, "To Do");
    }

    void showConfirmDialog(String displayText, String[] contactText) {
        /*(new AlertDialog.Builder(ConfirmContactActivity.this))
                .setMessage(displayText)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //startGalleryChooser();
                    }
                })
                .setNegativeButton(R.string.retry, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getPhoto(null);
                    }
                }).create().show();*/
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        final int previous = (int) parent.getTag();
        if (position == previous) return;
        ContactSpinnerAdapter adapter = (ContactSpinnerAdapter)parent.getAdapter();
        if (adapter.checkUnique(position)) {
            adapter.setNewSelection(position, previous);
            parent.setTag(position);
        } else {
            Toast.makeText(this, "Each label must be unique, mix and match", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) { }
}
