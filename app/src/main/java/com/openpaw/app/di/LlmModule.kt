package com.openpaw.app.di

import com.openpaw.app.data.remote.DelegatingLlmProvider
import com.openpaw.app.data.remote.LlmProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds [DelegatingLlmProvider] as the active [LlmProvider].
 *
 * DelegatingLlmProvider reads the user's selection from DataStore on every call
 * and routes to: AnthropicLlmProvider | AzureOpenAiLlmProvider | LocalLlmProvider.
 *
 * Switching providers in the Settings screen takes effect immediately, no restart needed.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LlmModule {

    @Binds
    @Singleton
    abstract fun bindLlmProvider(impl: DelegatingLlmProvider): LlmProvider
}
