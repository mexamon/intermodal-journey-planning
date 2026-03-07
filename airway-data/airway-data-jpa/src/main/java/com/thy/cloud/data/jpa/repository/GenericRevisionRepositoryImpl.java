package com.thy.cloud.data.jpa.repository;

import com.thy.cloud.data.jpa.entity.AbstractAuditionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryImpl;
import org.springframework.data.history.Revision;
import org.springframework.data.history.Revisions;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.data.repository.history.support.RevisionEntityInformation;

import jakarta.annotation.Nonnull;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.util.Optional;

/**
 * Generic Revision Repository Implementation
 * @author Engin Mahmut
 *
 * @param <T>
 * @param <I>
 * @param <N>
 */
@NoRepositoryBean
public class GenericRevisionRepositoryImpl
        <T extends AbstractAuditionEntity, I extends Serializable, N extends Number & Comparable<N>>
        extends
        GenericRepositoryImpl<T, I>
        implements
        GenericRevisionRepository<T, I, N> {

    private final RevisionRepository<T, I, N> revisionRepository;

    /**
     * Creates a new {@link GenericRevisionRepository} using the given {@link JpaEntityInformation}, {@link
     * RevisionEntityInformation} and {@link EntityManager}.
     *
     * @param entityInformation             must not be {@literal null}.
     * @param revisionEntityInformation     must not be {@literal null}.
     * @param entityManager                 must not be {@literal null}.
     */
    public GenericRevisionRepositoryImpl(JpaEntityInformation<T, I> entityInformation, RevisionEntityInformation revisionEntityInformation, final EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.revisionRepository = new EnversRevisionRepositoryImpl<>(entityInformation, revisionEntityInformation,
                entityManager);
        /*this.revisionRepository = new EnversRevisionRepositoryImpl<>(entityInformation,
                new ReflectionRevisionEntityInformation(DefaultRevisionEntity.class), entityManager);*/
    }

    /**
     * This method can be modified
     * @param id
     * @return
     */
    @Nonnull
    @Override
    public Optional<Revision<N, T>> findLastChangeRevision(@Nonnull I id) {
        return revisionRepository.findLastChangeRevision(id);
    }

    /**
     * This method can be modified
     * @param id
     * @return
     */
    @Nonnull
    @Override
    public Revisions<N, T> findRevisions(@Nonnull I id) {
        return revisionRepository.findRevisions(id);
    }

    /**
     * This method can be modified
     * @param id
     * @param pageable
     * @return
     */
    @Nonnull
    @Override
    public Page<Revision<N, T>> findRevisions(@Nonnull I id, Pageable pageable) {
        return revisionRepository.findRevisions(id, pageable);
    }

    /**
     * This method can be modified
     * @param id
     * @param revisionNumber
     * @return
     */
    @Nonnull
    @Override
    public Optional<Revision<N, T>> findRevision(@Nonnull I id, N revisionNumber) {
        return revisionRepository.findRevision(id, revisionNumber);
    }

}
