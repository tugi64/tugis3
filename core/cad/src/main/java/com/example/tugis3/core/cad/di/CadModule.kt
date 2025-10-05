package com.example.tugis3.core.cad.di

import com.example.tugis3.core.cad.parse.DxfParser
import com.example.tugis3.core.cad.repository.CadRepository
import com.example.tugis3.core.cad.repository.CadRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CadBindModule {
    @Binds
    @Singleton
    abstract fun bindCadRepository(impl: CadRepositoryImpl): CadRepository
}

@Module
@InstallIn(SingletonComponent::class)
object CadProvideModule {
    @Provides
    @Singleton
    fun provideDxfParser(): DxfParser = DxfParser()
}

