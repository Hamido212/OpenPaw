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
public final class AlarmTool_Factory implements Factory<AlarmTool> {
  private final Provider<Context> contextProvider;

  public AlarmTool_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AlarmTool get() {
    return newInstance(contextProvider.get());
  }

  public static AlarmTool_Factory create(Provider<Context> contextProvider) {
    return new AlarmTool_Factory(contextProvider);
  }

  public static AlarmTool newInstance(Context context) {
    return new AlarmTool(context);
  }
}
