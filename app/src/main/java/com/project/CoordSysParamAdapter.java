package com.project;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tugis3.R;

import java.util.List;

public class CoordSysParamAdapter extends ArrayAdapter<CoordSysParameter> {

    public CoordSysParamAdapter(@NonNull Context context, List<CoordSysParameter> parameters) {
        super(context, 0, parameters);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_coord_param, parent, false);
        }

        CoordSysParameter parameter = getItem(position);

        ImageView ivParamIcon = convertView.findViewById(R.id.iv_param_icon);
        TextView tvParamName = convertView.findViewById(R.id.tv_param_name);

        if (parameter != null) {
            ivParamIcon.setImageResource(parameter.getIconResId());
            tvParamName.setText(parameter.getName());
        }

        return convertView;
    }
}

