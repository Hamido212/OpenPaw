package com.openpaw.app.domain.tools;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class FileManagerTool_Factory implements Factory<FileManagerTool> {
  private final Provider<Context> contextProvider;

  public FileManagerTool_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public FileManagerTool get() {
    return newInstance(contextProvider.get());
  }

  public static FileManagerTool_Factory create(Provider<Context> contextProvider) {
    return new FileManagerTool_Factory(contextProvider);
  }

  public static FileManagerTool newInstance(Context context) {
    return new FileManagerTool(context);
  }
}
