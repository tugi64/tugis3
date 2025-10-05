package com.project;

public class Project {
    private String name;
    private String date;
    private boolean isSelected;

    public Project(String name, String date, boolean isSelected) {
        this.name = name;
        this.date = date;
        this.isSelected = isSelected;
    }

    public String getName() {
        return name;
    }

    public String getDate() {
        return date;
    }

    public boolean isSelected() {
        return isSelected;
    }
}

