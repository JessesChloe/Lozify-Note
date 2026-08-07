package com.witte.lozify.core.database;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
    "cast"
})
public final class DatabaseModule_ProvideLozifyDatabaseFactory implements Factory<LozifyDatabase> {
  private final Provider<Context> contextProvider;

  public DatabaseModule_ProvideLozifyDatabaseFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public LozifyDatabase get() {
    return provideLozifyDatabase(contextProvider.get());
  }

  public static DatabaseModule_ProvideLozifyDatabaseFactory create(
      Provider<Context> contextProvider) {
    return new DatabaseModule_ProvideLozifyDatabaseFactory(contextProvider);
  }

  public static LozifyDatabase provideLozifyDatabase(Context context) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideLozifyDatabase(context));
  }
}
