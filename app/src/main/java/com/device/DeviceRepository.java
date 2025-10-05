package com.device;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DeviceRepository {

    public static List<GnssBrand> getSupportedDevices() {
        List<GnssBrand> brands = new ArrayList<>();

        // Topcon
        brands.add(new GnssBrand("Topcon", Arrays.asList(
                new GnssModel("HiPer HR"),
                new GnssModel("HiPer VR"),
                new GnssModel("HiPer SR"),
                new GnssModel("GR-5"),
                new GnssModel("GRS-1")
        )));

        // Trimble
        brands.add(new GnssBrand("Trimble", Arrays.asList(
                new GnssModel("R12i"),
                new GnssModel("R12"),
                new GnssModel("R10"),
                new GnssModel("R8s"),
                new GnssModel("R2"),
                new GnssModel("Catalyst DA2")
        )));

        // Leica
        brands.add(new GnssBrand("Leica", Arrays.asList(
                new GnssModel("GS18 T"),
                new GnssModel("GS16"),
                new GnssModel("Viva GS15"),
                new GnssModel("Viva GS10"),
                new GnssModel("FLX100")
        )));

        // South
        brands.add(new GnssBrand("South", Arrays.asList(
                new GnssModel("Galaxy G1"),
                new GnssModel("Galaxy G1 Plus"),
                new GnssModel("Galaxy G2"),
                new GnssModel("Galaxy G3"),
                new GnssModel("Galaxy G6"),
                new GnssModel("Galaxy G7"),
                new GnssModel("Galaxy G9"),
                new GnssModel("INNO7"),
                new GnssModel("N1"),
                new GnssModel("S660P"),
                new GnssModel("Alps2"),
                new GnssModel("S82")
        )));

        // Sokkia
        brands.add(new GnssBrand("Sokkia", Arrays.asList(
                new GnssModel("GCX3"),
                new GnssModel("GRX3"),
                new GnssModel("Atlas")
        )));

        // CHCNAV
        brands.add(new GnssBrand("CHCNAV", Arrays.asList(
                new GnssModel("i90"),
                new GnssModel("i80"),
                new GnssModel("i73"),
                new GnssModel("i50")
        )));

        // Emlid
        brands.add(new GnssBrand("Emlid", Arrays.asList(
                new GnssModel("Reach RS2+"),
                new GnssModel("Reach RS2"),
                new GnssModel("Reach RX")
        )));

        // Hi-Target
        brands.add(new GnssBrand("Hi-Target", Arrays.asList(
                new GnssModel("V30 Plus"),
                new GnssModel("V90 Plus"),
                new GnssModel("iRTK5"),
                new GnssModel("iRTK4")
        )));

        // Stonex
        brands.add(new GnssBrand("Stonex", Arrays.asList(
                new GnssModel("S900"),
                new GnssModel("S850A"),
                new GnssModel("S9i")
        )));

        // ComNav
        brands.add(new GnssBrand("ComNav", Arrays.asList(
                new GnssModel("T300"),
                new GnssModel("N3"),
                new GnssModel("K8")
        )));

        return brands;
    }
}
