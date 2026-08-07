package com.witte.lozify.data.repository;

import com.witte.lozify.data.local.dao.AttachmentDao;
import com.witte.lozify.data.local.dao.NoteDao;
import com.witte.lozify.data.local.dao.NoteRelationDao;
import com.witte.lozify.data.local.dao.TagDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class NoteRepositoryImpl_Factory implements Factory<NoteRepositoryImpl> {
  private final Provider<NoteDao> noteDaoProvider;

  private final Provider<TagDao> tagDaoProvider;

  private final Provider<AttachmentDao> attachmentDaoProvider;

  private final Provider<NoteRelationDao> relationDaoProvider;

  public NoteRepositoryImpl_Factory(Provider<NoteDao> noteDaoProvider,
      Provider<TagDao> tagDaoProvider, Provider<AttachmentDao> attachmentDaoProvider,
      Provider<NoteRelationDao> relationDaoProvider) {
    this.noteDaoProvider = noteDaoProvider;
    this.tagDaoProvider = tagDaoProvider;
    this.attachmentDaoProvider = attachmentDaoProvider;
    this.relationDaoProvider = relationDaoProvider;
  }

  @Override
  public NoteRepositoryImpl get() {
    return newInstance(noteDaoProvider.get(), tagDaoProvider.get(), attachmentDaoProvider.get(), relationDaoProvider.get());
  }

  public static NoteRepositoryImpl_Factory create(Provider<NoteDao> noteDaoProvider,
      Provider<TagDao> tagDaoProvider, Provider<AttachmentDao> attachmentDaoProvider,
      Provider<NoteRelationDao> relationDaoProvider) {
    return new NoteRepositoryImpl_Factory(noteDaoProvider, tagDaoProvider, attachmentDaoProvider, relationDaoProvider);
  }

  public static NoteRepositoryImpl newInstance(NoteDao noteDao, TagDao tagDao,
      AttachmentDao attachmentDao, NoteRelationDao relationDao) {
    return new NoteRepositoryImpl(noteDao, tagDao, attachmentDao, relationDao);
  }
}
