package com.google.android.gms.samples.vision.ocrreader;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by emezias on 4/22/17.
 */

public class ConfirmTextDialog extends DialogFragment {
    public static final String TAG = ConfirmTextDialog.class.getSimpleName();

    public static ConfirmTextDialog newInstance(String[] contactText, String displayText) {
        ConfirmTextDialog fragment = new ConfirmTextDialog();
        final Bundle args = new Bundle();
        args.putStringArray(TAG, contactText);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment, most of the layout is gone, to be set visible based on args
        final View v =  inflater.inflate(R.layout.dialog_confirm, container, false);
        return v;
    }

}
