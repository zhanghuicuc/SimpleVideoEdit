package com.greymax.android.sve.app.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.greymax.android.sve.app.R;
import com.greymax.android.sve.filters.FilterType;

import java.util.ArrayList;
import java.util.List;


public class FilterListAdapter extends ArrayAdapter<FilterType> {

    static class ViewHolder {
        public TextView text;
    }

    private final Context context;
    private final ArrayList<FilterType> values;

    public FilterListAdapter(Context context, int resource, List<FilterType> objects) {
        super(context, resource, objects);
        this.context = context;
        values = (ArrayList<FilterType>) objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        // reuse views
        if (rowView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(R.layout.row_text, null);
            // configure view holder
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.text = (TextView) rowView.findViewById(R.id.label);
            rowView.setTag(viewHolder);
        }

        ViewHolder holder = (ViewHolder) rowView.getTag();
        String s = values.get(position).name();
        holder.text.setText(s);

        return rowView;
    }


}
