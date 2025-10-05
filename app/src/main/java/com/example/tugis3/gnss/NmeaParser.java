package com.example.tugis3.gnss;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NmeaParser {
    private static final String TAG = "NmeaParser";

    public static class Sentence {
        public String raw;
        public String talker;
        public String type;
        public List<String> fields = new ArrayList<>();
        public String checksum;

        @Override
        public String toString() {
            return "Sentence{" +
                    "talker='" + talker + '\'' +
                    ", type='" + type + '\'' +
                    ", fields=" + fields +
                    ", checksum='" + checksum + '\'' +
                    '}';
        }
    }

    public static Sentence parse(String line) {
        if (line == null) return null;
        line = line.trim();
        if (line.isEmpty()) return null;
        if (!line.startsWith("$")) return null;
        Sentence s = new Sentence();
        s.raw = line;
        try {
            String body, chk = null;
            int asterisk = line.indexOf('*');
            if (asterisk >= 0) {
                body = line.substring(1, asterisk);
                chk = line.substring(asterisk + 1);
            } else {
                body = line.substring(1);
            }
            s.checksum = chk;
            String[] parts = body.split(",");
            if (parts.length > 0) {
                String id = parts[0];
                if (id.length() >= 5) {
                    s.talker = id.substring(0, 2);
                    s.type = id.substring(2);
                } else if (id.length() >= 3) {
                    s.talker = id.substring(0, 2);
                    s.type = id.substring(2);
                } else {
                    s.type = id;
                }
                for (int i = 1; i < parts.length; i++) s.fields.add(parts[i]);
            }
            // Basic logging for debugging
            Log.d(TAG, "Parsed NMEA: " + s.toString());
            return s;
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse NMEA: " + line, e);
            return null;
        }
    }

    public static Map<String, Object> quickSummary(Sentence s) {
        Map<String, Object> map = new HashMap<>();
        if (s == null) return map;
        map.put("talker", s.talker);
        map.put("type", s.type);
        map.put("fieldsCount", s.fields.size());
        return map;
    }

    public static Map<String, Double> extractLatLon(Sentence s) {
        Map<String, Double> map = new HashMap<>();
        if (s == null || s.type == null) return map;
        try {
            String t = s.type.toUpperCase();
            if (t.endsWith("GGA")) {
                // fields: [time, lat, NS, lon, EW, ...]
                if (s.fields.size() >= 5) {
                    String latStr = s.fields.get(1);
                    String ns = s.fields.get(2);
                    String lonStr = s.fields.get(3);
                    String ew = s.fields.get(4);
                    Double lat = nmeaToDecimal(latStr, ns);
                    Double lon = nmeaToDecimal(lonStr, ew);
                    if (lat != null && lon != null) {
                        map.put("lat", lat);
                        map.put("lon", lon);
                    }
                }
            } else if (t.endsWith("RMC")) {
                // fields: [time, status, lat, NS, lon, EW, ...]
                if (s.fields.size() >= 6) {
                    String latStr = s.fields.get(2);
                    String ns = s.fields.get(3);
                    String lonStr = s.fields.get(4);
                    String ew = s.fields.get(5);
                    Double lat = nmeaToDecimal(latStr, ns);
                    Double lon = nmeaToDecimal(lonStr, ew);
                    if (lat != null && lon != null) {
                        map.put("lat", lat);
                        map.put("lon", lon);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "extractLatLon error", e);
        }
        return map;
    }

    private static Double nmeaToDecimal(String val, String hemi) {
        if (val == null || val.isEmpty()) return null;
        try {
            double d = Double.parseDouble(val);
            int deg = (int) (d / 100);
            double min = d - deg * 100;
            double dec = deg + min / 60.0;
            if (hemi != null) {
                hemi = hemi.trim().toUpperCase();
                if (hemi.equals("S") || hemi.equals("W")) dec = -dec;
            }
            return dec;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
