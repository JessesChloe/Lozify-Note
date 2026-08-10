package com.witte.lozify.presentation.editor;

import com.witte.lozify.domain.repository.AttachmentRepository;
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
public final class EditorViewModel_Factory implements Factory<EditorViewModel> {
  private final Provider<NoteRepository> noteRepositoryProvider;

  private final Provider<TagRepository> tagRepositoryProvider;

  private final Provider<AttachmentRepository> attachmentRepositoryProvider;

  public EditorViewModel_Factory(Provider<NoteRepository> noteRepositoryProvider,
      Provider<TagRepository> tagRepositoryProvider,
      Provider<AttachmentRepository> attachmentRepositoryProvider) {
    this.noteRepositoryProvider = noteRepositoryProvider;
    this.tagRepositoryProvider = tagRepositoryProvider;
    this.attachmentRepositoryProvider = attachmentRepositoryProvider;
  }

  @Override
  public EditorViewModel get() {
    return newInstance(noteRepositoryProvider.get(), tagRepositoryProvider.get(), attachmentRepositoryProvider.get());
  }

  public static EditorViewModel_Factory create(Provider<NoteRepository> noteRepositoryProvider,
      Provider<TagRepository> tagRepositoryProvider,
      Provider<AttachmentRepository> attachmentRepositoryProvider) {
    return new EditorViewModel_Factory(noteRepositoryProvider, tagRepositoryProvider, attachmentRepositoryProvider);
  }

  public static EditorViewModel newInstance(NoteRepository noteRepository,
      TagRepository tagRepository, AttachmentRepository attachmentRepository) {
    return new EditorViewModel(noteRepository, tagRepository, attachmentRepository);
  }
}
