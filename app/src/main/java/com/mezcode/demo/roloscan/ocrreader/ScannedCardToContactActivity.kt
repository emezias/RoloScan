package com.mezcode.demo.roloscan.ocrreader

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.mezcode.demo.roloscan.ocrreader.ContactSpinnerAdapter.Companion.IND_NOTES
import com.mezcode.demo.roloscan.ocrreader.Utils.showSnackbar
import kotlinx.android.synthetic.main.activity_create_contact.*
import java.util.*

/**
 * Created by emezias on 4/20/17.
 * This class takes the scanned text as an extra
 * It displays each line of text in its own edit text
 * The ContactSpinnerAdapter getIndices code applies logic to try to set the correct spinner value
 */
class ScannedCardToContactActivity : AppCompatActivity(), OnItemSelectedListener {

    private lateinit var contactFields: MutableList<String>
    private lateinit var spinDexes: Map<String, Int>
    //parallel arrays used to populate the screen with the scanned text
    private lateinit var editFields: Array<EditText>
    private lateinit var btn_fields: Array<ImageButton>
    private lateinit var spinners: Array<Spinner>

    companion object {
        val TAG = ScannedCardToContactActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_contact)
        editFields = arrayOf(cc_edit1, cc_edit2, cc_edit3, cc_edit4, cc_edit5,
                cc_edit6, cc_edit7, cc_edit8, cc_edit9, cc_edit10 )
        btn_fields = arrayOf(cc_btn1, cc_btn2, cc_btn3, cc_btn4, cc_btn5,
                cc_btn6, cc_btn7, cc_btn8, cc_btn9, cc_btn10)
        spinners = arrayOf(cc_spinner1, cc_spinner2, cc_spinner3, cc_spinner4, cc_spinner5,
                cc_spinner6, cc_spinner7, cc_spinner8, cc_spinner9, cc_spinner10)
        //These extras are passed into the activity
        contactFields = (intent.extras?.getStringArray(TAG) as Array<String>).toMutableList() //scanned text
        if (contactFields.isNullOrEmpty()) {
            showSnackbar(cc_spinner1, R.string.no_text)
            finish()
            return
        }
        //map value from the scanned text to the contact field types
        spinDexes = ContactSpinnerAdapter.guessIndices(applicationContext, contactFields.toList())
    }

    /**
     * The field setup is done in onResume to always show the data, even if the user presses back
     */
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "on resume ${contactFields.size} and the map ${spinDexes.size}" )
        //spinner index integers to set different labels for all rows
        //For loop runs through the rows of spinners and EditText to load the recognized text
        setLayoutOfScannedText()

        //finish resume by setting any leftover text into Notes in last edit text
        if (contactFields.isNotEmpty()) {
            val et = editFields.firstOrNull() { it.text.isEmpty()} ?: cc_edit10
            et.setText(contactFields.joinToString("\n"))
            et.setSelection(IND_NOTES)
        } //this happens if there are more strings than fields or strings that don't map
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.save_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        saveContact(null)
        return true
    }

    fun setLayoutOfScannedText() {
        //For loop runs through the 9 rows of spinners and EditText to load the recognized text
        for ((i, editText) in editFields.withIndex()) {
            //each spinner needs its own
            btn_fields[i].tag = i
            editText.addTextChangedListener(EditButton(btn_fields[i]))
            spinners[i].adapter = ContactSpinnerAdapter(this)
            //All fields on the row are variables. Inflating the layout to match the scanned text could be better
            spinners[i].onItemSelectedListener = this
            with(spinDexes.keys.elementAtOrElse(i, {""})) {
                //spinDexes map returned from the adapter contains every string from the recognizer as a key
                if (this.isEmpty()) btn_fields[i].isEnabled = false
                else {
                    editText.setText(this)
                    contactFields.subtract(listOf(this))
                    //tracking what's in place, filling notes with whatever is left
                    setSpinner(i, this)
                }
            }
        } //end for loop, text fields that matched contact types are set
    }

    private fun setSpinner(dex: Int, mappedValue: String) {
        if(spinDexes.getOrElse(mappedValue, { -1 }) >= 0)
            spinners[dex].setSelection(spinDexes.getValue(mappedValue))
    }

    /**
     * This method is set in the view xml and called by the action view on the app bar
     * It alerts the user if more than one text value is mapped to the same contact column
     * If not, it calls create contact to put together the Contacts content provider intent and extras
     * @param v
     */
    fun saveContact(v: View?) {
        Log.i(TAG, "open Contacts intent")
        val duplicates = ArrayList<EditText>()
        val contactMap = HashMap<Int, String>()
        var spinnerDex: Int
        for ((dex, editText) in editFields.withIndex()) {
            if (!TextUtils.isEmpty(editText.text.toString())) {
                spinnerDex = spinners[dex].selectedItemPosition
                if (contactMap[spinnerDex] != null) {
                    duplicates.add(editText)
                } else {
                    contactMap[spinnerDex] = editText.text.toString()
                }
            }
        }
        if (duplicates.isEmpty()) {
            createContact(contactMap)
        } else showDuplicatesDialog(duplicates, contactMap)
    }

    //This logic is guided by the ContactsProvider documentation on the Android dev site
    //https://developer.android.com/training/contacts-provider/modify-data


    /******** Click listeners *********/
    fun clearText(clearTextButton: View) {
        clearTextButton.isEnabled = false
        editFields[clearTextButton.tag as Int].text.clear()
    }

    private fun createContact(contactMap: HashMap<Int, String>) {
        // Creates a new Intent to insert a contact
        if (contactMap[ContactSpinnerAdapter.IND_NAME].isNullOrEmpty()) {
            showSnackbar(cc_spinner1, R.string.contact_error)
            return
        }
        Log.d(TAG, "data size? " + contactMap.size)
        val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
            // Sets the MIME type to match the Contacts Provider
            type = ContactsContract.RawContacts.CONTENT_TYPE
        }
        for (key in contactMap.keys) {
            when (key) {
                ContactSpinnerAdapter.IND_NAME ->
                    intent.putExtra(ContactsContract.Intents.Insert.NAME, contactMap[key])
                ContactSpinnerAdapter.IND_PHONE ->
                    intent.putExtra(ContactsContract.Intents.Insert.PHONE, contactMap[key])
                ContactSpinnerAdapter.IND_EMAIL ->
                    intent.putExtra(ContactsContract.Intents.Insert.EMAIL, contactMap[key])
                ContactSpinnerAdapter.IND_ADDR ->
                    intent.putExtra(ContactsContract.Intents.Insert.POSTAL, contactMap[key])
                ContactSpinnerAdapter.IND_COMPANY ->
                    intent.putExtra(ContactsContract.Intents.Insert.COMPANY, contactMap[key])
                ContactSpinnerAdapter.IND_TITLE ->
                    intent.putExtra(ContactsContract.Intents.Insert.JOB_TITLE, contactMap[key])
                ContactSpinnerAdapter.IND_IM ->
                    intent.putExtra(ContactsContract.Intents.Insert.IM_HANDLE, contactMap[key])
                ContactSpinnerAdapter.IND_NOTES ->
                    intent.putExtra(ContactsContract.Intents.Insert.NOTES, contactMap[key])
                ContactSpinnerAdapter.IND_PHONE2 ->
                    intent.putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE, contactMap[key])
                ContactSpinnerAdapter.IND_EMAIL2 ->
                    intent.putExtra(ContactsContract.Intents.Insert.SECONDARY_EMAIL, contactMap[key])
            }
        }
        //each content value fills a contacts contract row, the name is set as an extra
        startActivity(intent)
        Toast.makeText(this, "Creating new contact: " + intent.getStringExtra(ContactsContract.Intents.Insert.NAME), Toast.LENGTH_LONG).show()
        finish()
        //end the activity to avoid the mistake of returning here and creating the same contact twice
    }

    fun showDuplicatesDialog(duplicates: ArrayList<EditText>, contactMap: HashMap<Int, String>) {
        val message = StringBuilder("Dropping duplicated values:\n")
        for (id in duplicates) {
            message.append(id.text.toString()).append("\n")
        }
        AlertDialog.Builder(applicationContext)
                .setMessage(message.toString())
                .setPositiveButton(R.string.confirm) { _, _ -> createContact(contactMap) }
                .setNegativeButton(R.string.retry) { dialog, _ -> dialog.dismiss() }
                .create().show()
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        //val previous = parent.tag as Int
        //if (position == previous) return
        parent.tag = position
        //Move map to Contact Fields logic check to save button, nothing to do here!
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}
}