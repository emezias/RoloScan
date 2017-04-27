package com.google.android.gms.samples.vision.ocrreader;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

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
        final int[] spinDexes = ContactSpinnerAdapter.getIndices(mContactFields);
        int dex = 0;
        ContactSpinnerAdapter adapter;
        Spinner spinner;
        EditText text;
        for (String s: mContactFields) {
            adapter = new ContactSpinnerAdapter(this);
            text = (EditText) findViewById(editFields[dex]);
            spinner = (Spinner)findViewById(spinners[dex]);
            spinner.setAdapter(adapter);
            Log.d(TAG, "text to set: " + s);
            if (!TextUtils.isEmpty(s)) {
                text.setText(s);
            }
            spinner.setSelection(spinDexes[dex]);
            Log.d(TAG, "selected? " + spinner.getSelectedItemPosition());
            spinner.setTag(dex++);
            //important to set listener after
            spinner.setOnItemSelectedListener(this);
        } //end for loop
    }

    public void saveContact(View v) {
        Log.d(TAG, "To Do");
        createContactTest();

        /*ArrayList<Integer> duplicates = new ArrayList<>();
        HashMap<String, String> contactMap = new HashMap<>();
        String value, key;
        String[] contactKeys = getResources().getStringArray(R.array.contact_keys);
        int spinnerDex;
        for(int dex = 0; dex < editFields.length; dex++) {
            value = ((EditText) findViewById(editFields[dex])).getText().toString();
            if (!TextUtils.isEmpty(value)) {
                spinnerDex = ((Spinner) findViewById(spinners[dex])).getSelectedItemPosition();
                if (contactMap.containsKey(contactKeys[spinnerDex])) {
                    duplicates.add(editFields[dex]);
                } else {
                    contactMap.put(contactKeys[spinnerDex], value);
                }
            }
        }
        if (duplicates.isEmpty()) {
            createContact(contactMap);
        } else showDuplicatesDialog(duplicates, contactMap);*/
    }

    void createContactTest() {
        ArrayList<ContentValues> data = new ArrayList<ContentValues>();

        ContentValues row1 = new ContentValues();
        row1.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE);
        row1.put(ContactsContract.CommonDataKinds.Organization.COMPANY, "Android");
        data.add(row1);

        ContentValues row2 = new ContentValues();
        row2.put(Contacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
        row2.put(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM);
        row2.put(ContactsContract.CommonDataKinds.Email.LABEL, "Green Bot");
        row2.put(ContactsContract.CommonDataKinds.Email.ADDRESS, "android@android.com");
        data.add(row2);

        Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
        intent.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, data);

        startActivity(intent);
    }

    private void createContact(HashMap<String, String> contactMap) {
        // Creates a new Intent to insert a contact
        ArrayList<ContentValues> data = new ArrayList<>();
        ContentValues contactdata;
        for (String key: contactMap.keySet()) {
            Log.d(TAG, key + " and text to set: " + contactMap.get(key));
            switch(key) {
                case ContactsContract.Intents.Insert.NAME:
                    contactdata = new ContentValues();
                    contactdata.put(ContactsContract.Contacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
                    contactdata.put(ContactsContract.CommonDataKinds.StructuredName.FULL_NAME_STYLE, contactMap.get(key));
                    data.add(contactdata);
                    break;
                case ContactsContract.Intents.Insert.PHONE:
                    contactdata = new ContentValues();
                    contactdata.put(ContactsContract.Contacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
                    contactdata.put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_HOME);
                    contactdata.put(ContactsContract.CommonDataKinds.Phone.NUMBER, contactMap.get(key));
                    data.add(contactdata);
                    break;
                case ContactsContract.Intents.Insert.EMAIL:
                    contactdata = new ContentValues();
                    contactdata.put(ContactsContract.Contacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
                    contactdata.put(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK);
                    contactdata.put(ContactsContract.CommonDataKinds.Email.ADDRESS, contactMap.get(key));
                    data.add(contactdata);
                    break;
                case ContactsContract.Intents.Insert.COMPANY:
                    contactdata = new ContentValues();
                    contactdata.put(ContactsContract.Contacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE);
                    contactdata.put(ContactsContract.CommonDataKinds.Organization.COMPANY, contactMap.get(key));
                    data.add(contactdata);
                    break;
                case ContactsContract.Intents.Insert.JOB_TITLE:
                    contactdata = new ContentValues();
                    contactdata.put(ContactsContract.Contacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE);
                    contactdata.put(ContactsContract.CommonDataKinds.Organization.TITLE, contactMap.get(key));
                    data.add(contactdata);
                    break;
                case ContactsContract.Intents.Insert.POSTAL:
                    contactdata = new ContentValues();
                    contactdata.put(ContactsContract.Contacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE);
                    contactdata.put(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, contactMap.get(key));
                    data.add(contactdata);
                    break;
                case ContactsContract.Intents.Insert.IM_HANDLE:
                    contactdata = new ContentValues();
                    contactdata.put(ContactsContract.Contacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE);
                    contactdata.put(ContactsContract.CommonDataKinds.Im.PROTOCOL, contactMap.get(key));
                    data.add(contactdata);
                    break;
                case ContactsContract.Intents.Insert.NOTES:
                    contactdata = new ContentValues();
                    contactdata.put(ContactsContract.Contacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE);
                    contactdata.put(ContactsContract.CommonDataKinds.Note.NOTE, contactMap.get(key));
                    data.add(contactdata);
                    break;
                case ContactsContract.Intents.Insert.SECONDARY_PHONE:
                    contactdata = new ContentValues();
                    contactdata.put(ContactsContract.Contacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
                    contactdata.put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_OTHER);
                    contactdata.put(ContactsContract.CommonDataKinds.Phone.NUMBER, contactMap.get(key));
                    data.add(contactdata);
                    break;
                case ContactsContract.Intents.Insert.SECONDARY_EMAIL:
                    contactdata = new ContentValues();
                    contactdata.put(ContactsContract.Contacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
                    contactdata.put(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_OTHER);
                    contactdata.put(ContactsContract.CommonDataKinds.Email.ADDRESS, contactMap.get(key));
                    data.add(contactdata);
                    break;
            }
        }

        //Intent intent = new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI);
        /*Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);*/
        // Creates a new Intent to insert a contact
        //Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION);
        final Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION);
        // Sets the MIME type to match the Contacts Provider
        intent.setType(Contacts.CONTENT_ITEM_TYPE);
        intent.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, data);
        startActivity(intent);

        startActivity(intent);
        for (int id: editFields) {
            ((EditText)findViewById(id)).setText("");
        }
        Toast.makeText(this, "Creating new contact: " + contactMap.get(ContactsContract.Intents.Insert.NAME), Toast.LENGTH_LONG).show();
    }

    void showDuplicatesDialog(ArrayList<Integer> duplicates, final HashMap<String, String> contactMap) {
        StringBuilder message = new StringBuilder("Dropping duplicated values:\n");
        for (int id: duplicates) {
            message.append(((EditText) findViewById(id)).getText().toString()).append("\n");
        }
        (new AlertDialog.Builder(ConfirmContactActivity.this))
                .setMessage(message.toString())
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //startGalleryChooser();
                    }
                })
                .setNegativeButton(R.string.retry, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create().show();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        final int previous = (int) parent.getTag();
        if (position == previous) return;
        parent.setTag(position);
        //Move map to Contact Fields logic check to save button, nothing to do here!
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) { }
}
