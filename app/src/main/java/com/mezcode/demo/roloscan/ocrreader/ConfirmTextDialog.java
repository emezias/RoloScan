package com.mezcode.demo.roloscan.ocrreader;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by emezias on 4/22/17.
 * This class is going to display the confirmation after text is scanned
 */

public class ConfirmTextDialog extends DialogFragment {
    public static final String TAG = ConfirmTextDialog.class.getSimpleName();

    public static ConfirmTextDialog newInstance(String diplayText, boolean isPhoto) {
        ConfirmTextDialog fragment = new ConfirmTextDialog();
        //TODO set arguments to read in onCreateView when the start activity gets the scanned text and calls show()
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment, most of the layout is gone, to be set visible based on args
        final View v =  null;
        //TODO create view from dialog_confirm.xml
        return v;
    }

}
