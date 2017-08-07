package com.mezcode.demo.roloscan.ocrreader;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by emezias on 4/22/17.
 * This class is going to display the confirmation after text is scanned
 */

public class ConfirmTextDialog extends DialogFragment {
    public static final String TAG = ConfirmTextDialog.class.getSimpleName();

    public static ConfirmTextDialog newInstance(String diplayText, boolean isPhoto) {
        ConfirmTextDialog fragment = new ConfirmTextDialog();
        final Bundle args = new Bundle();
        args.putBoolean(TAG, isPhoto);
        args.putString(StartActivity.TAG, diplayText);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Do not create a new Fragment when the Activity is re-created such as orientation changes.
        setRetainInstance(true);
        setStyle(DialogFragment.STYLE_NORMAL, getTheme());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment, most of the layout is gone, to be set visible based on args
        final View v =  inflater.inflate(R.layout.dialog_confirm, container, false);
        final Bundle params = getArguments();
        final TextView tv = (TextView) v.findViewById(R.id.dlg_message);
        tv.setText(params.getString(StartActivity.TAG));
        tv.setMovementMethod(new ScrollingMovementMethod());
        v.findViewById(R.id.dlg_retry).setTag(params.getBoolean(TAG));
        setCancelable(true);
        return v;
    }

}
