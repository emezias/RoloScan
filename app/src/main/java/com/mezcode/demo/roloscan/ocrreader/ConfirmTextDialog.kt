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
import androidx.fragment.app.activityViewModels
import com.mezcode.demo.roloscan.ocrreader.databinding.DialogConfirmBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Created by emezias on 4/22/17.
 * This class shows a confirmation the user to check scanned text for accuracy
 * The user can retry, create a contact record, share the text or copy to the pasteboard
 */
@AndroidEntryPoint
class ConfirmTextDialog : DialogFragment(), View.OnClickListener {
    private lateinit var binding: DialogConfirmBinding
    private val viewModel: OCRViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment, most of the layout is gone, to be set visible based on args
        binding = DialogConfirmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            dlgMessage.text = viewModel.scannedTextCache.joinToString(separator = "\n")
            dlgMessage.movementMethod = ScrollingMovementMethod()
            dlgRetry.setOnClickListener(this@ConfirmTextDialog)
            dlgConfirm.setOnClickListener(this@ConfirmTextDialog)
            dlgShare.setOnClickListener(this@ConfirmTextDialog)
            dlgClipboard.setOnClickListener(this@ConfirmTextDialog)
        }
        isCancelable = true
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        dismiss()
    }



    /**
     * Fragment OnClickListener for 4 buttons
     * Toast response in case of copy, otherwise startActivity
     * Each button uses the button view context which can't be null
     */
    override fun onClick(btn: View) {
        Log.v(tag, "confirm dialog button ${binding.dlgMessage.text}")
        when (btn.id) {
            R.id.dlg_confirm -> {
                val tnt = Intent(btn.context, SetContactFieldsActivity::class.java)
                tnt.putExtra(SetContactFieldsActivity.TAG, viewModel.scannedTextCache.toTypedArray())
                btn.context.startActivity(tnt)
            }
            R.id.dlg_retry -> {
                dialog?.cancel()
            }
            R.id.dlg_clipboard -> {
                val clipboard = btn.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(getString(R.string.scan2label), binding.dlgMessage.text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
            }
            R.id.dlg_share -> {
                val intent = Intent(Intent.ACTION_SEND)
                intent.putExtra(Intent.EXTRA_TEXT, binding.dlgMessage.text)
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
        dialog?.dismiss()
    }

}