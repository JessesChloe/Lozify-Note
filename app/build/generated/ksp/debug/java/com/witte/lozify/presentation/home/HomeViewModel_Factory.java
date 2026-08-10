package com.witte.lozify.presentation.home;

import com.witte.lozify.domain.repository.NoteRepository;
import com.witte.lozify.domain.repository.TagRepository;
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

  private final Provider<TagRepository> tagRepositoryProvider;

  public HomeViewModel_Factory(Provider<NoteRepository> noteRepositoryProvider,
      Provider<TagRepository> tagRepositoryProvider) {
    this.noteRepositoryProvider = noteRepositoryProvider;
    this.tagRepositoryProvider = tagRepositoryProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(noteRepositoryProvider.get(), tagRepositoryProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<NoteRepository> noteRepositoryProvider,
      Provider<TagRepository> tagRepositoryProvider) {
    return new HomeViewModel_Factory(noteRepositoryProvider, tagRepositoryProvider);
  }

  public static HomeViewModel newInstance(NoteRepository noteRepository,
      TagRepository tagRepository) {
    return new HomeViewModel(noteRepository, tagRepository);
  }
}
