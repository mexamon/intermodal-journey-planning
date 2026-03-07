package com.thy.cloud.data.jpa.service;

import com.thy.cloud.data.jpa.entity.AbstractAuditionEntity;
import com.thy.cloud.data.jpa.entity.support.Range;
import com.thy.cloud.data.jpa.repository.GenericRepository;
import com.thy.cloud.data.jpa.util.StringFieldCount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Base Services Implementation Class
 *
 * @author Engin Mahmut
 */


public abstract class GenericServiceImpl<T extends AbstractAuditionEntity, P extends Serializable>
        implements GenericService<T, P> {

    @Autowired
    protected GenericRepository<T, P> genericRepository;

    //Class and BaseServiceImp constructor added.
    protected Class<T> clazz;

    //Class and BaseServiceImp constructor added.
    @SuppressWarnings("unchecked")
    protected GenericServiceImpl() {
        ParameterizedType type = (ParameterizedType) this.getClass().getGenericSuperclass();
        clazz = (Class<T>) type.getActualTypeArguments()[0];

        //Experimental Entity Validation
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

    }

    public GenericRepository<T, P> getGenericRepository() {
        return genericRepository;
    }

    public void setGenericRepository(GenericRepository<T, P> genericRepository) {
        this.genericRepository = genericRepository;
    }

    protected Validator validator;

    public T initDomain() {
        return genericRepository.initDomain();
    }

    @Override
    public Optional<T> saveOrUpdate(T entity) {

        //Experimental Entity Validation
        Set<ConstraintViolation<T>> violations = validator.validate(entity);
        if(!violations.isEmpty()) {
            throw new IllegalArgumentException("Entity is not valid");
        }
        entity = genericRepository.save(entity);
        return Optional.of(entity);

    }

    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 30, propagation = Propagation.REQUIRED)
    public T save( T t ) {
        return genericRepository.save( t );
    }

    @Override
    @Transactional
    public List<T> save( Iterable<T> ts ) {
        return genericRepository.saveAll( ts );
    }

    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 30, propagation = Propagation.REQUIRED)
    public void delete( P pk ) {
        genericRepository.deleteById( pk );
    }

    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 30, propagation = Propagation.REQUIRED)
    public void delete(Iterable<T> pks) {
        genericRepository.deleteAll(pks);
    }

    @Override
    public Optional<T> softDeleteById(P id) {
        return genericRepository.softDeleteById(id);
    }

    @Override
    @Transactional
    public T update( T t ) {
        return genericRepository.save( t );
    }

    @Override
    @Transactional( readOnly = true )
    public Optional<T> find(P pk ) {
        return genericRepository.findById( pk );
    }

    @Override
    public long countAll() {
        return genericRepository.count();
    }

    @Override
    public long countAll(Specification<T> spec) {
        return genericRepository.count(spec);
    }

    @Override
    public long countAllActive(Specification<T> spec) {
        return genericRepository.countAllActive(spec);
    }

    @Override
    @Transactional( readOnly = true )
    public T get(P pk) {
        return genericRepository.getReferenceById(pk);
    }

    @Override
    public Optional<T> findOne(Specification<T> spec) {
        return genericRepository.findOne(spec);
    }

    @Override
    @Transactional( readOnly = true )
    public List<T> findAll() {
        return genericRepository.findAll();
    }

    @Override
    public List<T> findAll(Specification<T> spec, Sort sort) {
        return genericRepository.findAll(spec, sort);
    }

    @Override
    public List<T> findAll(Specification<T> spec) {
        return genericRepository.findAll(spec);
    }

    @Override
    public Page<T> findAll(Pageable pageable){
        return genericRepository.findAll(pageable);
    }

    @Override
    public Page<T> findAll(Specification<T> specs, Pageable pageable) {
        return genericRepository.findAll(specs, pageable);
    }

    @Override
    public List<T> findAllActive(Specification<T> spec, Sort sort) {
        return genericRepository.findAllActive(spec, sort);
    }

    @Override
    public List<T> findAllActive(Specification<T> spec) {
        return genericRepository.findAllActive(spec);
    }

    @Override
    public Page<T> findAllActive(Specification<T> spec, Pageable pageable) {
        return genericRepository.findAllActive(spec,pageable);
    }

    @Override
    public List<T> findAllActive() {
        return genericRepository.findAllActive();
    }

    @Override
    public boolean exists(P pk) {
        //Second usage "return genericRepository.existsById(pk)"
        return genericRepository.findById(pk).isPresent();
    }

    @Override
    public Page<T> queryByExampleWithRange(Example<T> example, List<Range<T>> ranges, Pageable pageable) {
        return genericRepository.queryByExampleWithRange(example,ranges,pageable);
    }

    @Override
    public List<T> queryByExampleWithRange(Example<T> example, List<Range<T>> ranges) {
        return genericRepository.queryByExampleWithRange(example,ranges);
    }

    @Override
    public List<T> queryByExampleWithRange(Example<T> example, List<Range<T>> ranges, Sort sort) {
        return genericRepository.queryByExampleWithRange(example,ranges,sort);
    }

    @Override
    public Page<T> find(T t, Pageable pageable){
        return genericRepository.find(t, pageable);
    }

    @Override
    public void updateBySql(String sql, Object... args) {
        genericRepository.updateBySql(sql, args);
    }

    @Override
    public void updateByHql(String hql, Object... args) {
        genericRepository.updateByHql(hql, args);
    }

    @Override
    public List<StringFieldCount> groupingByStringFieldAndCount(Class<T> entityClass, String fieldName) {
        return genericRepository.groupingByStringFieldAndCount(entityClass, fieldName);
    }

    @Override
    public List<StringFieldCount> groupingByStringFieldAndCount(Class<T> entityClass, String joinFieldName, String groupingByFieldName) {
        return genericRepository.groupingByStringFieldAndCount(entityClass, joinFieldName, groupingByFieldName);
    }

}
