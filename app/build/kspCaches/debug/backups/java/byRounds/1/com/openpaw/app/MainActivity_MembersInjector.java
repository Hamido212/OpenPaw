package com.openpaw.app;

import com.openpaw.app.presentation.voice.VoiceInputManager;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<VoiceInputManager> voiceInputManagerProvider;

  public MainActivity_MembersInjector(Provider<VoiceInputManager> voiceInputManagerProvider) {
    this.voiceInputManagerProvider = voiceInputManagerProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<VoiceInputManager> voiceInputManagerProvider) {
    return new MainActivity_MembersInjector(voiceInputManagerProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectVoiceInputManager(instance, voiceInputManagerProvider.get());
  }

  @InjectedFieldSignature("com.openpaw.app.MainActivity.voiceInputManager")
  public static void injectVoiceInputManager(MainActivity instance,
      VoiceInputManager voiceInputManager) {
    instance.voiceInputManager = voiceInputManager;
  }
}
