package com.mezcode.demo.roloscan.ocrreader

import android.content.*
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.activity_start.*
import kotlinx.android.synthetic.main.dialog_confirm.*

/**
 * Created by emezias on 4/22/17.
 * This class shows a confirmation the user to check scanned text for accuracy
 * The user can retry, create a contact record, share the text or copy to the pasteboard
 */
class ConfirmTextDialog : DialogFragment(), View.OnClickListener {
    companion object {
        val TAG = ConfirmTextDialog::class.simpleName
        fun newInstance(displayText: Array<String>, isPhoto: Boolean): ConfirmTextDialog {
            val fragment = ConfirmTextDialog()
            val args = Bundle()
            with (args) {
                putBoolean(TAG, isPhoto)
                putStringArray(StartActivity.TAG, displayText)
            }
            fragment.arguments = args
            return fragment
        }
    }

    /*override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Do not create a new Fragment when the Activity is re-created such as orientation changes.
        *//*this.retainInstance = true
        setStyle(STYLE_NORMAL, theme)*//*
        isCancelable = true
    }*/

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment, most of the layout is gone, to be set visible based on args
        return inflater.inflate(R.layout.dialog_confirm, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            dlg_message.text = it.getStringArray(StartActivity.TAG)?.toList()?.joinToString(separator = "\n")
                    ?: "text not found"
            dlg_retry.tag = it.getBoolean(TAG)
        }
        dlg_message.movementMethod = ScrollingMovementMethod()
        isCancelable = true

        dlg_confirm.setOnClickListener(this)
        dlg_retry.setOnClickListener(this)
        dlg_clipboard.setOnClickListener(this)
        dlg_share.setOnClickListener(this)
    }

    /**
     * Fragment OnClickListener for 4 buttons
     * Toast response in case of copy, otherwise startActivity
     * Each button uses the button view context which can't be null
     */
    override fun onClick(btn: View) {
        Log.v(StartActivity.TAG, "confirm dialog button ${dlg_message.text}")
        when (btn) {
            dlg_confirm -> {
                val tnt = Intent(btn.context, ScannedCardToContactActivity::class.java)
                tnt.putExtra(ScannedCardToContactActivity.TAG, arguments?.getStringArray(StartActivity.TAG))
                btn.context.startActivity(tnt)
            }
            dlg_retry -> with(context as StartActivity) {
                if (btn.tag as Boolean) {
                    this.getPhoto(start_gallery)
                } else {
                    this.getPhoto(start_photo)
                }
            }
            dlg_clipboard -> {
                val clipboard = btn.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(getString(R.string.scan2label), dlg_message.text)
                Log.v(StartActivity.TAG, "confirm dialog clip ${dlg_message.text}")
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
            }
            dlg_share -> {
                val intent = Intent(Intent.ACTION_SEND)
                intent.putExtra(Intent.EXTRA_TEXT, dlg_message.text)
                intent.type = ClipDescription.MIMETYPE_TEXT_PLAIN
                val chooser = Intent.createChooser(intent, resources.getString(R.string.share))
                // Verify the intent will resolve to at least one activity
                if (intent.resolveActivity(btn.context.packageManager) != null) {
                    btn.context.startActivity(chooser)
                } else {
                    Toast.makeText(context, R.string.no_app, Toast.LENGTH_SHORT).show()
                }
            }
        }
        dismiss()
    }

}