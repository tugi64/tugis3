package com.project;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CsvParser {

    public static List<Ellipsoid> parseEllipsoids(Context context) {
        List<Ellipsoid> ellipsoids = new ArrayList<>();
        try {
            InputStream inputStream = context.getAssets().open("Projeksions.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            boolean isFirstLine = true;
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                String[] tokens = line.split(",");
                if (tokens.length > 4) {
                    String ellipsoidName = tokens[2];
                    String majorAxis = tokens[3];
                    String invFlattening = tokens[4];
                    // Simple check to avoid duplicates
                    boolean exists = false;
                    for (Ellipsoid e : ellipsoids) {
                        if (e.getName().equals(ellipsoidName)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        ellipsoids.add(new Ellipsoid(ellipsoidName, majorAxis, invFlattening, false));
                    }
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ellipsoids;
    }
}

