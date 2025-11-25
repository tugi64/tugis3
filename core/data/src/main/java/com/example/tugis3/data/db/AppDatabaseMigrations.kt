package com.example.tugis3.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object AppDatabaseMigrations {
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE points ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        }
    }
    // v6 -> v7 geçişi: Yapısal değişiklik yok; sadece sürüm artırımı
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) { /* No-op */ }
    }
    // v7 -> v8: points tablosuna index eklendi (projectId, name)
    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS index_points_projectId ON points(projectId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_points_name ON points(name)")
        }
    }
    // v8 -> v9 geçişi: No-op migration, sadece sürüm artırımı
    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) { /* No-op */ }
    }
    // v9 -> v10 : yeni survey_points, detail_features, detail_feature_points tabloları
    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS survey_points (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    projectId INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    code TEXT,
                    latitude REAL,
                    longitude REAL,
                    elevation REAL,
                    northing REAL,
                    easting REAL,
                    zone TEXT,
                    hrms REAL,
                    vrms REAL,
                    pdop REAL,
                    satellites INTEGER,
                    fixType TEXT,
                    antennaHeight REAL,
                    timestamp INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )""".trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_survey_points_projectId ON survey_points(projectId)")
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS detail_features (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    projectId INTEGER NOT NULL,
                    type TEXT NOT NULL,
                    code TEXT,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )""".trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_detail_features_projectId ON detail_features(projectId)")
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS detail_feature_points (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    featureId INTEGER NOT NULL,
                    x REAL NOT NULL,
                    y REAL NOT NULL,
                    z REAL,
                    code TEXT,
                    orderIndex INTEGER NOT NULL,
                    timestamp INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )""".trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_detail_feature_points_featureId ON detail_feature_points(featureId)")
        }
    }
    // v10 -> v11: points tablosuna soft delete için sütunlar ve index eklendi
    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE points ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE points ADD COLUMN deletedAt INTEGER")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_points_projectId_deleted ON points(projectId, deleted)")
        }
    }
    // v11 -> v12: projects tablosuna utmZone, utmNorthHemisphere, epsgCode
    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE projects ADD COLUMN utmZone INTEGER")
            db.execSQL("ALTER TABLE projects ADD COLUMN utmNorthHemisphere INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE projects ADD COLUMN epsgCode INTEGER")
        }
    }
    // v12 -> v13: Önceki sürüm artışı (bilinmeyen değişiklik) için no-op placeholder
    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) { /* No-op placeholder */ }
    }
    // v13 -> v14: localization için projects tablosuna sütun eklenmesi ve calibration_points tablosu
    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE projects ADD COLUMN locScale REAL")
            db.execSQL("ALTER TABLE projects ADD COLUMN locRotRad REAL")
            db.execSQL("ALTER TABLE projects ADD COLUMN locTx REAL")
            db.execSQL("ALTER TABLE projects ADD COLUMN locTy REAL")
            db.execSQL("ALTER TABLE projects ADD COLUMN locPointCount INTEGER")
            db.execSQL("ALTER TABLE projects ADD COLUMN locLastSolvedAt INTEGER")
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS calibration_points (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    projectId INTEGER NOT NULL,
                    srcNorth REAL NOT NULL,
                    srcEast REAL NOT NULL,
                    dstNorth REAL NOT NULL,
                    dstEast REAL NOT NULL,
                    weight REAL NOT NULL DEFAULT 1.0,
                    include INTEGER NOT NULL DEFAULT 1,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )""".trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_calibration_points_projectId ON calibration_points(projectId)")
        }
    }
    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS ntrip_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    profileId INTEGER,
                    startTs INTEGER NOT NULL,
                    endTs INTEGER NOT NULL,
                    rtcmBytes INTEGER NOT NULL,
                    nmeaBytes INTEGER NOT NULL,
                    avgRateBps REAL NOT NULL,
                    maxRateBps REAL NOT NULL,
                    corrections INTEGER NOT NULL,
                    finalFix TEXT,
                    simulated INTEGER NOT NULL
                )""".trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_ntrip_sessions_profileId ON ntrip_sessions(profileId)")
        }
    }
    val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS cad_layers (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    projectId INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    colorIndex INTEGER,
                    visible INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )""".trimIndent()
            )
            // UNIQUE index (önceden unique olmayan oluşturulmuş olabilir; 21->22 migration bunu düzeltecek)
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_cad_layers_projectId_name ON cad_layers(projectId,name)")
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS cad_entities (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    projectId INTEGER NOT NULL,
                    layerId INTEGER NOT NULL,
                    type TEXT NOT NULL,
                    dataEncoded TEXT NOT NULL,
                    colorIndex INTEGER,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )""".trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_cad_entities_projectId ON cad_entities(projectId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_cad_entities_layerId ON cad_entities(layerId)")
        }
    }
    val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS survey_ranges (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    projectId INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    pointCount INTEGER NOT NULL DEFAULT 0,
                    area REAL,
                    perimeter REAL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )""".trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_survey_ranges_projectId ON survey_ranges(projectId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_survey_ranges_projectId_name ON survey_ranges(projectId,name)")
        }
    }
    val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS gis_features (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    projectId INTEGER NOT NULL,
                    type TEXT NOT NULL,
                    attr TEXT,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )""".trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_gis_features_projectId ON gis_features(projectId)")
        }
    }
    val MIGRATION_18_19 = object : Migration(18, 19) { // Şema değişikliği yok; sadece hash uyuşmazlığını düzgün sürüm zincirine oturtmak için placeholder
        override fun migrate(db: SupportSQLiteDatabase) { /* No-op */ }
    }
    val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // ntrip_sessions tablosundaki eksik index'i ekle (hata mesajından görülen sorun)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_ntrip_sessions_profileId ON ntrip_sessions(profileId)")
        }
    }
    val MIGRATION_20_21 = object : Migration(20, 21) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // No-op
        }
    }
    val MIGRATION_21_22 = object : Migration(21, 22) { // Schema repair: cad_layers unique index & ntrip_sessions index tutarlılığı
        override fun migrate(db: SupportSQLiteDatabase) {
            runCatching { db.execSQL("DROP INDEX IF EXISTS index_cad_layers_projectId_name") }
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_cad_layers_projectId_name ON cad_layers(projectId,name)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_ntrip_sessions_profileId ON ntrip_sessions(profileId)")
        }
    }
    val MIGRATION_22_23 = object : Migration(22, 23) { // Zorunlu onarım: tam tablo recreate ile hash farklarını gider
        override fun migrate(db: SupportSQLiteDatabase) {
            // cad_layers tablo onarımı
            try {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS cad_layers_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        projectId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        colorIndex INTEGER,
                        visible INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )""".trimIndent()
                )
                db.execSQL("INSERT INTO cad_layers_new (id,projectId,name,colorIndex,visible,createdAt,updatedAt) SELECT id,projectId,name,colorIndex,visible,createdAt,updatedAt FROM cad_layers")
                db.execSQL("DROP TABLE cad_layers")
                db.execSQL("ALTER TABLE cad_layers_new RENAME TO cad_layers")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_cad_layers_projectId_name ON cad_layers(projectId,name)")
            } catch (_: Exception) { /* tablo yoksa geç */ }

            // ntrip_sessions tablo onarımı (index/düzen farklarını normalize et)
            try {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS ntrip_sessions_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        profileId INTEGER,
                        startTs INTEGER NOT NULL,
                        endTs INTEGER NOT NULL,
                        rtcmBytes INTEGER NOT NULL,
                        nmeaBytes INTEGER NOT NULL,
                        avgRateBps REAL NOT NULL,
                        maxRateBps REAL NOT NULL,
                        corrections INTEGER NOT NULL,
                        finalFix TEXT,
                        simulated INTEGER NOT NULL
                    )""".trimIndent()
                )
                // Eski tablo varsa verileri kopyala
                runCatching {
                    db.execSQL("INSERT INTO ntrip_sessions_new (id,profileId,startTs,endTs,rtcmBytes,nmeaBytes,avgRateBps,maxRateBps,corrections,finalFix,simulated) SELECT id,profileId,startTs,endTs,rtcmBytes,nmeaBytes,avgRateBps,maxRateBps,corrections,finalFix,simulated FROM ntrip_sessions")
                    db.execSQL("DROP TABLE ntrip_sessions")
                    db.execSQL("ALTER TABLE ntrip_sessions_new RENAME TO ntrip_sessions")
                }.onFailure { _ ->
                    // Eski tablo yoksa yeni tablo zaten hazır
                }
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ntrip_sessions_profileId ON ntrip_sessions(profileId)")
            } catch (_: Exception) { }
        }
    }
    // Yeni: v23 -> v24 ek şema onarım ve indeks normalizasyonu (önceki cihazlarda kalmış hatalı index kombinasyonlarını temizler)
    val MIGRATION_23_24 = object : Migration(23, 24) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // ntrip_sessions için beklenen tablo yapısını zorla (index zaten varsa sorun yok)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_ntrip_sessions_profileId ON ntrip_sessions(profileId)")

            // cad_layers: non-unique index kalıntısı varsa kaldırıp unique oluştur
            runCatching { db.execSQL("DROP INDEX IF EXISTS index_cad_layers_projectId_name") }
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_cad_layers_projectId_name ON cad_layers(projectId,name)")

            // gis_features: eski tek kolon index kaldır, composite index oluştur
            runCatching { db.execSQL("DROP INDEX IF EXISTS index_gis_features_projectId") }
            db.execSQL("CREATE INDEX IF NOT EXISTS index_gis_features_projectId_id ON gis_features(projectId,id)")
        }
    }
    val MIGRATION_24_25 = object : Migration(24, 25) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // projects tablosuna yeni projeksiyon alanlarını ekle (idempotent)
            fun add(column: String, type: String) = runCatching { db.execSQL("ALTER TABLE projects ADD COLUMN $column $type") }
            add("projectionType", "TEXT")
            add("projCentralMeridianDeg", "REAL")
            add("projFalseNorthing", "REAL")
            add("projFalseEasting", "REAL")
            add("projScaleFactor", "REAL")
            add("projLatOrigin", "REAL")
            add("projStdParallel1", "REAL")
            add("projStdParallel2", "REAL")
        }
    }

    // v25 -> v26 : Index / şema tutarlılık onarımı (cad_layers unique index, gis_features composite index)
    val MIGRATION_25_26 = object : Migration(25, 26) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // cad_layers: eski non-unique index kalıntısını kaldırıp yeniden unique oluştur
            runCatching { db.execSQL("DROP INDEX IF EXISTS index_cad_layers_projectId_name") }
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_cad_layers_projectId_name ON cad_layers(projectId,name)")

            // Her ihtimale karşı tabloyu gerçekten unique hale getiremeyen cihazlar için zorlayıcı yeniden kurulum (son çare)
            // Unique ihlali yoksa veri korunur; varsa duplicate satırlar tespit edilip ilk kaydı bırakıp diğerlerini siliyoruz.
            runCatching {
                // Duplicate isimleri tespit et ve fazlalıkları sil
                db.execSQL(
                    """DELETE FROM cad_layers WHERE id IN (
                        SELECT c2.id FROM cad_layers c1
                        JOIN cad_layers c2 ON c1.projectId = c2.projectId AND c1.name = c2.name AND c1.id < c2.id
                    )""".trimIndent()
                )
            }
            // Tekrar index'i oluştur (garanti)
            runCatching { db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_cad_layers_projectId_name ON cad_layers(projectId,name)") }

            // gis_features: eski tek kolon index'i kaldır, composite (projectId,id) index oluştur
            runCatching { db.execSQL("DROP INDEX IF EXISTS index_gis_features_projectId") }
            db.execSQL("CREATE INDEX IF NOT EXISTS index_gis_features_projectId_id ON gis_features(projectId,id)")
        }
    }
    val MIGRATION_26_27 = object : Migration(26, 27) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS measurement_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    projectId INTEGER,
                    mode TEXT,
                    eventType TEXT NOT NULL,
                    message TEXT,
                    createdAt INTEGER NOT NULL,
                    extra TEXT
                )""".trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_measurement_logs_projectId ON measurement_logs(projectId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_measurement_logs_createdAt ON measurement_logs(createdAt)")
        }
    }
    // v27 -> v28 : measurement_logs tablo index normalizasyonu (önce indeksleri olmayan cihazlar için)
    val MIGRATION_27_28 = object : Migration(27, 28) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS index_measurement_logs_createdAt ON measurement_logs(createdAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_measurement_logs_projectId ON measurement_logs(projectId)")
        }
    }

    // v28 -> v29 : measurement_logs tabloyu indeksleri kaldırarak yeniden oluştur (Expected indices=[] hatasını düzeltmek için)
    val MIGRATION_28_29 = object : Migration(28, 29) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Eğer tablo yoksa (taze kurulum) sadece oluştur.
            val colsCursor = db.query("PRAGMA table_info(measurement_logs)")
            val tableExists = colsCursor.use { it.count > 0 }
            if (!tableExists) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS measurement_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        projectId INTEGER,
                        mode TEXT,
                        eventType TEXT NOT NULL,
                        message TEXT,
                        createdAt INTEGER NOT NULL,
                        extra TEXT
                    )""".trimIndent()
                )
                return
            }
            // Geçici tablo oluştur
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS measurement_logs_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    projectId INTEGER,
                    mode TEXT,
                    eventType TEXT NOT NULL,
                    message TEXT,
                    createdAt INTEGER NOT NULL,
                    extra TEXT
                )""".trimIndent()
            )
            // Verileri kopyala (kolon adları birebir)
            runCatching {
                db.execSQL("INSERT INTO measurement_logs_new (id,projectId,mode,eventType,message,createdAt,extra) SELECT id,projectId,mode,eventType,message,createdAt,extra FROM measurement_logs")
            }
            // Eski tabloyu bırak ve yenisini yeniden adlandır
            db.execSQL("DROP TABLE measurement_logs")
            db.execSQL("ALTER TABLE measurement_logs_new RENAME TO measurement_logs")
            // Eski index kalıntılarını (varsa) temizle (DROP INDEX IF EXISTS güvenli)
            runCatching { db.execSQL("DROP INDEX IF EXISTS index_measurement_logs_createdAt") }
            runCatching { db.execSQL("DROP INDEX IF EXISTS index_measurement_logs_projectId") }
        }
    }
    // 29 -> 30 : measurement_logs entity'den indeksler kaldırıldı; tablo zaten indeksiz olmalı.
    // Eski bazı cihazlarda indeksler kalmışsa güvenli şekilde düşür.
    val MIGRATION_29_30 = object : Migration(29, 30) {
        override fun migrate(db: SupportSQLiteDatabase) {
            runCatching { db.execSQL("DROP INDEX IF EXISTS index_measurement_logs_createdAt") }
            runCatching { db.execSQL("DROP INDEX IF EXISTS index_measurement_logs_projectId") }
            // Tabloyu yeniden yaratmaya gerek yok; kolon yapısı değişmedi.
        }
    }
    // 30 -> 31 : gis_features yeni kolonlar (layer, geometryJson)
    val MIGRATION_30_31 = object : Migration(30, 31) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Kolonlar nullable olduğu için direkt eklemek güvenli (varsa hata yutuluyor)
            runCatching { db.execSQL("ALTER TABLE gis_features ADD COLUMN layer TEXT") }
            runCatching { db.execSQL("ALTER TABLE gis_features ADD COLUMN geometryJson TEXT") }
        }
    }

    // 31 -> 32 : gis_features schema uyuşmazlığını düzeltmek için güvenli onarım
    val MIGRATION_31_32 = object : Migration(31, 32) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Kolonlar bazı cihazlarda zaten eklenmiş olabilir; güvenli ekle
            runCatching { db.execSQL("ALTER TABLE gis_features ADD COLUMN geometryJson TEXT") }
            runCatching { db.execSQL("ALTER TABLE gis_features ADD COLUMN layer TEXT") }
            // Beklenen composite index'i garanti altına al
            db.execSQL("CREATE INDEX IF NOT EXISTS index_gis_features_projectId_id ON gis_features(projectId,id)")
        }
    }

    // 32 -> 33 : line_definitions tablosu eklendi
    val MIGRATION_32_33 = object : Migration(32, 33) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS line_definitions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    projectId INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    startPointId INTEGER NOT NULL,
                    endPointId INTEGER NOT NULL,
                    startKm REAL NOT NULL DEFAULT 0.0,
                    length REAL NOT NULL DEFAULT 0.0,
                    azimuth REAL NOT NULL DEFAULT 0.0,
                    useInterval INTEGER NOT NULL DEFAULT 0,
                    intervalMeters REAL NOT NULL DEFAULT 5.0,
                    slopePercent REAL NOT NULL DEFAULT 0.0,
                    autoNearestPoint INTEGER NOT NULL DEFAULT 0,
                    calculationMode TEXT NOT NULL DEFAULT 'FIXED_DISTANCE',
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE
                )""".trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_line_definitions_projectId ON line_definitions(projectId)")
        }
    }

    // v33 -> v34: cad_layers tablosuna locked ve isWorkLayer alanları eklendi
    val MIGRATION_33_34 = object : Migration(33, 34) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE cad_layers ADD COLUMN locked INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE cad_layers ADD COLUMN isWorkLayer INTEGER NOT NULL DEFAULT 0")
        }
    }

    val ALL = arrayOf(
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_9_10,
        MIGRATION_10_11,
        MIGRATION_11_12,
        MIGRATION_12_13,
        MIGRATION_13_14,
        MIGRATION_14_15,
        MIGRATION_15_16,
        MIGRATION_16_17,
        MIGRATION_17_18,
        MIGRATION_18_19,
        MIGRATION_19_20,
        MIGRATION_20_21,
        MIGRATION_21_22,
        MIGRATION_22_23,
        MIGRATION_23_24,
        MIGRATION_24_25,
        MIGRATION_25_26,
        MIGRATION_26_27,
        MIGRATION_27_28,
        MIGRATION_28_29,
        MIGRATION_29_30,
        MIGRATION_30_31,
        MIGRATION_31_32,
        MIGRATION_32_33,
        MIGRATION_33_34 // yeni - Layer Settings için locked ve isWorkLayer alanları
    )
}
