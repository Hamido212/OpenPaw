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
public final class WhatsAppTool_Factory implements Factory<WhatsAppTool> {
  private final Provider<Context> contextProvider;

  public WhatsAppTool_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public WhatsAppTool get() {
    return newInstance(contextProvider.get());
  }

  public static WhatsAppTool_Factory create(Provider<Context> contextProvider) {
    return new WhatsAppTool_Factory(contextProvider);
  }

  public static WhatsAppTool newInstance(Context context) {
    return new WhatsAppTool(context);
  }
}
