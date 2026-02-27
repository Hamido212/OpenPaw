package com.openpaw.app.di

import android.content.Context
import androidx.room.Room
import com.openpaw.app.data.local.AppDatabase
import com.openpaw.app.data.local.MemoryDao
import com.openpaw.app.data.local.MessageDao
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.openpaw.app.data.remote.AnthropicApiService
import com.openpaw.app.data.remote.AzureOpenAiApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "openpaw.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()

    @Provides
    fun provideMemoryDao(db: AppDatabase): MemoryDao = db.memoryDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    fun provideAnthropicApiService(client: OkHttpClient, gson: Gson): AnthropicApiService =
        Retrofit.Builder()
            .baseUrl("https://api.anthropic.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(AnthropicApiService::class.java)

    /**
     * Azure OpenAI Service – uses @Url so the base URL is a placeholder.
     * The real URL (with resource name + deployment) is passed at call time.
     */
    @Provides
    @Singleton
    fun provideAzureOpenAiApiService(client: OkHttpClient, gson: Gson): AzureOpenAiApiService =
        Retrofit.Builder()
            .baseUrl("https://placeholder.azure.openai.com/")   // overridden by @Url
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(AzureOpenAiApiService::class.java)
}
