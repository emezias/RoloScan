package com.mezcode.demo.roloscan.ocrreader;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ImageButton;

/**
 * Created by emezias on 4/29/17.
 * http://stackoverflow.com/questions/4283062/textwatcher-for-more-than-one-edittext
 * This logic will help manage the button that clears the text, set disabled by default
 */

public class EditButton implements TextWatcher {
    private final ImageButton mXButton;

    public EditButton(ImageButton btn) {
        mXButton = btn;
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (before == 0 && count > 0) {
            mXButton.setEnabled(true);
        }
    }

    public void afterTextChanged(Editable s) {
        if (s.length() > 0) mXButton.setEnabled(true);
        else mXButton.setEnabled(false);
    }
}
