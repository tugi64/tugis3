package com.example.tugis3.coord

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipInputStream

class KmzParseTest {
    @Test
    fun parse_kmz_and_validate_first_coordinate() {
        val kmz = File("C:/Users/CASPER/AndroidStudioProjects/tugis3/kml/kml.kmz")
        assertTrue("KMZ bulunamadı: ${kmz.absolutePath}", kmz.exists())
        var kmlContent: String? = null
        ZipInputStream(kmz.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".kml", ignoreCase = true)) {
                    kmlContent = zis.readBytes().toString(Charsets.UTF_8)
                    break
                }
                entry = zis.nextEntry
            }
        }
        val xml = kmlContent
        assertNotNull("KMZ içinde KML bulunamadı", xml)
        // Basit regex: <coordinates>lon,lat[,h] ...</coordinates>
        val regex = Regex("<coordinates>\\s*([+-]?[0-9]+\\.[0-9]+),([+-]?[0-9]+\\.[0-9]+)")
        val m = regex.find(xml!!)
        assertNotNull("KML içinde koordinat bulunamadı", m)
        val lon = m!!.groupValues[1].toDouble()
        val lat = m.groupValues[2].toDouble()
        // Uşak/Sivaslı/Akarca civarı kaba aralığı
        assertTrue("lat beklenen aralıkta değil: $lat", lat in 38.0..39.0)
        assertTrue("lon beklenen aralıkta değil: $lon", lon in 29.0..30.5)
    }
}

