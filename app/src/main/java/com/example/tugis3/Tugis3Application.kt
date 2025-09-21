package com.example.tugis3

import android.app.Application
import androidx.room.Room
import com.example.tugis3.database.AppDatabase
import com.example.tugis3.repository.CorsRepository
import com.example.tugis3.repository.GnssRepository
import com.example.tugis3.repository.ProjectRepository

class Tugis3Application : Application() {
    
    // Database
    lateinit var database: AppDatabase
        private set
    
    // Repositories
    lateinit var corsRepository: CorsRepository
        private set
    
    lateinit var gnssRepository: GnssRepository
        private set
    
    lateinit var projectRepository: ProjectRepository
        private set
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize database
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "tugis3_database"
        ).build()
        
        // Initialize repositories
        corsRepository = CorsRepository(database.corsDao())
        gnssRepository = GnssRepository(database.gnssDataDao())
        projectRepository = ProjectRepository(database.projectDao())
    }
}
