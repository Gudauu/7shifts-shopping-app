package com.sevenshifts.shopping.di

import com.sevenshifts.shopping.data.CatalogRepositoryImpl
import com.sevenshifts.shopping.domain.CatalogRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {
    @Binds
    @Singleton
    fun bindCatalogRepository(impl: CatalogRepositoryImpl): CatalogRepository
}
