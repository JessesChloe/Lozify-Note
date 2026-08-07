package com.witte.lozify.presentation.home;

import com.witte.lozify.domain.repository.NoteRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<NoteRepository> noteRepositoryProvider;

  public HomeViewModel_Factory(Provider<NoteRepository> noteRepositoryProvider) {
    this.noteRepositoryProvider = noteRepositoryProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(noteRepositoryProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<NoteRepository> noteRepositoryProvider) {
    return new HomeViewModel_Factory(noteRepositoryProvider);
  }

  public static HomeViewModel newInstance(NoteRepository noteRepository) {
    return new HomeViewModel(noteRepository);
  }
}
