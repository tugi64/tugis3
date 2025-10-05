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

public class ProjectAdapter extends ArrayAdapter<Project> {

    public ProjectAdapter(@NonNull Context context, List<Project> projects) {
        super(context, 0, projects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_project, parent, false);
        }

        Project project = getItem(position);

        TextView tvProjectName = convertView.findViewById(R.id.tv_project_name);
        TextView tvProjectDate = convertView.findViewById(R.id.tv_project_date);

        if (project != null) {
            tvProjectName.setText(project.getName());
            tvProjectDate.setText(project.getDate());

            if (project.isSelected()) {
                convertView.setBackgroundColor(Color.parseColor("#3366FF")); // Blue highlight
                tvProjectName.setTextColor(Color.WHITE);
                tvProjectDate.setTextColor(Color.WHITE);
            } else {
                convertView.setBackgroundColor(Color.TRANSPARENT);
                tvProjectName.setTextColor(Color.BLACK);
                tvProjectDate.setTextColor(Color.DKGRAY);
            }
        }

        return convertView;
    }
}

