package com.openpaw.app.presentation.chat;

import com.openpaw.app.data.local.MessageDao;
import com.openpaw.app.data.repository.SettingsRepository;
import com.openpaw.app.domain.usecase.AgentUseCase;
import com.openpaw.app.presentation.voice.VoiceInputManager;
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
public final class ChatViewModel_Factory implements Factory<ChatViewModel> {
  private final Provider<AgentUseCase> agentUseCaseProvider;

  private final Provider<MessageDao> messageDaoProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<VoiceInputManager> voiceInputManagerProvider;

  public ChatViewModel_Factory(Provider<AgentUseCase> agentUseCaseProvider,
      Provider<MessageDao> messageDaoProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<VoiceInputManager> voiceInputManagerProvider) {
    this.agentUseCaseProvider = agentUseCaseProvider;
    this.messageDaoProvider = messageDaoProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.voiceInputManagerProvider = voiceInputManagerProvider;
  }

  @Override
  public ChatViewModel get() {
    return newInstance(agentUseCaseProvider.get(), messageDaoProvider.get(), settingsRepositoryProvider.get(), voiceInputManagerProvider.get());
  }

  public static ChatViewModel_Factory create(Provider<AgentUseCase> agentUseCaseProvider,
      Provider<MessageDao> messageDaoProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<VoiceInputManager> voiceInputManagerProvider) {
    return new ChatViewModel_Factory(agentUseCaseProvider, messageDaoProvider, settingsRepositoryProvider, voiceInputManagerProvider);
  }

  public static ChatViewModel newInstance(AgentUseCase agentUseCase, MessageDao messageDao,
      SettingsRepository settingsRepository, VoiceInputManager voiceInputManager) {
    return new ChatViewModel(agentUseCase, messageDao, settingsRepository, voiceInputManager);
  }
}
