package com.google.android.gms.samples.vision.ocrreader;

import android.content.Context;
import android.database.DataSetObserver;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by emezias on 4/13/17.
 * This spinner adapter is shared among several spinner views
 * Once an item is set, it cannot be reused
 */

public class ContactSpinnerAdapter implements SpinnerAdapter {

    public static final String[] contactKeys = {
            ContactsContract.Intents.Insert.NAME,
            ContactsContract.Intents.Insert.PHONE,
            ContactsContract.Intents.Insert.EMAIL,
            ContactsContract.Intents.Insert.COMPANY,
            ContactsContract.Intents.Insert.JOB_TITLE,
            ContactsContract.Intents.Insert.POSTAL,
            ContactsContract.Intents.Insert.IM_HANDLE,
            ContactsContract.Intents.Insert.NOTES,
            ContactsContract.Intents.Insert.SECONDARY_PHONE,
            ContactsContract.Intents.Insert.SECONDARY_EMAIL
    };

    static String[] sValueList;
    static final ArrayList<Integer> selectedValues = new ArrayList<>();

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
        if (selectedValues.contains(position)) {
            ((View)convertView.getTag()).setEnabled(false);
        } else {
            ((View)convertView.getTag()).setEnabled(true);
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
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    //These two methods help keep things in order
    public void setDefault(int valueShown) {
        selectedValues.add(valueShown);
    }

    public void setNewSelection(int selection, int tagValue) {
        selectedValues.remove(tagValue);
        selectedValues.add(selection);
    }

    public boolean checkUnique(int newSelection) {
        return selectedValues.contains(newSelection);
    }
}
