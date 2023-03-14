package com.mezcode.demo.roloscan.ocrreader

import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * Created by emezias on 4/20/17.
 * This class takes the scanned text as an extra
 * It displays each line of text in its own edit text
 * The ContactSpinnerAdapter getIndices code applies logic to set the correct spinner value
 */
class SetContactFieldsActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private val editFields = intArrayOf(
        R.id.cc_edit1, R.id.cc_edit2, R.id.cc_edit3, R.id.cc_edit4, R.id.cc_edit5,
        R.id.cc_edit6, R.id.cc_edit7, R.id.cc_edit8, R.id.cc_edit9, R.id.cc_edit10
    )
    private val spinners = intArrayOf(
        R.id.cc_spinner1, R.id.cc_spinner2, R.id.cc_spinner3, R.id.cc_spinner4, R.id.cc_spinner5,
        R.id.cc_spinner6, R.id.cc_spinner7, R.id.cc_spinner8, R.id.cc_spinner9, R.id.cc_spinner10
    )
    private val btn_fields = intArrayOf(
        R.id.cc_btn1, R.id.cc_btn2, R.id.cc_btn3, R.id.cc_btn4, R.id.cc_btn5,
        R.id.cc_btn6, R.id.cc_btn7, R.id.cc_btn8, R.id.cc_btn9, R.id.cc_btn10
    )
    private lateinit var mContactFields: Array<String>
    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_contact)
    }

    /**
     * The field setup is done in onResume to always show the data, even if the user presses back
     */
    protected override fun onResume() {
        super.onResume()
        //These extras are passed into the activity
        val bundle = intent.extras
        if (bundle == null) {
            // easy error handling
            Toast.makeText(
                this,
                getString(R.string.no_text),
                Toast.LENGTH_SHORT
            ).show()
            return
        } else {
            //scanned text
            mContactFields = bundle.getStringArray(TAG) as Array<String>
        }
        val spinDexes: IntArray = ContactSpinnerAdapter.Companion.getIndices(mContactFields)
        //spinner index integers to set different labels for all rows
        var adapter: ContactSpinnerAdapter
        var spinner: Spinner
        var text: EditText
        var btn: ImageButton
        var s: String
        for (i in mContactFields.indices) {
            s = mContactFields[i]
            adapter = ContactSpinnerAdapter(this)
            text = findViewById<EditText>(editFields[i])
            btn = findViewById<ImageButton>(btn_fields[i])
            text.addTextChangedListener(EditButton(btn))
            spinner = findViewById<Spinner>(spinners[i])
            spinner.setAdapter(adapter)
            if (!TextUtils.isEmpty(s)) {
                text.setText(s)
                if (i < spinDexes.size && spinDexes[i] > 0) {
                    spinner.setSelection(spinDexes[i])
                } else {
                    spinner.setSelection(i)
                }
            } else {
                spinner.setSelection(i)
                btn.setEnabled(false)
            }
            spinner.setTag(i)
            btn.setTag(i)
            //important to set listener
            spinner.onItemSelectedListener = this
        } //end for loop
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.save_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        saveContact(null)
        return true
    }

    /**
     * This method is set in the view xml and called by the action view on the app bar
     * It alerts the user if more than one text value is mapped to the same contact column
     * If not, it calls create contact to put together the Contacts content provider intent and extras
     * @param v
     */
    fun saveContact(v: View?) {
        Log.i(TAG, "open Contacts intent")
        val duplicates = ArrayList<Int>()
        val contactMap = HashMap<Int, String>()
        var value: String
        var spinnerDex: Int
        for (dex in editFields.indices) {
            value = (findViewById<EditText>(editFields[dex])).text.toString()
            if (!TextUtils.isEmpty(value)) {
                spinnerDex = (findViewById<Spinner>(spinners[dex])).selectedItemPosition
                if (contactMap[spinnerDex] != null) {
                    duplicates.add(editFields[dex])
                } else {
                    contactMap[spinnerDex] = value
                }
            }
        }
        if (duplicates.isEmpty()) {
            createContact(contactMap)
        } else showDuplicatesDialog(duplicates, contactMap)
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

    / ******** Click listeners *********/
    fun clearText(btn: View) {
        btn.isEnabled = false
        (findViewById<EditText>(editFields[(btn.tag as Int)])).text.clear()
    }

    private fun createContact(contactMap: HashMap<Int, String>) {
        // Creates a new Intent to insert a contact
        val data: ArrayList<ContentValues> = ArrayList<ContentValues>()
        var contactdata: ContentValues
        Log.d(TAG, "data size? " + contactMap.size)
        var company: String? = null
        val intent = Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI)
        for (key in contactMap.keys) {
            when (key) {
                ContactSpinnerAdapter.Companion.IND_NAME ->                     //could not find the right combination of content value keys for the name
                    intent.putExtra(ContactsContract.Intents.Insert.NAME, contactMap[key])
                ContactSpinnerAdapter.Companion.IND_PHONE -> {
                    contactdata = ContentValues()
                    contactdata.put(
                        ContactsContract.Contacts.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                    )
                    contactdata.put(
                        ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_WORK
                    )
                    contactdata.put(ContactsContract.CommonDataKinds.Phone.NUMBER, contactMap[key])
                    data.add(contactdata)
                }
                ContactSpinnerAdapter.Companion.IND_EMAIL -> {
                    contactdata = ContentValues()
                    contactdata.put(
                        ContactsContract.Contacts.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                    )
                    contactdata.put(
                        ContactsContract.CommonDataKinds.Email.TYPE,
                        ContactsContract.CommonDataKinds.Email.TYPE_WORK
                    )
                    contactdata.put(ContactsContract.CommonDataKinds.Email.ADDRESS, contactMap[key])
                }
                ContactSpinnerAdapter.Companion.IND_ADDR -> {
                    contactdata = ContentValues()
                    contactdata.put(
                        ContactsContract.Contacts.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE
                    )
                    contactdata.put(
                        ContactsContract.CommonDataKinds.StructuredPostal.TYPE,
                        ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK
                    )
                    contactdata.put(
                        ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS,
                        contactMap[key]
                    )
                    data.add(contactdata)
                }
                ContactSpinnerAdapter.Companion.IND_COMPANY, ContactSpinnerAdapter.Companion.IND_TITLE -> {
                    if (company != null) break
                    contactdata = ContentValues()
                    company = contactMap[ContactSpinnerAdapter.Companion.IND_COMPANY]
                    contactdata.put(
                        ContactsContract.Contacts.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE
                    )
                    if (!TextUtils.isEmpty(company)) {
                        contactdata.put(
                            ContactsContract.CommonDataKinds.Organization.COMPANY,
                            company
                        )
                        Log.d(TAG, "adding company $company")
                    }
                    company = contactMap[ContactSpinnerAdapter.Companion.IND_TITLE]
                    if (!TextUtils.isEmpty(company)) {
                        contactdata.put(
                            ContactsContract.CommonDataKinds.Organization.TITLE,
                            company
                        )
                        Log.d(TAG, "adding title $company")
                    }
                    data.add(contactdata)
                }
                ContactSpinnerAdapter.Companion.IND_IM -> {
                    contactdata = ContentValues()
                    contactdata.put(
                        ContactsContract.Contacts.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE
                    )
                    contactdata.put(
                        ContactsContract.CommonDataKinds.Im.TYPE,
                        ContactsContract.CommonDataKinds.Im.TYPE_CUSTOM
                    )
                    contactdata.put(ContactsContract.CommonDataKinds.Im.LABEL, "Chat")
                    contactdata.put(ContactsContract.CommonDataKinds.Im.DATA, contactMap[key])
                    data.add(contactdata)
                }
                ContactSpinnerAdapter.Companion.IND_NOTES -> {
                    contactdata = ContentValues()
                    contactdata.put(
                        ContactsContract.Contacts.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE
                    )
                    contactdata.put(ContactsContract.CommonDataKinds.Note.NOTE, contactMap[key])
                    data.add(contactdata)
                }
                ContactSpinnerAdapter.Companion.IND_PHONE2 -> {
                    contactdata = ContentValues()
                    contactdata.put(
                        ContactsContract.Contacts.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                    )
                    contactdata.put(
                        ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_OTHER
                    )
                    contactdata.put(ContactsContract.CommonDataKinds.Phone.NUMBER, contactMap[key])
                    data.add(contactdata)
                }
                ContactSpinnerAdapter.Companion.IND_EMAIL2 -> {
                    contactdata = ContentValues()
                    contactdata.put(
                        ContactsContract.Contacts.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                    )
                    contactdata.put(
                        ContactsContract.CommonDataKinds.Email.TYPE,
                        ContactsContract.CommonDataKinds.Email.TYPE_OTHER
                    )
                    contactdata.put(ContactsContract.CommonDataKinds.Email.ADDRESS, contactMap[key])
                    data.add(contactdata)
                }
            }
        }
        //each content value fills a contacts contract row, the name is set as an extra
        intent.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, data)
        startActivity(intent)
        Toast.makeText(
            this,
            "Creating new contact: " + intent.getStringExtra(ContactsContract.Intents.Insert.NAME),
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showDuplicatesDialog(duplicates: ArrayList<Int>, contactMap: HashMap<Int, String>) {
        val message = StringBuilder("Dropping duplicated values:\n")
        for (id in duplicates) {
            message.append((findViewById(id) as EditText).getText().toString()).append("\n")
        }
        AlertDialog.Builder(this@SetContactFieldsActivity)
            .setMessage(message.toString())
            .setPositiveButton(R.string.confirm) { dialog, which -> createContact(contactMap) }
            .setNegativeButton(R.string.retry) { dialog, which -> dialog.dismiss() }.create().show()
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val previous = parent.tag as Int
        if (position == previous) return
        parent.tag = position
        //Move map to Contact Fields logic check to save button, nothing to do here!
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    companion object {
        val TAG = SetContactFieldsActivity::class.java.simpleName
    }
}