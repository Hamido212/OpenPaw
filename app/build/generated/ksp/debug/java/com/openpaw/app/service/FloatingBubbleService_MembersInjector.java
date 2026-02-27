package com.openpaw.app.service;

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
public final class FloatingBubbleService_MembersInjector implements MembersInjector<FloatingBubbleService> {
  private final Provider<VoiceInputManager> voiceInputManagerProvider;

  public FloatingBubbleService_MembersInjector(
      Provider<VoiceInputManager> voiceInputManagerProvider) {
    this.voiceInputManagerProvider = voiceInputManagerProvider;
  }

  public static MembersInjector<FloatingBubbleService> create(
      Provider<VoiceInputManager> voiceInputManagerProvider) {
    return new FloatingBubbleService_MembersInjector(voiceInputManagerProvider);
  }

  @Override
  public void injectMembers(FloatingBubbleService instance) {
    injectVoiceInputManager(instance, voiceInputManagerProvider.get());
  }

  @InjectedFieldSignature("com.openpaw.app.service.FloatingBubbleService.voiceInputManager")
  public static void injectVoiceInputManager(FloatingBubbleService instance,
      VoiceInputManager voiceInputManager) {
    instance.voiceInputManager = voiceInputManager;
  }
}
