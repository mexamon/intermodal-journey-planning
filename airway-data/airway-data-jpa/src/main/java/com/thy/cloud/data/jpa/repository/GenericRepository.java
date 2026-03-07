package com.thy.cloud.data.jpa.repository;

import com.thy.cloud.data.jpa.entity.AbstractAuditionEntity;
import com.thy.cloud.data.jpa.entity.support.Range;
import com.thy.cloud.data.jpa.util.StringFieldCount;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Base Repository Interface
 *
 * @author Engin Mahmut
 * @param <T> Persistence Class Type
 * @param <I>  Key
 */

@NoRepositoryBean
public interface GenericRepository<T extends AbstractAuditionEntity, I extends Serializable>
        extends GenericBaseRepository<T, I> {

    /**
     * Query All Records
     *
     * @param spec
     * @return
     */
    List<T> findAll(Specification<T> spec);

    /**
     * Sorting
     *
     * @param spec
     * @param sort
     * @return
     */
    List<T> findAll(Specification<T> spec, Sort sort);

    /**
     * Query All Records
     * @param spec
     * @return
     */
    List<T> findAllActive(Specification<T> spec);

    /**
     * Sorting
     *
     * @param spec
     * @param sort
     * @return
     */
    List<T> findAllActive(Specification<T> spec, Sort sort);

    /**
     * Find Active
     *
     * @return
     */
    List<T> findAllActive();

    /**
     * Query All Records Pageble
     *
     * @param pageable
     * @return
     */
    Page<T> findAll(Pageable pageable);

    /**
     * Paging
     *
     * @param spec
     * @param pageable
     * @return
     */
    Page<T> findAll(Specification<T> spec, Pageable pageable);

    /**
     * Paging for Active Records
     *
     * @param spec
     * @param pageable
     * @return
     */
    Page<T> findAllActive(Specification<T> spec, Pageable pageable);



    /**
     * Count Not Deleted
     * @param spec
     * @return
     */
    long countAllActive(@Nullable Specification<T> spec);

    /**
     * Insert Entity and set Deleted=False
     *
     * @param entity
     * @return
     */
    void insert(T entity);

    /**
     * SoftDelete Item
     *
     * @param id
     * @return
     */
    Optional<T> softDeleteById(I id);

    /**
     * Query By Example With Range
     * @param example
     * @param ranges
     * @return
     */
    List<T> queryByExampleWithRange(Example<T> example, List<Range<T>> ranges);

    /**
     * Query By Example With Range Pageable
     * @param example
     * @param ranges
     * @param pageable
     * @return
     */
    Page<T> queryByExampleWithRange(Example<T> example, List<Range<T>> ranges, Pageable pageable);


    /**
     * Query By Example With Range Sort
     * @param example
     * @param ranges
     * @param sort
     * @return
     */
    List<T> queryByExampleWithRange(Example<T> example, List<Range<T>> ranges, Sort sort);

    /**
     * Find Domain Class Pageable
     * @param t
     * @param pageable
     * @return
     */
    Page<T> find(T t, Pageable pageable);

    /**
     *
     * @param sql Native SQL will be handled
     * @param args
     */
    @Transactional(rollbackFor = Exception.class)
    void updateBySql(String sql,Object...args);

    /**
     *
     * @param hql HQL will be handled
     * @param args
     */
    @Transactional(rollbackFor = Exception.class)
    void updateByHql(String hql,Object...args);

    /**
     * Find all by id list
     * @param ids
     * @param sort
     * @return
     */
    @NonNull
    List<T> findAllByIdIn(@NonNull Collection<I> ids, @NonNull Sort sort);

    /**
     * Find all by id list pageable
     * @param ids
     * @param pageable
     * @return
     */
    @NonNull
    Page<T> findAllByIdIn(@NonNull Collection<I> ids, @NonNull Pageable pageable);

    /**
     * Deletes by id
     * @param ids
     * @return
     */
    long deleteByIdIn(@NonNull Collection<I> ids);

    /**
     *
     * @param entityClass
     * @param fieldName
     * @return
     */
    List<StringFieldCount> groupingByStringFieldAndCount(Class<T> entityClass, String fieldName);

    /**
     *
     * @param entityClass
     * @param joinFieldName
     * @param groupByFieldName
     * @return
     */
    List<StringFieldCount> groupingByStringFieldAndCount(Class<T> entityClass, String joinFieldName, String groupByFieldName);

}
