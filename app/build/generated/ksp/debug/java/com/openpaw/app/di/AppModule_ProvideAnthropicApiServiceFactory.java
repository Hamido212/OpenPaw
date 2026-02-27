package com.openpaw.app.di;

import com.google.gson.Gson;
import com.openpaw.app.data.remote.AnthropicApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

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
public final class AppModule_ProvideAnthropicApiServiceFactory implements Factory<AnthropicApiService> {
  private final Provider<OkHttpClient> clientProvider;

  private final Provider<Gson> gsonProvider;

  public AppModule_ProvideAnthropicApiServiceFactory(Provider<OkHttpClient> clientProvider,
      Provider<Gson> gsonProvider) {
    this.clientProvider = clientProvider;
    this.gsonProvider = gsonProvider;
  }

  @Override
  public AnthropicApiService get() {
    return provideAnthropicApiService(clientProvider.get(), gsonProvider.get());
  }

  public static AppModule_ProvideAnthropicApiServiceFactory create(
      Provider<OkHttpClient> clientProvider, Provider<Gson> gsonProvider) {
    return new AppModule_ProvideAnthropicApiServiceFactory(clientProvider, gsonProvider);
  }

  public static AnthropicApiService provideAnthropicApiService(OkHttpClient client, Gson gson) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideAnthropicApiService(client, gson));
  }
}
