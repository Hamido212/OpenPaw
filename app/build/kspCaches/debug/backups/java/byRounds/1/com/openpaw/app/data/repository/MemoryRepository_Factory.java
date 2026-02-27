package com.openpaw.app.data.repository;

import com.openpaw.app.data.local.MemoryDao;
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
public final class MemoryRepository_Factory implements Factory<MemoryRepository> {
  private final Provider<MemoryDao> memoryDaoProvider;

  public MemoryRepository_Factory(Provider<MemoryDao> memoryDaoProvider) {
    this.memoryDaoProvider = memoryDaoProvider;
  }

  @Override
  public MemoryRepository get() {
    return newInstance(memoryDaoProvider.get());
  }

  public static MemoryRepository_Factory create(Provider<MemoryDao> memoryDaoProvider) {
    return new MemoryRepository_Factory(memoryDaoProvider);
  }

  public static MemoryRepository newInstance(MemoryDao memoryDao) {
    return new MemoryRepository(memoryDao);
  }
}
