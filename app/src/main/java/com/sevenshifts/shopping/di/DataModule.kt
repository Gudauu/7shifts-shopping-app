package com.sevenshifts.shopping.di

import com.sevenshifts.shopping.data.CatalogRepositoryImpl
import com.sevenshifts.shopping.data.StubPurchaseRepository
import com.sevenshifts.shopping.domain.CatalogRepository
import com.sevenshifts.shopping.domain.PurchaseRepository
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

    @Binds
    @Singleton
    fun bindPurchaseRepository(impl: StubPurchaseRepository): PurchaseRepository
}
