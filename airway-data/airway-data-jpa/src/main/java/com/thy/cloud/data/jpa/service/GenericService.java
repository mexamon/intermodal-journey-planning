package com.thy.cloud.data.jpa.service;

import com.thy.cloud.data.jpa.entity.AbstractAuditionEntity;
import com.thy.cloud.data.jpa.entity.support.Range;
import com.thy.cloud.data.jpa.util.StringFieldCount;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * Base Services Interface
 *
 * @author Engin Mahmut
 */

public interface GenericService<T extends AbstractAuditionEntity, P extends Serializable>  {

    T initDomain();

    Optional<T> saveOrUpdate(T entity);

    T save(T t);

    List<T> save(Iterable<T> ts);

    void delete(P pk);

    void delete(Iterable<T> pks);

    Optional<T> softDeleteById(P id);

    T update(T t);

    Optional<T> find(P pk);

    long countAll();

    long countAll(Specification<T> spec);

    long countAllActive(Specification<T> spec);

    T get(P pk);

    Optional<T> findOne(@Nullable Specification<T> spec);

    List<T> findAll();

    List<T> findAll(Specification<T> spec, Sort sort);

    List<T> findAll(Specification<T> spec);

    Page<T> findAll(Pageable pageable);

    Page<T> findAll(Specification<T> specs, Pageable pageable);

    List<T> findAllActive(Specification<T> spec, Sort sort);

    List<T> findAllActive(Specification<T> spec);

    Page<T> findAllActive(Specification<T> spec, Pageable pageable);

    List<T> findAllActive();

    boolean exists(P pk);

    Page<T> queryByExampleWithRange(Example<T> example, List<Range<T>> ranges, Pageable pageable);

    List<T> queryByExampleWithRange(Example<T> example, List<Range<T>> ranges);

    List<T> queryByExampleWithRange(Example<T> example, List<Range<T>> ranges, Sort sort);

    Page<T> find(T t, Pageable pageable);

    void updateBySql(String sql,Object...args);

    void updateByHql(String hql,Object...args);

    List<StringFieldCount> groupingByStringFieldAndCount(Class<T> entityClass, String fieldName);

    List<StringFieldCount> groupingByStringFieldAndCount(Class<T> entityClass, String joinFieldName, String groupingByFieldName);


}
