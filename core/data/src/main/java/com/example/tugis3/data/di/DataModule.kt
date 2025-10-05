package com.example.tugis3.data.di

import android.content.Context
import androidx.room.Room
import com.example.tugis3.data.db.AppDatabase
import com.example.tugis3.data.db.AppDatabaseMigrations
import com.example.tugis3.data.db.dao.NtripProfileDao
import com.example.tugis3.data.db.dao.PointDao
import com.example.tugis3.data.db.dao.ProjectDao
import com.example.tugis3.data.db.dao.SurveyPointDao
import com.example.tugis3.data.db.dao.DetailFeatureDao
import com.example.tugis3.data.db.dao.CalibrationPointDao
import com.example.tugis3.data.db.dao.NtripSessionDao
import com.example.tugis3.data.db.dao.CadLayerDao
import com.example.tugis3.data.db.dao.CadEntityDao
import com.example.tugis3.data.db.dao.SurveyRangeDao
import com.example.tugis3.data.db.dao.GisFeatureDao
import com.example.tugis3.data.db.dao.MeasurementLogDao
import com.example.tugis3.data.repository.NtripProfileRepository
import com.example.tugis3.data.repository.PointRepository
import com.example.tugis3.data.repository.ProjectRepository
import com.example.tugis3.data.repository.SurveyPointRepository
import com.example.tugis3.data.repository.DetailFeatureRepository
import com.example.tugis3.data.repository.CalibrationPointRepository
import com.example.tugis3.data.repository.NtripSessionRepository
import com.example.tugis3.data.repository.CadPersistenceRepository
import com.example.tugis3.data.repository.SurveyRangeRepository
import com.example.tugis3.data.repository.GisFeatureRepository
import com.example.tugis3.data.repository.MeasurementLogRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.example.tugis3.core.data.BuildConfig

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDb(@ApplicationContext ctx: Context): AppDatabase {
        val builder = Room.databaseBuilder(ctx, AppDatabase::class.java, "tugis3.db")
            .addMigrations(*AppDatabaseMigrations.ALL)
        // DEBUG + isteğe bağlı bayrak (Gradle property -PfallbackOff ile kapatılabilir)
        if (BuildConfig.DEBUG && BuildConfig.ENABLE_DESTRUCTIVE_FALLBACK) {
            builder.fallbackToDestructiveMigration()
        }
        return builder.build()
    }

    // --- DAO Providers ---
    @Provides fun provideProjectDao(db: AppDatabase): ProjectDao = db.projectDao()
    @Provides fun providePointDao(db: AppDatabase): PointDao = db.pointDao()
    @Provides fun provideNtripProfileDao(db: AppDatabase): NtripProfileDao = db.ntripProfileDao()
    @Provides fun provideSurveyPointDao(db: AppDatabase): SurveyPointDao = db.surveyPointDao()
    @Provides fun provideDetailFeatureDao(db: AppDatabase): DetailFeatureDao = db.detailFeatureDao()
    @Provides fun provideCalibrationPointDao(db: AppDatabase): CalibrationPointDao = db.calibrationPointDao()
    @Provides fun provideNtripSessionDao(db: AppDatabase): NtripSessionDao = db.ntripSessionDao()
    @Provides fun provideCadLayerDao(db: AppDatabase): CadLayerDao = db.cadLayerDao()
    @Provides fun provideCadEntityDao(db: AppDatabase): CadEntityDao = db.cadEntityDao()
    @Provides fun provideSurveyRangeDao(db: AppDatabase): SurveyRangeDao = db.surveyRangeDao()
    @Provides fun provideGisFeatureDao(db: AppDatabase): GisFeatureDao = db.gisFeatureDao()
    @Provides fun provideMeasurementLogDao(db: AppDatabase): MeasurementLogDao = db.measurementLogDao()

    // --- Repository Providers ---
    @Provides @Singleton fun provideProjectRepository(dao: ProjectDao): ProjectRepository = ProjectRepository(dao)
    @Provides @Singleton fun providePointRepository(dao: PointDao): PointRepository = PointRepository(dao)
    @Provides @Singleton fun provideNtripProfileRepository(dao: NtripProfileDao): NtripProfileRepository = NtripProfileRepository(dao)
    @Provides @Singleton fun provideSurveyPointRepository(dao: SurveyPointDao): SurveyPointRepository = SurveyPointRepository(dao)
    @Provides @Singleton fun provideDetailFeatureRepository(dao: DetailFeatureDao): DetailFeatureRepository = DetailFeatureRepository(dao)
    @Provides @Singleton fun provideCalibrationPointRepository(dao: CalibrationPointDao, projectDao: ProjectDao): CalibrationPointRepository = CalibrationPointRepository(dao, projectDao)
    @Provides @Singleton fun provideNtripSessionRepository(dao: NtripSessionDao): NtripSessionRepository = NtripSessionRepository(dao)
    @Provides @Singleton fun provideCadPersistenceRepository(layerDao: CadLayerDao, entityDao: CadEntityDao): CadPersistenceRepository = CadPersistenceRepository(layerDao, entityDao)
    @Provides @Singleton fun provideSurveyRangeRepository(dao: SurveyRangeDao): SurveyRangeRepository = SurveyRangeRepository(dao)
    @Provides @Singleton fun provideGisFeatureRepository(dao: GisFeatureDao): GisFeatureRepository = GisFeatureRepository(dao)
    @Provides @Singleton fun provideMeasurementLogRepository(dao: MeasurementLogDao): MeasurementLogRepository = MeasurementLogRepository(dao)
}
