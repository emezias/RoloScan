package com.mezcode.demo.roloscan.ocrreader

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.INVALID_POSITION
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mezcode.demo.roloscan.ocrreader.databinding.TextListItemBinding

class ScannedTextAdapter(private var scannedText: List<String>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val TAG: String = this::class.java.simpleName
    private val fieldMap = mutableMapOf<String, Utils.SpinnerIndex?>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        // Inflate XML, don't forget, don't attach
        val binding = TextListItemBinding.inflate(inflater, parent, false)
        val vHolder = LineViewHolder(binding)
        vHolder.setupSpinnerAdapter()
        return vHolder
    }

    override fun getItemCount(): Int = scannedText.size


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as LineViewHolder).bind(scannedText[position])
    }

    fun returnContactFields(): Map<String, Utils.SpinnerIndex?> = fieldMap.toMap()

    inner class LineViewHolder(private val binding: TextListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(text: String) {
            if (text.isEmpty()) {
                return
            }

            // can use a map to match contact contracts fields to text fields
            var spinnerIndex = fieldMap[text]
            if (spinnerIndex == null) {
                // guess the SpinnerIndex value based on the text when first created
                spinnerIndex = Utils.guessSpinnerIndex(text)
            }
            val spinnerSelection = binding.spinner.selectedItemPosition

            if (spinnerIndex == null) {
                //still null, really?
                if (spinnerSelection != INVALID_POSITION) {
                    spinnerIndex = Utils.SpinnerIndex.values()[spinnerSelection]
                }
            }

            if (spinnerIndex != null && spinnerIndex.dex != spinnerSelection) {
                binding.spinner.setSelection(spinnerIndex.dex)
            }

            with(binding.editText) {
                setText(text)
            }
            fieldMap[text] = spinnerIndex
        }

        /******** Clear text button click listener *********/
        private fun clearText(imageButton: View) {
            // EditText from the layout is set as the button tag in the layout
            imageButton.isEnabled = false
            with(imageButton.tag as EditText) {
                val textToDrop = text.toString()
                fieldMap.remove(textToDrop)
                text.clear()
            }
        }

        fun setupSpinnerAdapter() {
            binding.clearButton.setOnClickListener { clearText(binding.clearButton) }
            binding.clearButton.tag = binding.editText
            binding.editText.addTextChangedListener(EditButton(binding.clearButton))
            binding.editText.compoundDrawablePadding = 16
            /*ArrayAdapter.createFromResource(
                binding.spinner.context,
                R.array.labels,
                android.R.layout.simple_spinner_item
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                // Apply the adapter to the spinner
                binding.spinner.adapter = adapter
            }*/
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
                    fieldMap[textSet] = spinnerIndex
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    Log.d(TAG, "spinner nothing selected")
                }
            }

        }
    }
}
