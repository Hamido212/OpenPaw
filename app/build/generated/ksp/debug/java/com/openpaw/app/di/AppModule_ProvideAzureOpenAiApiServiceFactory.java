package com.openpaw.app.di;

import com.google.gson.Gson;
import com.openpaw.app.data.remote.AzureOpenAiApiService;
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
public final class AppModule_ProvideAzureOpenAiApiServiceFactory implements Factory<AzureOpenAiApiService> {
  private final Provider<OkHttpClient> clientProvider;

  private final Provider<Gson> gsonProvider;

  public AppModule_ProvideAzureOpenAiApiServiceFactory(Provider<OkHttpClient> clientProvider,
      Provider<Gson> gsonProvider) {
    this.clientProvider = clientProvider;
    this.gsonProvider = gsonProvider;
  }

  @Override
  public AzureOpenAiApiService get() {
    return provideAzureOpenAiApiService(clientProvider.get(), gsonProvider.get());
  }

  public static AppModule_ProvideAzureOpenAiApiServiceFactory create(
      Provider<OkHttpClient> clientProvider, Provider<Gson> gsonProvider) {
    return new AppModule_ProvideAzureOpenAiApiServiceFactory(clientProvider, gsonProvider);
  }

  public static AzureOpenAiApiService provideAzureOpenAiApiService(OkHttpClient client, Gson gson) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideAzureOpenAiApiService(client, gson));
  }
}
