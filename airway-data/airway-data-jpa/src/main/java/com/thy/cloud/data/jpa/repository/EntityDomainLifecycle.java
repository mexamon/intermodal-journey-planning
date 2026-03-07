package com.thy.cloud.data.jpa.repository;

/**
 * Entity Domain LifeCycle
 * @author Engin Mahmut
 *
 * @param <T>
 */
public interface EntityDomainLifecycle<T> {
	/**
	 * Get Domain
	 * @return
	 */
	T initDomain();
}
