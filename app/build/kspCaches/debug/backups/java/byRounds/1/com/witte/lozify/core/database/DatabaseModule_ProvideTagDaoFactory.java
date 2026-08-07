package com.witte.lozify.core.database;

import com.witte.lozify.data.local.dao.TagDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
    "cast"
})
public final class DatabaseModule_ProvideTagDaoFactory implements Factory<TagDao> {
  private final Provider<LozifyDatabase> databaseProvider;

  public DatabaseModule_ProvideTagDaoFactory(Provider<LozifyDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public TagDao get() {
    return provideTagDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideTagDaoFactory create(
      Provider<LozifyDatabase> databaseProvider) {
    return new DatabaseModule_ProvideTagDaoFactory(databaseProvider);
  }

  public static TagDao provideTagDao(LozifyDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideTagDao(database));
  }
}
