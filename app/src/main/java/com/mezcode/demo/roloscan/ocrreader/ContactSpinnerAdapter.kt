package com.mezcode.demo.roloscan.ocrreader

import android.content.Context
import android.database.DataSetObserver
import android.graphics.drawable.Drawable
import android.util.Log
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
class ContactSpinnerAdapter(private val ctx: Context) : SpinnerAdapter {
    private val TAG: String = this::class.java.simpleName
    private var sValueList: Array<String> = ctx.resources.getStringArray(R.array.labels)
    
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View? {
        var newView: View? = convertView
        if (position >= sValueList.size) return null
        if (convertView == null) {
            newView = LayoutInflater.from(parent.context).inflate(R.layout.spinner_item, null)
        }
        (newView as TextView).text = sValueList[position]
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
        }
        
        with (newView as TextView) {
            text = sValueList[position]
            //Log.d(TAG, "setting drawable ${Utils.SpinnerIndex.values()[position].drawableResource}")
            setCompoundDrawablesWithIntrinsicBounds(
                0, 0, Utils.SpinnerIndex.values()[position].drawableResource, 0)
        }
        Log.d(TAG, "text got set to ${sValueList[position]}")
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