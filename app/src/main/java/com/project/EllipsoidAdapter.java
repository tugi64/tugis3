package com.project;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tugis3.R;

import java.util.List;

public class EllipsoidAdapter extends ArrayAdapter<Ellipsoid> {

    public EllipsoidAdapter(@NonNull Context context, List<Ellipsoid> ellipsoids) {
        super(context, 0, ellipsoids);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_ellipsoid, parent, false);
        }

        Ellipsoid ellipsoid = getItem(position);

        TextView tvName = convertView.findViewById(R.id.tv_ellipsoid_name);
        TextView tvMajorAxis = convertView.findViewById(R.id.tv_ellipsoid_major_axis);
        TextView tvInvFlat = convertView.findViewById(R.id.tv_ellipsoid_inv_flat);

        if (ellipsoid != null) {
            tvName.setText(ellipsoid.getName());
            tvMajorAxis.setText(ellipsoid.getMajorAxis());
            tvInvFlat.setText(ellipsoid.getInverseFlattening());

            if (ellipsoid.isSelected()) {
                convertView.setBackgroundColor(Color.parseColor("#3366FF"));
                tvName.setTextColor(Color.WHITE);
                ((TextView)convertView.findViewById(R.id.textView5)).setTextColor(Color.WHITE);
                ((TextView)convertView.findViewById(R.id.textView6)).setTextColor(Color.WHITE);
                tvMajorAxis.setTextColor(Color.WHITE);
                tvInvFlat.setTextColor(Color.WHITE);
            } else {
                convertView.setBackgroundColor(Color.TRANSPARENT);
                tvName.setTextColor(Color.BLACK);
                ((TextView)convertView.findViewById(R.id.textView5)).setTextColor(Color.DKGRAY);
                ((TextView)convertView.findViewById(R.id.textView6)).setTextColor(Color.DKGRAY);
                tvMajorAxis.setTextColor(Color.DKGRAY);
                tvInvFlat.setTextColor(Color.DKGRAY);
            }
        }

        return convertView;
    }
}

