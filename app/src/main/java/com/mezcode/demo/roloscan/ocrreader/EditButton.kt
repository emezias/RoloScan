package com.mezcode.demo.roloscan.ocrreader

import android.text.Editable
import android.text.TextWatcher
import android.widget.ImageButton

/**
 * Created by emezias on 4/29/17.
 * http://stackoverflow.com/questions/4283062/textwatcher-for-more-than-one-edittext
 * This logic will help manage the button that clears the text, set disabled by default
 */
class EditButton(private val mXButton: ImageButton) : TextWatcher {
    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        if (before == 0 && count > 0) {
            mXButton.isEnabled = true
        }
    }

    override fun afterTextChanged(s: Editable) {
        if (s.length > 0) mXButton.isEnabled = true else mXButton.isEnabled = false
    }
}