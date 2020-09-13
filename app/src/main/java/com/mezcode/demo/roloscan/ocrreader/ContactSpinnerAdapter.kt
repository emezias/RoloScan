package com.mezcode.demo.roloscan.ocrreader

import android.content.Context
import android.database.DataSetObserver
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SpinnerAdapter
import android.widget.TextView
import java.util.*

/**
 * Created by emezias on 4/13/17.
 * This spinner adapter is shared among several spinner views
 * Once an item is set, it cannot be reused
 */
class ContactSpinnerAdapter(ctx: Context) : SpinnerAdapter {

    companion object {
        val TAG = ContactSpinnerAdapter::class.java.simpleName
        lateinit var sIcons: IntArray
        lateinit var sLabels: Array<String>
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
        //this is static and need be set only once

        /**
         * This method returns the spinner indices for the ScannedCardToContactActivity
         * if there is no pattern match, contact activity has to handle the default -1 index value
         * @param values - the text blocks read in by the TextDetector
         * @return - the indices for the spinners that have a value
         */
        fun guessIndices(ctx: Context, values: List<String>): HashMap<String, Int> {
            Log.d(TAG, "get Indices")
            sLabels = ctx.resources.getStringArray(R.array.labels)
            sIcons = ctx.resources.getIntArray(R.array.spinicons)
            val sValues = values.toList()
            val selected = ArrayList<Int>()
            //int dex = values.length;
            //try to match string value to correct label
            val map = HashMap<String, Int>()
            var dex: Int
            for (valueShown in values) {
                //if (valueShown.isEmpty()) break
                if (isEmail(valueShown) || Patterns.EMAIL_ADDRESS.matcher(valueShown).matches()) {
                    if (map.values.contains(IND_EMAIL)) {
                        selected.add(IND_EMAIL2)
                        map[valueShown] = IND_EMAIL2
                    } else {
                        selected.add(IND_EMAIL)
                        map[valueShown] = IND_EMAIL
                    }
                    break
                } else if (valueShown.contains("@")) {
                    //not an email, try an IM handle
                    selected.add(IND_IM)
                    map[valueShown] = IND_IM
                    break
                }
                if (isPhone(valueShown) || Patterns.PHONE.matcher(valueShown).matches()
                        || valueShown.contains("(") && valueShown.contains(")")) {
                    if (map.values.contains(IND_PHONE)) {
                        map[valueShown] = IND_PHONE2
                        selected.add(IND_PHONE2)
                    } else {
                        map[valueShown] = IND_PHONE
                        selected.add(IND_PHONE2)
                    }
                    break
                }

                if (isAddress((valueShown)) || //digit to begin and end?
                        (Character.isDigit(valueShown[0]) && Character.isDigit(valueShown[valueShown.length - 1]))) {
                    map[valueShown] = IND_ADDR
                    selected.add(IND_ADDR)
                    break
                }
                if (isName(valueShown)) {
                    selected.add(IND_NAME)
                    map[valueShown] = IND_NAME
                }
                Log.d(TAG, "fell through $valueShown")
                map[valueShown] = -1
            } //end for loop string values
            selected.clear()
            return map
        }

        private fun isName(value: String): Boolean  = "^[a-zA-Z\\\\s]+".toRegex().matches(value)

        private fun isAddress(value: String): Boolean = "/^\\s*\\S+(?:\\s+\\S+){2}/".toRegex().matches(value)

        //almost perfect email regex http://emailregex.com/
        private fun isEmail(value: String): Boolean =
                "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}\\b".toRegex().matches(value)

        //from StackOverflow https://stackoverflow.com/questions/3868753/find-phone-numbers-in-python-script
        private fun isPhone(value: String): Boolean =
                ("(?:(?:\\+?([1-9]|[0-9][0-9]|[0-9][0-9][0-9])\\s*(?:[.-]\\s*)?)?(?:\\(\\s*([2-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9])" +
                        "\\s*\\)|([0-9][1-9]|[0-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9]))\\s*(?:[.-]\\s*)?)?" +
                        "([2-9]1[02-9]|[2-9][02-9]1|[2-9][02-9]{2})\\s*(?:[.-]\\s*)?([0-9]{4})(?:\\s*(?:#|x\\.?|ext\\.?|extension)\\s*(\\d+))?\n")
                        .toRegex().matches(value)
        //ouch... this is a wild one
    }

    override fun getDropDownView(position: Int, v: View?, parent: ViewGroup): View? {
        if (position >= sLabels.size) return null
        var convertView = v ?: LayoutInflater.from(parent.context)
                .inflate(R.layout.spinner_item, null).apply { tag = findViewById(R.id.labelText) }

        (convertView.tag as TextView).text = sLabels[position]
        return convertView
    }

    override fun registerDataSetObserver(dataSetObserver: DataSetObserver) {}
    override fun unregisterDataSetObserver(dataSetObserver: DataSetObserver) {}
    override fun getCount(): Int {
        return sLabels.size
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

    override fun getView(position: Int, v: View?, parent: ViewGroup): View? {
        if (position >= sLabels.size) return null
        val convertView = v ?: LayoutInflater.from(parent.context)
                .inflate(R.layout.spinner_item, null)
                .apply { tag = findViewById(R.id.labelText) }
        //set the row data here
        (convertView.tag as TextView).text = sLabels[position]
        (convertView.tag as TextView).setCompoundDrawablesRelativeWithIntrinsicBounds(
                0, 0, sIcons[position], 0)
        return convertView
    }

    override fun getItemViewType(i: Int): Int {
        return 0
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun isEmpty(): Boolean {
        return sLabels.isNullOrEmpty()
    }

}