package com.example.tugis3.device;

import com.example.tugis3.gnss.GnssProtocol;

import java.util.Arrays;
import java.util.List;

public class DeviceProtocolMapper {

    /**
     * Basit heuristic ile cihaz adı/modeline göre tercih edilen protokolün index'ini döndürür.
     * Eğer eşleşme yoksa -1 döner.
     */
    public static int getPreferredProtocolIndex(String deviceName, List<GnssProtocol> protocols) {
        if (deviceName == null || deviceName.isEmpty() || protocols == null || protocols.isEmpty()) return -1;
        String n = deviceName.toLowerCase();

        // South marka ve modelleri (ekledik: S82, ALPS2 vb.)
        List<String> southTokens = Arrays.asList(
                "south", "alps2", "alps-2", "s82", "s82 pro", "s82m", "s660", "s660p", "s86", "s86pro"
        );

        // SurvStar-like tokens (genel survey cihazları)
        List<String> survStarTokens = Arrays.asList(
                "survstar", "alps2", "s82", "s660p", "galaxy g1", "galaxy g6", "galaxy g7", "galaxy g9",
                "inno7", "n1", "stonex", "south", "sokkia", "chcnav", "emlid", "hitarget", "hi-target",
                "surveyor", "svs", "sokkia", "s9i"
        );

        // SurvX-like tokens (explicit SurvX names)
        List<String> survXTokens = Arrays.asList(
                "survx", "surv x", "survx4", "survx4.5", "survx4.5", "survx4."
        );

        // Trimble/Topcon/Leica/ComNav common models
        List<String> vendorTokens = Arrays.asList(
                "r12", "r12i", "r10", "r8s", "r2", "hiper", "gr-5", "grs-1",
                "gs18", "gs16", "viva", "t300", "n3", "k8", "hi_per", "topcon", "trimble", "leica", "comnav", "chc"
        );

        // Check SurvX first (explicit)
        for (String t : survXTokens) if (n.contains(t)) return findProtocolIndexByName("SurvX", protocols);

        // Check South-specific tokens next
        for (String t : southTokens) if (n.contains(t)) {
            int idx = findProtocolIndexByName("South", protocols);
            if (idx >= 0) return idx;
            // fallback to SurvStar if South-specific protocol not present
            idx = findProtocolIndexByName("SurvStar", protocols);
            if (idx >= 0) return idx;
        }

        // Check SurvStar tokens
        for (String t : survStarTokens) if (n.contains(t)) return findProtocolIndexByName("SurvStar", protocols);

        // Vendor tokens fallback
        for (String t : vendorTokens) if (n.contains(t)) {
            int idx = findProtocolIndexByName("SurvStar", protocols);
            if (idx >= 0) return idx;
        }

        // Specific common models mapping (additional)
        if (n.contains("reach rs2") || n.contains("reach rx") || n.contains("emlid")) {
            int idx = findProtocolIndexByName("SurvStar", protocols);
            if (idx >= 0) return idx;
        }

        // If nothing matched, try partial match on protocol names
        int byName = findProtocolIndexByName("surv", protocols);
        if (byName >= 0) return byName;

        return -1;
    }

    private static int findProtocolIndexByName(String key, List<GnssProtocol> protocols) {
        for (int i = 0; i < protocols.size(); i++) {
            String pname = protocols.get(i).getName().toLowerCase();
            if (pname.contains(key.toLowerCase())) return i;
        }
        return -1;
    }
}
