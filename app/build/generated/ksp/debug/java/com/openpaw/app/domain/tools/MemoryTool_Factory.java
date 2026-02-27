package com.openpaw.app.domain.tools;

import com.openpaw.app.data.repository.MemoryRepository;
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
public final class MemoryTool_Factory implements Factory<MemoryTool> {
  private final Provider<MemoryRepository> memoryRepositoryProvider;

  public MemoryTool_Factory(Provider<MemoryRepository> memoryRepositoryProvider) {
    this.memoryRepositoryProvider = memoryRepositoryProvider;
  }

  @Override
  public MemoryTool get() {
    return newInstance(memoryRepositoryProvider.get());
  }

  public static MemoryTool_Factory create(Provider<MemoryRepository> memoryRepositoryProvider) {
    return new MemoryTool_Factory(memoryRepositoryProvider);
  }

  public static MemoryTool newInstance(MemoryRepository memoryRepository) {
    return new MemoryTool(memoryRepository);
  }
}
