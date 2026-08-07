package com.witte.lozify.core.database;

import com.witte.lozify.data.local.dao.NoteRelationDao;
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
public final class DatabaseModule_ProvideNoteRelationDaoFactory implements Factory<NoteRelationDao> {
  private final Provider<LozifyDatabase> databaseProvider;

  public DatabaseModule_ProvideNoteRelationDaoFactory(Provider<LozifyDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public NoteRelationDao get() {
    return provideNoteRelationDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideNoteRelationDaoFactory create(
      Provider<LozifyDatabase> databaseProvider) {
    return new DatabaseModule_ProvideNoteRelationDaoFactory(databaseProvider);
  }

  public static NoteRelationDao provideNoteRelationDao(LozifyDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideNoteRelationDao(database));
  }
}
