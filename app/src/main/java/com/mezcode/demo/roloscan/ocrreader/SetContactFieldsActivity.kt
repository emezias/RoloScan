package com.mezcode.demo.roloscan.ocrreader

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.mezcode.demo.roloscan.ocrreader.databinding.ActivityCreateContactBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Created by emezias on 4/20/17.
 * This class takes the scanned text as an extra
 * It displays each line of text in its own edit text
 * The ContactSpinnerAdapter getIndices code applies logic to set the correct spinner value
 * TBH this logic isn't very useful - it is intended to demonstrate the ContactsContract
 * Need to update to use 'query' params in the Manifest
 */
@AndroidEntryPoint
class SetContactFieldsActivity : AppCompatActivity() {
    
    private lateinit var adapter: ScannedTextAdapter
    private lateinit var binding: ActivityCreateContactBinding

    companion object {
        val TAG: String = this::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val contactFields = (intent.extras?.getStringArray(TAG) as Array<String>).toMutableList()
        //scanned text from StartActivity view model instance is packed as an extra
        //could put a different view model here
        if (contactFields.isEmpty()) {
            Utils.showSnackbar(binding.progress, R.string.no_text)
            finish()
            return
        }
        adapter = ScannedTextAdapter(contactFields)
        binding.contactFields.adapter = adapter
        binding.contactFields.layoutManager = LinearLayoutManager(this@SetContactFieldsActivity)
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


    /**
     * This method is called by the action view on the app bar
     * It alerts the user if no text value is mapped to the name contact column
     * Otherwise it calls create contact to put together the Contacts content provider intent and extras
     */
    private fun saveContact() {
        Log.i(TAG, "open Contacts intent")
        val scannedTextList = adapter.returnContactFields()
        val nameKey = Utils.SpinnerIndex.values()[1]
        if (!scannedTextList.containsValue(nameKey) ||
            scannedTextList.values.filter { it == nameKey }.size > 1 ) {
            Utils.showSnackbar(binding.progress, R.string.contact_error)
            return
        }
        // The contact contract should have one and only one name field, not checking the rest...
        val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
            // Sets the MIME type to match the Contacts Provider
            type = ContactsContract.RawContacts.CONTENT_TYPE
            for(scannedText in scannedTextList.keys) {
                val spinDex = scannedTextList[scannedText]
                if (spinDex == null) {
                    putExtra(ContactsContract.Intents.Insert.NOTES, scannedText)
                } else {
                    putExtra(spinDex.key, scannedText)
                }
            }
        } 
        //intent creation complete - boots and suspenders check
        if (intent.hasExtra(ContactsContract.Intents.Insert.NAME)) {
            Toast.makeText(this, "Creating new contact: " + intent.getStringExtra(ContactsContract.Intents.Insert.NAME), Toast.LENGTH_LONG).show()
            startActivity(intent)
            finish()
            //end the activity, don't want to create the same contact twice
        } else {
            Utils.showSnackbar(binding.progress, R.string.contact_error)
            return
        }

    }

    //This logic is guided by the ContactsProvider documentation on the Android dev site
    //https://developer.android.com/training/contacts-provider/modify-data

}