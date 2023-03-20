package com.mezcode.demo.roloscan.ocrreader

import android.content.*
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment

/**
 * Created by emezias on 4/22/17.
 * This class shows a confirmation the user to check scanned text for accuracy
 * The user can retry, create a contact record, share the text or copy to the pasteboard
 */
class ConfirmTextDialog : DialogFragment(), View.OnClickListener {
    companion object {
        val TAG = ConfirmTextDialog::class.simpleName
        fun newInstance(displayText: List<String>, isPhoto: Boolean): ConfirmTextDialog {
            val fragment = ConfirmTextDialog()
            val args = Bundle()
            with (args) {
                putBoolean(TAG, isPhoto)
                putStringArray(StartActivity.TAG, displayText.toTypedArray())
            }
            fragment.arguments = args
            return fragment
        }
    }

    lateinit var messageTxt: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment, most of the layout is gone, to be set visible based on args
        return inflater.inflate(R.layout.dialog_confirm, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        messageTxt = view.findViewById<TextView>(R.id.dlg_message)
        val retryButton = view.findViewById<ImageButton>(R.id.dlg_retry)
        arguments?.let {
            messageTxt.text = it.getStringArray(StartActivity.TAG)?.toList()?.joinToString(separator = "\n")
                    ?: "text not found"
            retryButton.tag = it.getBoolean(TAG)
        }
        messageTxt.movementMethod = ScrollingMovementMethod()
        isCancelable = true

        view.findViewById<ImageButton>(R.id.dlg_confirm).setOnClickListener(this)
        retryButton.setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.dlg_clipboard).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.dlg_share).setOnClickListener(this)
    }

    /**
     * Fragment OnClickListener for 4 buttons
     * Toast response in case of copy, otherwise startActivity
     * Each button uses the button view context which can't be null
     */
    override fun onClick(btn: View) {
        Log.v(StartActivity.TAG, "confirm dialog button ${messageTxt.text}")
        when (btn.id) {
            R.id.dlg_confirm -> {
                val tnt = Intent(btn.context, SetContactFieldsActivity::class.java)
                tnt.putExtra(SetContactFieldsActivity.TAG, arguments?.getStringArray(StartActivity.TAG))
                btn.context.startActivity(tnt)
            }
            R.id.dlg_retry -> with(context as StartActivity) {
                if (btn.tag as Boolean) {
                    this.getPhoto(this.startGallery)
                } else {
                    this.getPhoto(startPhoto)
                }
            }
            R.id.dlg_clipboard -> {
                val clipboard = btn.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(getString(R.string.scan2label), messageTxt.text)
                Log.v(StartActivity.TAG, "confirm dialog clip ${messageTxt.text}")
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
            }
            R.id.dlg_share -> {
                val intent = Intent(Intent.ACTION_SEND)
                intent.putExtra(Intent.EXTRA_TEXT, messageTxt.text)
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