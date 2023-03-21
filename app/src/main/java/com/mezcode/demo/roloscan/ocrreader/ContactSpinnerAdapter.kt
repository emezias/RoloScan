package com.mezcode.demo.roloscan.ocrreader

import android.content.Context
import android.database.DataSetObserver
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SpinnerAdapter
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat

/**
 * Created by emezias on 4/13/17.
 * This spinner adapter is shared among several spinner views
 * Once an item is set, it cannot be reused
 */
class ContactSpinnerAdapter(ctx: Context) : SpinnerAdapter {
    val TAG = ContactSpinnerAdapter::class.java.simpleName

    lateinit var sValueList: Array<String>
    lateinit var spinIcons: Set<Drawable?>

    init {
        val rsrcs = ctx.resources
        val thm = ctx.theme
        sValueList = rsrcs.getStringArray(R.array.labels)
        spinIcons = setOf(
            ResourcesCompat.getDrawable(rsrcs, R.drawable.ic_account_circle_white_24dp, thm),
            ResourcesCompat.getDrawable(rsrcs, R.drawable.ic_phone_white_24dp, thm),
            ResourcesCompat.getDrawable(rsrcs, R.drawable.ic_mail_outline_white_24dp, thm),
            ResourcesCompat.getDrawable(rsrcs, R.drawable.ic_business_white_24dp, thm),
            ResourcesCompat.getDrawable(rsrcs, R.drawable.ic_title_white_24dp, thm),
            ResourcesCompat.getDrawable(rsrcs, R.drawable.ic_location_on_white_24dp, thm),
            ResourcesCompat.getDrawable(rsrcs, R.drawable.ic_chat_white_24dp, thm),
            ResourcesCompat.getDrawable(rsrcs, R.drawable.ic_note_add_white_24dp, thm),
            ResourcesCompat.getDrawable(rsrcs, R.drawable.ic_phone_white_24dp, thm),
            ResourcesCompat.getDrawable(rsrcs, R.drawable.ic_mail_outline_white_24dp, thm),
        )
        // cannot set R drawable values as an integer array in values
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View? {
        var newView: View? = convertView
        if (position >= sValueList.size) return null
        if (newView == null) {
            newView = LayoutInflater.from(parent.context).inflate(R.layout.spinner_item, null)
            newView.tag = newView.findViewById(R.id.labelText)
        }
        (newView?.tag as TextView).text = sValueList[position]
        return newView
    }

    override fun registerDataSetObserver(dataSetObserver: DataSetObserver) {}
    override fun unregisterDataSetObserver(dataSetObserver: DataSetObserver) {}
    override fun getCount(): Int {
        return sValueList.size
    }

    override fun getItem(i: Int): Any? {
        return null
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        var newView : View? = convertView
        if (position >= sValueList.size) return null
        if (newView == null) {
            newView = LayoutInflater.from(parent.context).inflate(R.layout.spinner_item, null)
            newView.tag = newView.findViewById(R.id.labelText)
        }
        (newView?.tag as TextView).text = sValueList[position]
        (newView.tag as TextView).setCompoundDrawablesRelativeWithIntrinsicBounds(
            null, null, spinIcons.elementAt(position), null
        )
        return newView
    }

    override fun getItemViewType(i: Int): Int {
        return 0
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun isEmpty(): Boolean {
        return false
    }

}