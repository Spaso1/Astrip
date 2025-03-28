package org.astral.astrip.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.astral.astrip.been.Pointer;

import java.util.List;

public class PointsAdapter extends ArrayAdapter<Pointer> {

    public PointsAdapter(Context context, List<Pointer> points) {
        super(context, 0, points);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Pointer pointer = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        TextView textView = convertView.findViewById(android.R.id.text1);
        textView.setText("ID:" + pointer.getPointId() + " "+pointer.getPointName() + " \n(" + pointer.getX() + ", " + pointer.getY() + ")");

        return convertView;
    }
}
