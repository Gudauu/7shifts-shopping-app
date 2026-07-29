package com.sevenshifts.shopping.di

import com.sevenshifts.shopping.data.network.ShoppingApi
import com.sevenshifts.shopping.data.network.createShoppingApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideShoppingApi(): ShoppingApi = createShoppingApi(OkHttpClient())
}
