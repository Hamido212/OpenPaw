package com.openpaw.app.presentation.onboarding;

import com.openpaw.app.data.repository.SettingsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class OnboardingViewModel_Factory implements Factory<OnboardingViewModel> {
  private final Provider<SettingsRepository> settingsRepositoryProvider;

  public OnboardingViewModel_Factory(Provider<SettingsRepository> settingsRepositoryProvider) {
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public OnboardingViewModel get() {
    return newInstance(settingsRepositoryProvider.get());
  }

  public static OnboardingViewModel_Factory create(
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new OnboardingViewModel_Factory(settingsRepositoryProvider);
  }

  public static OnboardingViewModel newInstance(SettingsRepository settingsRepository) {
    return new OnboardingViewModel(settingsRepository);
  }
}
