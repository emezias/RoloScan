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
import androidx.appcompat.app.AppCompatActivity
import com.mezcode.demo.roloscan.ocrreader.databinding.ActivityCreateContactBinding

/**
 * Created by emezias on 4/20/17.
 * This class takes the scanned text as an extra
 * It displays each line of text in its own edit text
 * The ContactSpinnerAdapter getIndices code applies logic to set the correct spinner value
 */
class SetContactFieldsActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private lateinit var contactFields: MutableList<String>
    private lateinit var spinDexes: Map<String, Utils.SpinnerIndex?>
    //parallel arrays used to populate the screen with the scanned text
    private lateinit var editFields: Array<EditText>
    private lateinit var btn_fields: Array<ImageButton>
    private lateinit var spinners: Array<Spinner>
    private lateinit var binding: ActivityCreateContactBinding

    companion object {
        val TAG = SetContactFieldsActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        contactFields = (intent.extras?.getStringArray(TAG) as Array<String>).toMutableList()
        //scanned text
        if (contactFields.isEmpty()) {
            Utils.showSnackbar(binding.ccSpinner1, R.string.no_text)
            finish()
            return
        }
        with(binding) {
            editFields = arrayOf(ccEdit1, ccEdit2, ccEdit3, ccEdit4, ccEdit5,
                ccEdit6, ccEdit7, ccEdit8, ccEdit9, ccEdit10 )
            btn_fields = arrayOf(ccBtn1, ccBtn2, ccBtn3, ccBtn4, ccBtn5,
                ccBtn6, ccBtn7, ccBtn8, ccBtn9, ccBtn10)
            spinners = arrayOf(ccSpinner1, ccSpinner2, ccSpinner3, ccSpinner4, ccSpinner5,
                ccSpinner6, ccSpinner7, ccSpinner8, ccSpinner9, ccSpinner10)

        }

        //map value from the scanned text to the contact field types
        spinDexes = Utils.guessIndices(contactFields.toList())
    }

    /**
     * The field setup is done in onResume to always show the data, even if the user presses back
     */
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "on resume ${contactFields.size} and the map ${spinDexes.size}" )
        //spinner index integers to set different labels for all lines of text
        //For loop runs through the rows of spinners and EditText to load the recognized text
        setLayoutOfScannedText()

        //finish resume by setting any leftover text into Notes in last edit text
        if (contactFields.isNotEmpty()) {
            val et = findViewById<EditText>(R.id.cc_edit10)
            val stringToSet = et.text.toString()+"\n"+contactFields.joinToString("\n")
            et.setText(stringToSet)
            et.setSelection(Utils.SpinnerIndex.IND_NOTES.dex)
        } //this happens if there are more strings than fields or strings don't map
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.save_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        saveContact()
        return true
    }

    private fun setLayoutOfScannedText() {
        //For loop runs through the 9 rows of spinners and EditText to load the recognized text
        var spinner: Spinner
        var button: ImageButton
        for ((i, editText) in editFields.withIndex()) {
            //set up each spinner and image button
            button = btn_fields[i]
            button.setOnClickListener {
                clearText(it)
            }
            button.tag = editText
            editText.addTextChangedListener(EditButton(button))

            spinner = spinners[i]
            spinner.adapter = ContactSpinnerAdapter(this)
            //All fields on the row are variables. Inflating the layout to match the scanned text could be better
            spinner.onItemSelectedListener = this

            val textToShow = spinDexes.keys.elementAtOrNull(i)
            textToShow?.let {
                val spinnerIndex = spinDexes[textToShow]
                button.isEnabled = true
                contactFields.remove(textToShow)
                editText.setText(textToShow)
                setSpinner(spinner, button, textToShow)
            } ?: {
                // no text for this row of spinner/edit text/button
                button.isEnabled = false
                spinner.tag = null
            }
        } //end for loop, text fields that matched contact types are set, text listeners enabled
    }

    private fun setSpinner(spinner: Spinner, button: ImageButton, mappedValue: String) {
        val spinnerIndex = spinDexes[mappedValue]
        if (spinnerIndex != null) {
            spinner.setSelection(spinnerIndex.dex)
            spinner.tag = spinnerIndex

        } else {
            spinner.tag = null
        }
    }

    /**
     * This method is called by the action view on the app bar
     * It alerts the user if no text value is mapped to the name contact column
     * Otherwise it calls create contact to put together the Contacts content provider intent and extras
     */
    private fun saveContact() {
        Log.i(TAG, "open Contacts intent")
        val duplicates = mutableSetOf<EditText>()
        val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
            // Sets the MIME type to match the Contacts Provider
            type = ContactsContract.RawContacts.CONTENT_TYPE
            for (editText in editFields) {
                val field = editText.text?.toString()
                if (!field.isNullOrEmpty()) {
                    // apply contact contract extras to the intent when they have text
                    val spindex = spinners[editFields.indexOf(editText)].selectedItemPosition
                    if (spindex != AdapterView.INVALID_POSITION) {
                        // spindex matches enum index, aka ordinal
                        putExtra(Utils.SpinnerIndex.values()[spindex].key, field)
                        Log.d(TAG, "mapping field $field to intent")
                    }
                } else {
                    Log.d(TAG, "skipping field, no text")
                }
            }
        } //intent creation complete

        if (intent.hasExtra(ContactsContract.Intents.Insert.NAME)) {
            startActivity(intent)
            Toast.makeText(this, "Creating new contact: " + intent.getStringExtra(ContactsContract.Intents.Insert.NAME), Toast.LENGTH_LONG).show()
            finish()
            //end the activity, don't want to create the same contact twice
        } else {
            Utils.showSnackbar(findViewById(R.id.cc_spinner1), R.string.contact_error)
            return
        }

    }

    //This logic is guided by the ContactsProvider documentation on the Android dev site
    //https://developer.android.com/training/contacts-provider/modify-data


    /******** Click listeners *********/
    private fun clearText(clearTextButton: View) {
        clearTextButton.isEnabled = false
        (clearTextButton.tag as EditText).text.clear()
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        //val previous = parent.tag as Int
        //if (position == previous) return
        parent.tag = position
        //could have pattern matching logic check here, not sure what the tag is doing anymore
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}
}