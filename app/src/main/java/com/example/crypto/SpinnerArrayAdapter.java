package com.example.crypto;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tobias on 28.04.16.
 */
public class SpinnerArrayAdapter extends ArrayAdapter {

    private int _textEnabledColor;
    private int _textDisabledColor;

    private List<Integer> _disabledPositions = new ArrayList<>();

    public SpinnerArrayAdapter(Context context, int resource, String[] objects) {
        super(context, resource, objects);

        _textEnabledColor = Color.BLACK;
        _textDisabledColor = ContextCompat.getColor(context, R.color.colorAccent);
    }

    @Override
    public boolean isEnabled(int position) {
        if (position == 0 || _disabledPositions.contains(position)) {
            return false;
        }

        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = super.getView(position, convertView, parent);

        if (position == 0) {
            // Hint farblich anpassen
            TextView tevHint = (TextView) v.findViewById(android.R.id.text1);
            tevHint.setTextColor(_textDisabledColor);
        }

        return v;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View v = null;

        if (position == 0) {
            TextView tv = new TextView(getContext());
            tv.setHeight(0);
            v = tv;
        } else {
            v = super.getDropDownView(position, null, parent);

            if (_disabledPositions.contains(position)) {
                ((TextView) v).setTextColor(_textDisabledColor);
            } else {
                ((TextView) v).setTextColor(_textEnabledColor);
            }
        }

        parent.setVerticalScrollBarEnabled(false);
        return v;
    }

    public int getPosition(String item) {
        for (int i = 0; i < this.getCount(); i++) {
            String currItem = (String) this.getItem(i);

            if (currItem.equals(item)) {
                return i;
            }
        }

        return -1;
    }

    public void setDisabledItems(String[] items) {
        for (String item : items) {
            int pos = getPosition(item);
            _disabledPositions.add(pos);
        }
    }
}
