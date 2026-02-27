package com.openpaw.app.presentation.tile;

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
public final class OpenPawQsTile_MembersInjector implements MembersInjector<OpenPawQsTile> {
  private final Provider<VoiceInputManager> voiceInputManagerProvider;

  public OpenPawQsTile_MembersInjector(Provider<VoiceInputManager> voiceInputManagerProvider) {
    this.voiceInputManagerProvider = voiceInputManagerProvider;
  }

  public static MembersInjector<OpenPawQsTile> create(
      Provider<VoiceInputManager> voiceInputManagerProvider) {
    return new OpenPawQsTile_MembersInjector(voiceInputManagerProvider);
  }

  @Override
  public void injectMembers(OpenPawQsTile instance) {
    injectVoiceInputManager(instance, voiceInputManagerProvider.get());
  }

  @InjectedFieldSignature("com.openpaw.app.presentation.tile.OpenPawQsTile.voiceInputManager")
  public static void injectVoiceInputManager(OpenPawQsTile instance,
      VoiceInputManager voiceInputManager) {
    instance.voiceInputManager = voiceInputManager;
  }
}
