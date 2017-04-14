package com.google.android.gms.samples.vision.ocrreader;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

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
    public static final String BLANK = "Unused";

    static final ArrayList<String> sValueList = new ArrayList<>();
    static final ArrayList<Integer> selectedValues = new ArrayList<>();

    public ContactSpinnerAdapter(Context ctx) {
        if (!sValueList.contains(BLANK)) {
            sValueList.add(BLANK);
            sValueList.addAll(Arrays.asList(
                    ctx.getResources().getStringArray(R.array.Labels)
            ));
        } //data ready

    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (position >= sValueList.size()) return null;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.spinner_item, null);
            convertView.setTag(convertView.findViewById(R.id.labelText));
        }
        if (selectedValues.contains(position)) {
            ((View)convertView.getTag()).setEnabled(false);
        } else {
            ((View)convertView.getTag()).setEnabled(true);
        }
        ((TextView)convertView.getTag()).setText(sValueList.get(position));
        return convertView;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver dataSetObserver) { }

    @Override
    public void unregisterDataSetObserver(DataSetObserver dataSetObserver) { }

    @Override
    public int getCount() {
        return sValueList.size();
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
        if (position >= sValueList.size()) return null;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.spinner_item, null);
            convertView.setTag(convertView.findViewById(R.id.labelText));
        }
        ((TextView)convertView.getTag()).setText(sValueList.get(position));
        ((TextView)convertView.getTag()).setTextColor(Color.BLACK);
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
}
