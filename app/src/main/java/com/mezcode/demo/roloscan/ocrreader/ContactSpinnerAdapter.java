package com.mezcode.demo.roloscan.ocrreader;

import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;

/**
 * Created by emezias on 4/13/17.
 * This spinner adapter is set on all the spinner views in the SetContactFields activity
 */

public class ContactSpinnerAdapter implements SpinnerAdapter {
    public static final String TAG = ContactSpinnerAdapter.class.getSimpleName();
    /** Labels array in strings.xml
     * <item>Name</item>
     <item>Phone</item>
     <item>Email</item>
     <item>Company</item>
     <item>Job Title</item>
     <item>Address</item>
     <item>I.M.</item>
     <item>Notes</item>
     <item>Second Phone</item>
     <item>Second Email</item>
     */
    static final int IND_NAME = 0;
    static final int IND_PHONE = 1;
    static final int IND_EMAIL = 2;
    static final int IND_COMPANY = 3;
    static final int IND_TITLE = 4;
    static final int IND_ADDR = 5;
    static final int IND_IM = 6;
    static final int IND_NOTES = 7;
    static final int IND_PHONE2 = 8;
    static final int IND_EMAIL2 = 9;

    static String[] sValueList;
    static Drawable[] sIcons;
    final static int[] spinicons = new int[] {
            R.drawable.ic_account_circle_white_24dp,
            R.drawable.ic_phone_white_24dp,
            R.drawable.ic_mail_outline_white_24dp,
            R.drawable.ic_business_white_24dp,
            R.drawable.ic_title_white_24dp,
            R.drawable.ic_location_on_white_24dp,
            R.drawable.ic_chat_white_24dp,
            R.drawable.ic_note_add_white_24dp,
            R.drawable.ic_contact_phone_white_24dp,
            R.drawable.ic_contact_mail_white_24dp
    };

    public ContactSpinnerAdapter(Context ctx) {
        if (sValueList == null) {
            final Resources rsrcs = ctx.getResources();
            final Resources.Theme thm = ctx.getTheme();
            sValueList = rsrcs.getStringArray(R.array.labels);
            final int sz = sValueList.length;
            sIcons = new Drawable[sz];
            for (int dex = 0; dex < sz; dex++) {
                Log.d(TAG, "resources? " + spinicons[dex]);
                sIcons[dex] = rsrcs.getDrawable(spinicons[dex], thm);
            }
        }
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (position >= sValueList.length) return null;
        //TODO inflate view, set text
        return convertView;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver dataSetObserver) { }

    @Override
    public void unregisterDataSetObserver(DataSetObserver dataSetObserver) { }

    @Override
    public int getCount() {
        return sValueList.length;
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position >= sValueList.length) return null;
        //TODO inflate view, set text
        return convertView;
    }

    @Override
    public int getItemViewType(int i) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    /**
     * This method returns the spinner indices for the SetContactFieldsActivity
     * if there is no pattern match, contact activity has to handle the default -1 index value
     * @param values - the text blocks read in by the TextDetector
     * @return - the indices for the spinners that have a value
     */
    public static int[] getIndices(String[] values) {
        //TODO return label indexes for each text value in the String array
        int[] returnValues = new int[]{};
        return returnValues;
    }

}
