package com.thy.cloud.data.jpa.repository;

import com.thy.cloud.data.jpa.entity.AbstractAuditionEntity;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.history.RevisionRepository;

import java.io.Serializable;

/**
 * Generic Revision Repository Interface
 * @author Engin Mahmut
 *
 * @param <T>
 * @param <I>
 * @param <N>
 */
@NoRepositoryBean
public interface GenericRevisionRepository <T extends AbstractAuditionEntity, I extends Serializable, N extends Number & Comparable<N>>
            extends
            RevisionRepository<T, I, N>, GenericRepository<T, I> {

}
