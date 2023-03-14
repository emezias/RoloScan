package com.mezcode.demo.roloscan.ocrreader

import android.content.Context
import android.database.DataSetObserver
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SpinnerAdapter
import android.widget.TextView

/**
 * Created by emezias on 4/13/17.
 * This spinner adapter is shared among several spinner views
 * Once an item is set, it cannot be reused
 */
class ContactSpinnerAdapter(ctx: Context) : SpinnerAdapter {
    init {
        val rsrcs = ctx.resources
        val thm = ctx.theme
        sValueList = rsrcs.getStringArray(R.array.labels)
        val sz = sValueList.size
        sIcons = arrayOfNulls(sz)
        for (dex in 0 until sz) {
            Log.d(TAG, "resources? " + spinicons[dex])
            sIcons[dex] = rsrcs.getDrawable(
                spinicons[dex], thm
            )
        }
    }

    override fun getDropDownView(position: Int, convertView: View, parent: ViewGroup): View? {
        var newView = convertView
        if (position >= sValueList.size) return null
        if (newView == null) {
            newView = LayoutInflater.from(parent.context).inflate(R.layout.spinner_item, null)
            newView.tag = newView.findViewById(R.id.labelText)
        }
        (newView.tag as TextView).text = sValueList[position]
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

    override fun getView(position: Int, convertView: View, parent: ViewGroup): View? {
        var newView = convertView
        if (position >= sValueList.size) return null
        if (newView == null) {
            newView = LayoutInflater.from(parent.context).inflate(R.layout.spinner_item, null)
            newView.tag = newView.findViewById(R.id.labelText)
        }
        (newView.tag as TextView).text = sValueList[position]
        (newView.tag as TextView).setCompoundDrawablesRelativeWithIntrinsicBounds(
            null, null, sIcons[position], null
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

    companion object {
        val TAG = ContactSpinnerAdapter::class.java.simpleName

        /**
         * <item>Name</item>
         * <item>Phone</item>
         * <item>Email</item>
         * <item>Company</item>
         * <item>Job Title</item>
         * <item>Address</item>
         * <item>I.M.</item>
         * <item>Notes</item>
         * <item>Second Phone</item>
         * <item>Second Email</item>
         */
        const val IND_NAME = 0
        const val IND_PHONE = 1
        const val IND_EMAIL = 2
        const val IND_COMPANY = 3
        const val IND_TITLE = 4
        const val IND_ADDR = 5
        const val IND_IM = 6
        const val IND_NOTES = 7
        const val IND_PHONE2 = 8
        const val IND_EMAIL2 = 9
        val spinicons = intArrayOf(
            R.drawable.ic_account_circle_white_24dp,
            R.drawable.ic_phone_white_24dp,
            R.drawable.ic_mail_outline_white_24dp,
            R.drawable.ic_business_white_24dp,
            R.drawable.ic_title_white_24dp,
            R.drawable.ic_location_on_white_24dp,
            R.drawable.ic_chat_white_24dp,
            R.drawable.ic_note_add_white_24dp,
            R.drawable.ic_phone_white_24dp,
            R.drawable.ic_mail_outline_white_24dp
        )
        lateinit var sValueList: Array<String>
        lateinit var sIcons: Array<Drawable?>

        /**
         * This method returns the spinner indices for the SetContactFieldsActivity
         * if there is no pattern match, contact activity has to handle the default -1 index value
         * @param values - the text blocks read in by the TextDetector
         * @return - the indices for the spinners that have a value
         */
        fun getIndices(values: Array<String>): IntArray {
            Log.d(TAG, "get Indices")
            val selected = ArrayList<Int>()
            //int dex = values.length;
            //try to match string value to correct label
            val map = HashMap<String, Int>()
            for (valueShown in values) {
                if (TextUtils.isEmpty(valueShown)) break
                if (Patterns.EMAIL_ADDRESS.matcher(valueShown).matches()) {
                    if (!selected.contains(IND_EMAIL)) {
                        selected.add(IND_EMAIL)
                        map[valueShown] = IND_EMAIL
                    } else {
                        map[valueShown] = IND_EMAIL2
                    }
                    break
                }
                if (valueShown.contains("@")) {
                    if (valueShown.contains(".")) {
                        if (!selected.contains(IND_EMAIL)) {
                            selected.add(IND_EMAIL)
                            map[valueShown] = IND_EMAIL
                        } else {
                            map[valueShown] = IND_EMAIL2
                        }
                    } else {
                        map[valueShown] = IND_IM
                    }
                    break
                }
                if (Patterns.PHONE.matcher(valueShown)
                        .matches() || valueShown.contains("(") && valueShown.contains(")")
                ) {
                    if (!selected.contains(IND_PHONE)) {
                        map[valueShown] = IND_PHONE
                        selected.add(IND_PHONE)
                    } else {
                        map[valueShown] = IND_PHONE2
                    }
                    break
                }
                if (Patterns.PHONE.matcher(valueShown).matches()) {
                    if (!selected.contains(IND_PHONE)) {
                        map[valueShown] = IND_PHONE
                        selected.add(IND_PHONE)
                    } else {
                        map[valueShown] = IND_PHONE2
                    }
                    break
                }
                if (valueShown.contains("-")) {
                    if (valueShown.substring(valueShown.indexOf("-") + 1).contains("-")) {
                        if (!selected.contains(IND_PHONE)) {
                            map[valueShown] = IND_PHONE
                            selected.add(IND_PHONE)
                        } else {
                            map[valueShown] = IND_PHONE2
                        }
                        break
                    }
                }
                if (Character.isDigit(valueShown[0]) &&
                    Character.isDigit(valueShown[valueShown.length - 1])
                ) {
                    map[valueShown] = IND_ADDR
                    break
                }
                Log.d(TAG, "fell through $valueShown")
                map[valueShown] = -1
            } //end for loop string values
            selected.clear()
            var dex = 0
            val returnValues = IntArray(map.size)
            for (index in map.values) {
                returnValues[dex++] = index
            }
            return returnValues
        }
    }
}