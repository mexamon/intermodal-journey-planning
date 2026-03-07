package com.thy.cloud.data.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.io.Serializable;

/**
 * Generic Repository Interface
 * @author Engin Mahmut
 *
 * @param <T>
 * @param <I>
 */@NoRepositoryBean
public interface GenericBaseRepository<T, I extends Serializable>
		extends
		JpaRepository<T, I>,
		JpaSpecificationExecutor<T>,
		PagingAndSortingRepository<T, I>,
		EntityDomainLifecycle<T> {
}
