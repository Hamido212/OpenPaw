package com.openpaw.app.service;

import com.openpaw.app.domain.usecase.AgentUseCase;
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

  private final Provider<AgentUseCase> agentUseCaseProvider;

  public FloatingBubbleService_MembersInjector(
      Provider<VoiceInputManager> voiceInputManagerProvider,
      Provider<AgentUseCase> agentUseCaseProvider) {
    this.voiceInputManagerProvider = voiceInputManagerProvider;
    this.agentUseCaseProvider = agentUseCaseProvider;
  }

  public static MembersInjector<FloatingBubbleService> create(
      Provider<VoiceInputManager> voiceInputManagerProvider,
      Provider<AgentUseCase> agentUseCaseProvider) {
    return new FloatingBubbleService_MembersInjector(voiceInputManagerProvider, agentUseCaseProvider);
  }

  @Override
  public void injectMembers(FloatingBubbleService instance) {
    injectVoiceInputManager(instance, voiceInputManagerProvider.get());
    injectAgentUseCase(instance, agentUseCaseProvider.get());
  }

  @InjectedFieldSignature("com.openpaw.app.service.FloatingBubbleService.voiceInputManager")
  public static void injectVoiceInputManager(FloatingBubbleService instance,
      VoiceInputManager voiceInputManager) {
    instance.voiceInputManager = voiceInputManager;
  }

  @InjectedFieldSignature("com.openpaw.app.service.FloatingBubbleService.agentUseCase")
  public static void injectAgentUseCase(FloatingBubbleService instance, AgentUseCase agentUseCase) {
    instance.agentUseCase = agentUseCase;
  }
}
