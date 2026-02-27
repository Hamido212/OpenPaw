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
public final class DelegatingLlmProvider_Factory implements Factory<DelegatingLlmProvider> {
  private final Provider<AnthropicLlmProvider> anthropicProvider;

  private final Provider<AzureOpenAiLlmProvider> azureProvider;

  private final Provider<LocalLlmProvider> localProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  public DelegatingLlmProvider_Factory(Provider<AnthropicLlmProvider> anthropicProvider,
      Provider<AzureOpenAiLlmProvider> azureProvider, Provider<LocalLlmProvider> localProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.anthropicProvider = anthropicProvider;
    this.azureProvider = azureProvider;
    this.localProvider = localProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public DelegatingLlmProvider get() {
    return newInstance(anthropicProvider.get(), azureProvider.get(), localProvider.get(), settingsRepositoryProvider.get());
  }

  public static DelegatingLlmProvider_Factory create(
      Provider<AnthropicLlmProvider> anthropicProvider,
      Provider<AzureOpenAiLlmProvider> azureProvider, Provider<LocalLlmProvider> localProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new DelegatingLlmProvider_Factory(anthropicProvider, azureProvider, localProvider, settingsRepositoryProvider);
  }

  public static DelegatingLlmProvider newInstance(AnthropicLlmProvider anthropicProvider,
      AzureOpenAiLlmProvider azureProvider, LocalLlmProvider localProvider,
      SettingsRepository settingsRepository) {
    return new DelegatingLlmProvider(anthropicProvider, azureProvider, localProvider, settingsRepository);
  }
}
