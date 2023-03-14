package com.mezcode.demo.roloscan.ocrreader

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment

/**
 * Created by emezias on 4/22/17.
 * This class is going to display the confirmation after text is scanned
 */
class ConfirmTextDialog : DialogFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Do not create a new Fragment when the Activity is re-created such as orientation changes.
        retainInstance = true
        setStyle(DialogFragment.STYLE_NORMAL, getTheme())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment, most of the layout is gone, to be set visible based on args
        val v: View = inflater.inflate(R.layout.dialog_confirm, container, false)
        val params = arguments
        val tv: TextView = v.findViewById<View>(R.id.dlg_message) as TextView
        if (params != null) {
            tv.text = params.getString(StartActivity.Companion.TAG)
            v.findViewById<View>(R.id.dlg_retry).tag = params.getBoolean(TAG)
        }
        tv.movementMethod = ScrollingMovementMethod()
        isCancelable = true
        return v
    }

    companion object {
        val TAG = ConfirmTextDialog::class.java.simpleName
        fun newInstance(displayText: String?, isPhoto: Boolean): ConfirmTextDialog {
            val fragment = ConfirmTextDialog()
            val args = Bundle()
            args.putBoolean(TAG, isPhoto)
            args.putString(StartActivity.Companion.TAG, displayText)
            fragment.arguments = args
            return fragment
        }
    }
}