package com.witte.lozify.presentation.editor;

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
public final class EditorViewModel_Factory implements Factory<EditorViewModel> {
  private final Provider<NoteRepository> noteRepositoryProvider;

  public EditorViewModel_Factory(Provider<NoteRepository> noteRepositoryProvider) {
    this.noteRepositoryProvider = noteRepositoryProvider;
  }

  @Override
  public EditorViewModel get() {
    return newInstance(noteRepositoryProvider.get());
  }

  public static EditorViewModel_Factory create(Provider<NoteRepository> noteRepositoryProvider) {
    return new EditorViewModel_Factory(noteRepositoryProvider);
  }

  public static EditorViewModel newInstance(NoteRepository noteRepository) {
    return new EditorViewModel(noteRepository);
  }
}
