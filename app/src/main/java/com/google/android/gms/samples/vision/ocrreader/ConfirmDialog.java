package com.google.android.gms.samples.vision.ocrreader;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Created by emezias on 4/30/17.
 */

public class ConfirmDialog extends DialogFragment {
    public static final String TAG = ConfirmDialog.class.getSimpleName();

    public ConfirmDialog newInstance(String[] scannedText) {
        ConfirmDialog frag = new ConfirmDialog();
        Bundle args = new Bundle();
        args.putStringArray(TAG, scannedText);
        frag.setArguments(args);
        return frag;
    }

}
