package com.example.tugis3.di

/*
 * DEPRECATED MODULE (DatabaseModule)
 * Bu modül AppModule ile çakışan (duplicate) DAO provider tanımları yüzünden
 * Hilt DuplicateBindings hatasına yol açıyordu. Tüm sağlayıcılar AppModule'de
 * zaten mevcut olduğundan bu modül devre dışı bırakıldı.
 *
 * Migration mantığını ileride entegre etmek isterseniz MIGRATION_3_4 tanımı
 * AppModule.provideDb içerisine eklenebilir.
 */

// Eski içerik referans amaçlı yoruma alınmıştır:
/*
import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.tugis3.data.db.AppDatabase as NewAppDb
import com.example.tugis3.data.db.dao.ProjectDao as NewProjectDao
import com.example.tugis3.data.db.dao.PointDao
import com.example.tugis3.data.db.dao.NtripProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val add = listOf(
            "ALTER TABLE points ADD COLUMN latDeg REAL",
            "ALTER TABLE points ADD COLUMN lonDeg REAL",
            "ALTER TABLE points ADD COLUMN fixType TEXT",
            "ALTER TABLE points ADD COLUMN hrms REAL",
            "ALTER TABLE points ADD COLUMN pdop REAL",
            "ALTER TABLE points ADD COLUMN hdop REAL",
            "ALTER TABLE points ADD COLUMN vdop REAL"
        )
        add.forEach { sql -> try { db.execSQL(sql) } catch (_: Exception) {} }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton fun provideAppDatabase(@ApplicationContext context: Context): NewAppDb =
        Room.databaseBuilder(context, NewAppDb::class.java, "tugis_database")
            .addMigrations(MIGRATION_3_4)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    @Provides fun provideProjectDao(database: NewAppDb): NewProjectDao = database.projectDao()
    @Provides fun providePointDao(database: NewAppDb): PointDao = database.pointDao()
    @Provides fun provideNtripProfileDao(database: NewAppDb): NtripProfileDao = database.ntripProfileDao()
}
*/
