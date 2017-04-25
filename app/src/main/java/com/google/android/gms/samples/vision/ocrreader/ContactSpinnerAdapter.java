package com.google.android.gms.samples.vision.ocrreader;

import android.content.Context;
import android.database.DataSetObserver;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by emezias on 4/13/17.
 * This spinner adapter is shared among several spinner views
 * Once an item is set, it cannot be reused
 */

public class ContactSpinnerAdapter implements SpinnerAdapter {
    public static final String TAG = ContactSpinnerAdapter.class.getSimpleName();
    static String[] sValueList;

    public ContactSpinnerAdapter(Context ctx) {
        if (sValueList == null) sValueList = ctx.getResources().getStringArray(R.array.labels);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (position >= sValueList.length) return null;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.spinner_item, null);
            convertView.setTag(convertView.findViewById(R.id.labelText));
        }

        ((TextView)convertView.getTag()).setText(sValueList[position]);
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
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.spinner_item, null);
            convertView.setTag(convertView.findViewById(R.id.labelText));
        }
        ((TextView)convertView.getTag()).setText(sValueList[position]);
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
    static final int IND_PHONE1 = 1;
    static final int IND_IM = 6;
    static final int IND_EMAIL = 2;
    static final int IND_ADDR = 5;
    public static int[] getIndices(String[] values) {
        Log.d(TAG, "get Indices");
        final ArrayList<Integer> selected = new ArrayList<>();
        int dex;
        for (dex = 0; dex < values.length; dex++) {
            selected.add(dex, dex);
        }
        //try to match string value to correct label
        final HashMap<String, Integer> map = new HashMap<>();
        for (String valueShown : values) {
            if (TextUtils.isEmpty(valueShown)) break;
            if (android.util.Patterns.EMAIL_ADDRESS.matcher(valueShown).matches()) {
                if (selected.contains(IND_EMAIL)) {
                    selected.remove(selected.indexOf(IND_EMAIL));
                    map.put(valueShown, IND_EMAIL);
                    break;
                }
            } else if (valueShown.contains("@")) {
                if (valueShown.contains(".")) {
                    if (selected.contains(IND_EMAIL)) {
                        selected.remove(selected.indexOf(IND_EMAIL));
                        map.put(valueShown, IND_EMAIL);
                        break;
                    }
                } else {
                    if (selected.contains(IND_IM)) {
                        selected.remove(selected.indexOf(IND_IM));
                        map.put(valueShown, IND_IM);
                        break;
                    }
                }
            } else if (valueShown.contains("(") && valueShown.contains(")")) {
                if (selected.contains(IND_PHONE1)) {
                    selected.remove(selected.indexOf(IND_PHONE1));
                    map.put(valueShown, IND_PHONE1);
                    break;
                }
            } else if (Patterns.PHONE.matcher(valueShown).matches()) {
                if (selected.contains(IND_PHONE1)) {
                    selected.remove(selected.indexOf(IND_PHONE1));
                    map.put(valueShown, IND_PHONE1);
                    break;
                }
            } else if (valueShown.indexOf("-") > -1) {
                dex = valueShown.indexOf("-");
                if (valueShown.substring(dex+1).contains("-") && selected.contains(IND_PHONE1)) {
                    selected.remove(selected.indexOf(IND_PHONE1));
                    map.put(valueShown, IND_PHONE1);
                    break;
                }
            }else if (Character.isDigit(valueShown.charAt(0)) &&
                    Character.isDigit(valueShown.charAt(valueShown.length() - 1))) {
                if (selected.contains(IND_ADDR)) {
                    selected.remove(selected.indexOf(IND_ADDR));
                    map.put(valueShown, IND_ADDR);
                    break;
                }

            }
            //map.put(valueShown, selected.get(0));
            //selected.remove(0);
            Log.d(TAG, "fell through " + valueShown);
        } //end for loop string values
        selected.trimToSize();
        int[] returnValues = new int[map.size()+selected.size()];
        Log.d(TAG, "set up return array, sz " + returnValues.length);
        dex = 0;
        //put mapped indexes into the array
        for (String realVal: values) {
            if (!TextUtils.isEmpty(realVal) && map.containsKey(realVal)) {
                returnValues[dex++] = map.get(realVal);
            } else {
                returnValues[dex++] = selected.get(0);
                selected.remove(0);
            }
        }
        //fill out the fields with the rest of the label array
        for (Integer value: selected) {
            //if (dex < returnValues.length)
            returnValues[dex++] = value;
        }
        return returnValues;
    }

}
