package com.openpaw.app.domain.usecase;

import com.openpaw.app.data.local.MessageDao;
import com.openpaw.app.data.remote.LlmProvider;
import com.openpaw.app.data.repository.MemoryRepository;
import com.openpaw.app.data.repository.SettingsRepository;
import com.openpaw.app.domain.tools.ToolRegistry;
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
public final class AgentUseCase_Factory implements Factory<AgentUseCase> {
  private final Provider<LlmProvider> llmProvider;

  private final Provider<ToolRegistry> toolRegistryProvider;

  private final Provider<MessageDao> messageDaoProvider;

  private final Provider<MemoryRepository> memoryRepositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  public AgentUseCase_Factory(Provider<LlmProvider> llmProvider,
      Provider<ToolRegistry> toolRegistryProvider, Provider<MessageDao> messageDaoProvider,
      Provider<MemoryRepository> memoryRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.llmProvider = llmProvider;
    this.toolRegistryProvider = toolRegistryProvider;
    this.messageDaoProvider = messageDaoProvider;
    this.memoryRepositoryProvider = memoryRepositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public AgentUseCase get() {
    return newInstance(llmProvider.get(), toolRegistryProvider.get(), messageDaoProvider.get(), memoryRepositoryProvider.get(), settingsRepositoryProvider.get());
  }

  public static AgentUseCase_Factory create(Provider<LlmProvider> llmProvider,
      Provider<ToolRegistry> toolRegistryProvider, Provider<MessageDao> messageDaoProvider,
      Provider<MemoryRepository> memoryRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new AgentUseCase_Factory(llmProvider, toolRegistryProvider, messageDaoProvider, memoryRepositoryProvider, settingsRepositoryProvider);
  }

  public static AgentUseCase newInstance(LlmProvider llmProvider, ToolRegistry toolRegistry,
      MessageDao messageDao, MemoryRepository memoryRepository,
      SettingsRepository settingsRepository) {
    return new AgentUseCase(llmProvider, toolRegistry, messageDao, memoryRepository, settingsRepository);
  }
}
