package com.thy.cloud.data.jpa.repository.factory;

import com.thy.cloud.data.jpa.entity.AbstractAuditionEntity;
import com.thy.cloud.data.jpa.repository.GenericRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.lang.NonNull;

import jakarta.persistence.EntityManager;
import java.io.Serializable;

/**
 * RepositoryFactoryBean
 *
 * @author Engin Mahmut
 * @param <R>
 * @param <T>
 * @param <I>
 */
public class GenericRepositoryFactoryBean<
        R extends GenericRepository<T, I>,
        T extends AbstractAuditionEntity,
        I extends Serializable>
            extends
            JpaRepositoryFactoryBean<R, T, I> {

    public GenericRepositoryFactoryBean(Class<? extends R> repositoryInterface) {
        super(repositoryInterface);
    }

    /**
     * Return a custom RepositoryFactory
     * @param entityManager EntityManager
     * @return Custom RepositoryFactory
     */
    @SuppressWarnings( "rawtypes" )
    @NonNull
    @Override
    protected RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {
        return new GenericRepositoryFactory<T, I>(entityManager);
    }

}