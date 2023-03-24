package com.mezcode.demo.roloscan.ocrreader

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.mezcode.demo.roloscan.ocrreader.databinding.TextListItemBinding

class ScannedTextAdapter(private var scannedText: List<String>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val TAG: String = this::class.java.simpleName
    private val fieldMap = mutableListOf<TextItem>()

    init {
        for(text in scannedText) {
            fieldMap.add(TextItem(text, Utils.guessSpinnerIndex(text)))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        // Inflate XML, don't forget, don't attach
        val binding = TextListItemBinding.inflate(inflater, parent, false)
        val vHolder = LineViewHolder(binding, RecyclerTextChangeListener())
        vHolder.setupSpinnerAdapter()
        return vHolder
    }

    override fun getItemCount(): Int = fieldMap.size


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as LineViewHolder).bind(fieldMap.elementAt(position))
    }

    fun returnContactFields(): List<TextItem> {
        Log.d(TAG, fieldMap.toString())
        val nullList = fieldMap.filter { it.scannedText.isEmpty() }
        if (nullList.isNotEmpty()) {
            for (item in nullList) fieldMap.remove(item)
        }

        for (intentLabel in Utils.SpinnerIndex.values()) {
            val tempList = fieldMap.filter { it.contactLabel == intentLabel }
            if (tempList.isNotEmpty() && tempList.size > 1) {
                //combine the strings so there is only one string for the contact insert field key
                val builder = StringBuilder()
                for(item in tempList) {
                    builder.append(item.scannedText).append("\n")
                    fieldMap.remove(item)
                }
                // drop the last newline here, then reset the string into the field map
                fieldMap.add(TextItem(builder.toString().substring(0, builder.length-2), intentLabel))
            }
        }
        Log.d(TAG, fieldMap.toString())
        return fieldMap.toList()
    }

    inner class RecyclerTextChangeListener : TextWatcher {
        private var position = 0
        fun updatePosition(position: Int) {
            this.position = position
        }

        override fun beforeTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) { }

        override fun onTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {
            val textItem = fieldMap[position]
            textItem.scannedText = charSequence.toString()
        }

        override fun afterTextChanged(editable: Editable) { }
    }

    inner class LineViewHolder(private val binding: TextListItemBinding, private val watcher: RecyclerTextChangeListener) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(textItem: TextItem) {
            watcher.updatePosition(adapterPosition)
            binding.spinner.setSelection(textItem.contactLabel?.dex ?: Utils.SpinnerIndex.IND_NOTES.dex)
            binding.editText.setText(textItem.scannedText)
        }

        /******** Clear text button click listener *********/
        private fun clearText(imageButton: View) {
            // EditText from the layout is set as the button tag in the layout
            imageButton.isEnabled = false
            with(imageButton.tag as EditText) {
                setText("")
            }
        }

        fun setupSpinnerAdapter() {
            binding.clearButton.setOnClickListener { clearText(binding.clearButton) }
            binding.clearButton.tag = binding.editText
            // binding.editText.addTextChangedListener(EditButton(binding.clearButton))
            binding.editText.addTextChangedListener(watcher)
            binding.editText.compoundDrawablePadding = 16
            // text changes are getting lost...
            binding.spinner.adapter = ContactSpinnerAdapter(binding.root.context)
            binding.spinner.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val textSet = binding.editText.text.toString()
                    val spinnerIndex = Utils.SpinnerIndex.values()[position]
                    var textItemToDrop = fieldMap.firstOrNull() { it.scannedText == textSet }
                    if (textItemToDrop != null)  {
                        val replaceIndex = fieldMap.indexOf(textItemToDrop)
                        textItemToDrop = textItemToDrop.copy(contactLabel = spinnerIndex)
                        fieldMap[replaceIndex] = textItemToDrop
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    Log.d(TAG, "spinner nothing selected")
                }
            }
        }

    }
}

data class TextItem(
    var scannedText: String,
    var contactLabel: Utils.SpinnerIndex?)

