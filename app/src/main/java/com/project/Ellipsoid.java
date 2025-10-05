package com.project;

public class Ellipsoid {
    private String name;
    private String majorAxis;
    private String inverseFlattening;
    private boolean isSelected;

    public Ellipsoid(String name, String majorAxis, String inverseFlattening, boolean isSelected) {
        this.name = name;
        this.majorAxis = majorAxis;
        this.inverseFlattening = inverseFlattening;
        this.isSelected = isSelected;
    }

    public String getName() {
        return name;
    }

    public String getMajorAxis() {
        return majorAxis;
    }

    public String getInverseFlattening() {
        return inverseFlattening;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}

