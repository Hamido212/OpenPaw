package com.openpaw.app.data.remote;

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
public final class AnthropicLlmProvider_Factory implements Factory<AnthropicLlmProvider> {
  private final Provider<AnthropicApiService> apiServiceProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  public AnthropicLlmProvider_Factory(Provider<AnthropicApiService> apiServiceProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.apiServiceProvider = apiServiceProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public AnthropicLlmProvider get() {
    return newInstance(apiServiceProvider.get(), settingsRepositoryProvider.get());
  }

  public static AnthropicLlmProvider_Factory create(
      Provider<AnthropicApiService> apiServiceProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new AnthropicLlmProvider_Factory(apiServiceProvider, settingsRepositoryProvider);
  }

  public static AnthropicLlmProvider newInstance(AnthropicApiService apiService,
      SettingsRepository settingsRepository) {
    return new AnthropicLlmProvider(apiService, settingsRepository);
  }
}
