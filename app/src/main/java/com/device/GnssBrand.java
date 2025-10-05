package com.device;

import java.util.List;

public class GnssBrand {
    private String name;
    private List<GnssModel> models;

    public GnssBrand(String name, List<GnssModel> models) {
        this.name = name;
        this.models = models;
    }

    public String getName() {
        return name;
    }

    public List<GnssModel> getModels() {
        return models;
    }
}

