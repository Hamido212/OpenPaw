package com.openpaw.app.domain.tools;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class ScreenTool_Factory implements Factory<ScreenTool> {
  @Override
  public ScreenTool get() {
    return newInstance();
  }

  public static ScreenTool_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ScreenTool newInstance() {
    return new ScreenTool();
  }

  private static final class InstanceHolder {
    private static final ScreenTool_Factory INSTANCE = new ScreenTool_Factory();
  }
}
