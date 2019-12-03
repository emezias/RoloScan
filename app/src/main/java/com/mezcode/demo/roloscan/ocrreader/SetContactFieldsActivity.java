package com.mezcode.demo.roloscan.ocrreader;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by emezias on 4/20/17.
 * This class takes the scanned text as an extra
 * It displays each line of text in its own edit text
 * The ContactSpinnerAdapter getIndices code applies logic to set the correct spinner value
 */

public class SetContactFieldsActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    public static final String TAG = SetContactFieldsActivity.class.getSimpleName();
    final int[] editFields = new int[] { R.id.cc_edit1, R.id.cc_edit2, R.id.cc_edit3, R.id.cc_edit4, R.id.cc_edit5,
            R.id.cc_edit6, R.id.cc_edit7, R.id.cc_edit8, R.id.cc_edit9, R.id.cc_edit10 };
    final int[] spinners = new int[] { R.id.cc_spinner1, R.id.cc_spinner2, R.id.cc_spinner3, R.id.cc_spinner4, R.id.cc_spinner5,
            R.id.cc_spinner6, R.id.cc_spinner7, R.id.cc_spinner8, R.id.cc_spinner9, R.id.cc_spinner10 };
    final int[] btn_fields = new int[] { R.id.cc_btn1, R.id.cc_btn2, R.id.cc_btn3, R.id.cc_btn4, R.id.cc_btn5,
            R.id.cc_btn6, R.id.cc_btn7, R.id.cc_btn8, R.id.cc_btn9, R.id.cc_btn10 };
    String[] mContactFields;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_contact);
    }

    /**
     * The field setup is done in onResume to always show the data, even if the user presses back
     */
    @Override
    protected void onResume() {
        super.onResume();
        //These extras are passed into the activity
        mContactFields = getIntent().getExtras().getStringArray(TAG); //scanned text
        final int[] spinDexes = ContactSpinnerAdapter.getIndices(mContactFields);
        //spinner index integers to set different labels for all rows

        ContactSpinnerAdapter adapter;
        Spinner spinner;
        EditText text;
        ImageButton btn;
        String s;
        for (int i = 0; i < mContactFields.length; i++) {
            s = mContactFields[i];
            adapter = new ContactSpinnerAdapter(this);
            text = (EditText) findViewById(editFields[i]);
            btn = (ImageButton) findViewById(btn_fields[i]);
            text.addTextChangedListener(new EditButton(btn));
            spinner = (Spinner)findViewById(spinners[i]);
            spinner.setAdapter(adapter);
            if (!TextUtils.isEmpty(s)) {
                text.setText(s);
                if (i < spinDexes.length && spinDexes[i] > 0) {
                    spinner.setSelection(spinDexes[i]);
                } else {
                    spinner.setSelection(i);
                }
            } else {
                spinner.setSelection(i);
                btn.setEnabled(false);
            }
            spinner.setTag(i);
            btn.setTag(i);
            //important to set listener
            spinner.setOnItemSelectedListener(this);
        } //end for loop

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.save_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        saveContact(null);
        return true;
    }

    /**
     * This method is set in the view xml and called by the action view on the app bar
     * It alerts the user if more than one text value is mapped to the same contact column
     * If not, it calls create contact to put together the Contacts content provider intent and extras
     * @param v
     */
    public void saveContact(View v) {
        Log.i(TAG, "open Contacts intent");
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
                }
            }
        }
        if (duplicates.isEmpty()) {
            createContact(contactMap);
        } else showDuplicatesDialog(duplicates, contactMap);
    }

    //This demo code comes from the ContactsContract documentation
    /* https://developer.android.com/reference/android/provider/ContactsContract.Intents.Insert.html
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

    /******** Click listeners *********/

    public void clearText(View btn) {
        btn.setEnabled(false);
        ((EditText)findViewById(editFields[(Integer) btn.getTag()])).getText().clear();
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

    void showDuplicatesDialog(ArrayList<Integer> duplicates, final HashMap<Integer, String> contactMap) {
        StringBuilder message = new StringBuilder("Dropping duplicated values:\n");
        for (int id: duplicates) {
            message.append(((EditText) findViewById(id)).getText().toString()).append("\n");
        }
        (new AlertDialog.Builder(SetContactFieldsActivity.this))
                .setMessage(message.toString())
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        createContact(contactMap);
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
