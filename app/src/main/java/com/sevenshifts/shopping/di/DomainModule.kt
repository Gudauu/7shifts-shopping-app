package com.sevenshifts.shopping.di

import com.sevenshifts.shopping.domain.Cart
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {
    /** One cart for the whole app, so every screen sees the same contents. */
    @Provides
    @Singleton
    fun provideCart(): Cart = Cart()
}
