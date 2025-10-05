package com.project;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

import androidx.annotation.Nullable;

import com.example.tugis3.R;

import java.util.ArrayList;
import java.util.List;

public class ProjectManagerActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_manager);

        ListView projectListView = findViewById(R.id.project_list);

        List<Project> projects = new ArrayList<>();
        projects.add(new Project("20200213-2", "20200213 09:26", true));
        projects.add(new Project("20200212", "20200212 17:45", false));
        projects.add(new Project("20200213", "20200213 09:26", false));
        projects.add(new Project("20200213-1", "20200213 09:49", false));

        ProjectAdapter adapter = new ProjectAdapter(this, projects);
        projectListView.setAdapter(adapter);
    }
}
