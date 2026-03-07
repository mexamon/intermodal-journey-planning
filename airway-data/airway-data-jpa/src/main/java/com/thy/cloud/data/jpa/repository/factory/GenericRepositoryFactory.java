package com.thy.cloud.data.jpa.repository.factory;

import com.thy.cloud.data.jpa.entity.AbstractAuditionEntity;
import com.thy.cloud.data.jpa.repository.*;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.lang.NonNull;
import org.springframework.util.ClassUtils;

import jakarta.persistence.EntityManager;
import java.io.Serializable;

/**
 * GenericRepositoryFactory
 * @author Engin Mahmut
 *
 * @param <T>
 * @param <I>
 */
public class GenericRepositoryFactory<T extends AbstractAuditionEntity, I extends Serializable> extends JpaRepositoryFactory {

    private final EntityManager em;

    public GenericRepositoryFactory(EntityManager entityManager) {
        super(entityManager);
        this.em = entityManager;
    }

    @SuppressWarnings( { "unused", "unchecked" } )
    protected Object getTargetRepository(RepositoryMetadata metadata) {
        return new GenericRepositoryImpl<T,I>((Class<T>) metadata.getDomainType(), em);
    }

    /* @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected Object getTargetRepository(RepositoryInformation information) {
        if (isGenericRepository(information.getRepositoryInterface())) {
            if (isHierarchicalRepository(information.getRepositoryInterface()))
                return new GenericTreeRepositoryImpl(getEntityInformation(information.getDomainType()), em);
            return new GenericRepositoryImpl(getEntityInformation(information.getDomainType()), em);
        } else {
            return super.getTargetRepository(information);
        }
    }*/

    /**
     * Set the specific implementation class
     * @param metadata metadata
     * @return class
     */
    @NonNull
    @Override
    protected Class<?> getRepositoryBaseClass(@NonNull RepositoryMetadata metadata) {
        if (isGenericRepository(metadata.getRepositoryInterface())) {
            return GenericRepositoryImpl.class;
        }
        else if (isHierarchicalRepository(metadata.getRepositoryInterface())) {
            return GenericTreeRepositoryImpl.class;
        }
        else if (isRevisionRepository(metadata.getRepositoryInterface())) {
            return GenericRevisionRepositoryImpl.class;
        }
        else {
            return super.getRepositoryBaseClass(metadata);
        }

    }

    private boolean isRevisionRepository(Class<?> repositoryInterface) {
        return ClassUtils.isAssignable(GenericRevisionRepository.class, repositoryInterface);
    }

    private boolean isHierarchicalRepository(Class<?> repositoryInterface) {
        return ClassUtils.isAssignable(GenericTreeRepository.class, repositoryInterface);
    }

    private boolean isGenericRepository(Class<?> repositoryInterface) {
        return ClassUtils.isAssignable(GenericRepository.class, repositoryInterface);
    }

}
