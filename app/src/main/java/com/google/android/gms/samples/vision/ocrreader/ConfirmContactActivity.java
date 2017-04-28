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
            //Log.d(TAG, "text to set: " + s);
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
        //createContactTest();
        ArrayList<Integer> duplicates = new ArrayList<>();
        HashMap<Integer, String> contactMap = new HashMap<>();
        String value;
        int spinnerDex;
        for(int dex = 0; dex < editFields.length; dex++) {
            value = ((EditText) findViewById(editFields[dex])).getText().toString();
            if (!TextUtils.isEmpty(value)) {
                spinnerDex = ((Spinner) findViewById(spinners[dex])).getSelectedItemPosition();
                if (contactMap.get(spinnerDex) != null) {
                    duplicates.add(editFields[dex]);
                } else {
                    contactMap.put(spinnerDex, value);
                    Log.d(TAG, "value " + value);
                    Log.d(TAG, "key " + spinnerDex);
                }
            }
        }
        if (duplicates.isEmpty()) {
            createContact(contactMap);
        } else showDuplicatesDialog(duplicates);
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

    /**
     * Parse a line of text representing a name into the proper contact fields
     * @param fullName one line of text encompassing all Contact Structured name fields
     * @return Given name and Family name fields - as good as it gets
     */
    String[] splitName(String fullName) {
        String[] names = fullName.split(" ");
        switch (names.length) {
            case 1: return new String[] { fullName, ""};
            case 2: return names;
        }
        StringBuilder tmp = new StringBuilder(names[2].length());
        for (int dex = 1; dex < names.length; dex++) {
            tmp.append(names[dex]).append(" ");
        }
        return new String[] { names[0], tmp.toString() };
    }

    private void createContact(HashMap<Integer, String> contactMap) {
        // Creates a new Intent to insert a contact
        ArrayList<ContentValues> data = new ArrayList<>();
        ContentValues contactdata;
        Log.d(TAG, "data size? " + contactMap.size());
        String company = null;
        Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
        for (int key: contactMap.keySet()) {
            switch(key) {
                case ContactSpinnerAdapter.IND_NAME:
                    //could not find the right combination of content value keys for the name
                    intent.putExtra(ContactsContract.Intents.Insert.NAME, contactMap.get(key));
                    break;
                case ContactSpinnerAdapter.IND_PHONE:
                    contactdata = new ContentValues();
                    contactdata.put(ContactsContract.Contacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
                    contactdata.put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_WORK);
                    contactdata.put(ContactsContract.CommonDataKinds.Phone.NUMBER, contactMap.get(key));
                    data.add(contactdata);
                    break;
                case ContactSpinnerAdapter.IND_EMAIL:
                    contactdata = new ContentValues();
                    contactdata.put(ContactsContract.Contacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
                    contactdata.put(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK);
                    contactdata.put(ContactsContract.CommonDataKinds.Email.ADDRESS, contactMap.get(key));
                    //Log.d(TAG, "add? " + data.add(contactdata));
                    break;

                case ContactSpinnerAdapter.IND_ADDR:
                    contactdata = new ContentValues();
                    contactdata.put(ContactsContract.Contacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE);
                    contactdata.put(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK);
                    contactdata.put(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, contactMap.get(key));
                    data.add(contactdata);
                    break;

                case ContactSpinnerAdapter.IND_COMPANY:
                case ContactSpinnerAdapter.IND_TITLE:
                    if (company != null) break;
                    contactdata = new ContentValues();
                    company = contactMap.get(ContactSpinnerAdapter.IND_COMPANY);
                    contactdata.put(ContactsContract.Contacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE);
                    if (!TextUtils.isEmpty(company)) {
                        contactdata.put(ContactsContract.CommonDataKinds.Organization.COMPANY, company);
                        Log.d(TAG, "adding company " + company);
                    }
                    company = contactMap.get(ContactSpinnerAdapter.IND_TITLE);
                    if (!TextUtils.isEmpty(company)) {
                        contactdata.put(ContactsContract.CommonDataKinds.Organization.TITLE, company);
                        Log.d(TAG, "adding title " + company);
                    }
                    data.add(contactdata);
                    break;
                case ContactSpinnerAdapter.IND_IM:
                    contactdata = new ContentValues();
                    contactdata.put(ContactsContract.Contacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE);
                    contactdata.put(ContactsContract.CommonDataKinds.Im.TYPE, ContactsContract.CommonDataKinds.Im.TYPE_CUSTOM);
                    contactdata.put(ContactsContract.CommonDataKinds.Im.LABEL, "Chat");
                    contactdata.put(ContactsContract.CommonDataKinds.Im.DATA, contactMap.get(key));
                    data.add(contactdata);
                    break;
                case ContactSpinnerAdapter.IND_NOTES:
                    contactdata = new ContentValues();
                    contactdata.put(ContactsContract.Contacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE);
                    contactdata.put(ContactsContract.CommonDataKinds.Note.NOTE, contactMap.get(key));
                    data.add(contactdata);
                    break;
                case ContactSpinnerAdapter.IND_PHONE2:
                    contactdata = new ContentValues();
                    contactdata.put(ContactsContract.Contacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
                    contactdata.put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_OTHER);
                    contactdata.put(ContactsContract.CommonDataKinds.Phone.NUMBER, contactMap.get(key));
                    data.add(contactdata);
                    break;
                case ContactSpinnerAdapter.IND_EMAIL2:
                    contactdata = new ContentValues();
                    contactdata.put(ContactsContract.Contacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
                    contactdata.put(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_OTHER);
                    contactdata.put(ContactsContract.CommonDataKinds.Email.ADDRESS, contactMap.get(key));
                    data.add(contactdata);
                    break;
            }
        }
        //each content value fills a contacts contract row, the name is set as an extra
        intent.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, data);
        startActivity(intent);

        Toast.makeText(this, "Creating new contact: " + intent.getStringExtra(ContactsContract.Intents.Insert.NAME), Toast.LENGTH_LONG).show();
    }

    void showDuplicatesDialog(ArrayList<Integer> duplicates) {
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
