package com.openpaw.app.data.remote;

import com.google.gson.Gson;
import com.openpaw.app.data.repository.SettingsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class AzureOpenAiLlmProvider_Factory implements Factory<AzureOpenAiLlmProvider> {
  private final Provider<AzureOpenAiApiService> apiServiceProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<Gson> gsonProvider;

  public AzureOpenAiLlmProvider_Factory(Provider<AzureOpenAiApiService> apiServiceProvider,
      Provider<SettingsRepository> settingsRepositoryProvider, Provider<Gson> gsonProvider) {
    this.apiServiceProvider = apiServiceProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.gsonProvider = gsonProvider;
  }

  @Override
  public AzureOpenAiLlmProvider get() {
    return newInstance(apiServiceProvider.get(), settingsRepositoryProvider.get(), gsonProvider.get());
  }

  public static AzureOpenAiLlmProvider_Factory create(
      Provider<AzureOpenAiApiService> apiServiceProvider,
      Provider<SettingsRepository> settingsRepositoryProvider, Provider<Gson> gsonProvider) {
    return new AzureOpenAiLlmProvider_Factory(apiServiceProvider, settingsRepositoryProvider, gsonProvider);
  }

  public static AzureOpenAiLlmProvider newInstance(AzureOpenAiApiService apiService,
      SettingsRepository settingsRepository, Gson gson) {
    return new AzureOpenAiLlmProvider(apiService, settingsRepository, gson);
  }
}
