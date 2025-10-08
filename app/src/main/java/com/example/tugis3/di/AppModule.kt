package com.example.tugis3.di

import android.content.Context
import android.location.LocationManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.FusedLocationProviderClient
import com.example.tugis3.gnss.GnssEngine
import com.example.tugis3.ntrip.NtripClient
import com.example.tugis3.ntrip.NtripClientImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Data katmanı provider'ları core:data içindeki DataModule'a taşındı.
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideLocationManager(@ApplicationContext ctx: Context): LocationManager =
        ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @Provides
    @Singleton
    fun provideGnssEngine(lm: LocationManager) = GnssEngine(lm)

    @Provides
    @Singleton
    fun provideNtripClient(): NtripClient = NtripClientImpl()

    // Google Play Services konum istemcisi
    @Provides
    @Singleton
    fun provideFusedLocationProvider(@ApplicationContext ctx: Context): FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(ctx)
}
