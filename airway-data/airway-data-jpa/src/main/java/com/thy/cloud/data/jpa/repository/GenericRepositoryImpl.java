package com.thy.cloud.data.jpa.repository;

import com.thy.cloud.base.core.api.ResultType;
import com.thy.cloud.base.core.exception.AirwayException;
import com.thy.cloud.data.jpa.entity.AbstractAuditionEntity;
import com.thy.cloud.data.jpa.entity.support.Range;
import com.thy.cloud.data.jpa.specs.criteria.ExampleCriteria;
import com.thy.cloud.data.jpa.specs.criteria.RangeCriteria;
import com.thy.cloud.data.jpa.util.GenericSpecification;

import com.thy.cloud.data.jpa.util.StringFieldCount;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;

import static org.springframework.data.jpa.domain.Specification.where;

/**
 * Base Repository Implementation Class
 * @author Engin Mahmut
 *
 * @param <T> Persistence Class Type
 * @param <I> Key
 */

@NoRepositoryBean
public class GenericRepositoryImpl<T extends AbstractAuditionEntity, I extends Serializable>
        extends SimpleJpaRepository<T, I>
        implements GenericRepository<T, I> {

    private static final String DELETED_FIELD = "deleted";

    private JpaEntityInformation<T, I> entityInformation;

    private final EntityManager entityManager;

    /**
     * Creates a new {@link SimpleJpaRepository} to manage objects of the given
     * {@link JpaEntityInformation}.
     * @param domainClass
     * @param entityManager
     */
    public GenericRepositoryImpl(Class<T> domainClass, EntityManager entityManager) {
        super(domainClass, entityManager);
        this.entityManager = entityManager;
    }

    /**
     * Creates a new {@link SimpleJpaRepository} to manage objects of the given
     * {@link JpaEntityInformation}.
     *  @param entityInformation
     * @param entityManager
     */
    public GenericRepositoryImpl(JpaEntityInformation<T, I> entityInformation, final EntityManager entityManager) {
        super( entityInformation, entityManager );
        this.entityInformation = entityInformation;
        this.entityManager = entityManager;
    }

    @Override
    public List<T> findAllActive(Specification<T> spec) {
        return super.findAll(where(spec).and(isNotDeleted()));
    }

    @Override
    public Page<T> findAllActive(Specification<T> spec, Pageable pageable) {
        return super.findAll(where(spec).and(isNotDeleted()), pageable);
    }

    @Override
    public List<T> findAllActive(Specification<T> spec, Sort sort) {
        return super.findAll(where(spec).and(isNotDeleted()), sort);
    }

    @Override
    public List<T> findAllActive() {
        return super.findAll(where(isNotDeleted()));
    }

    @Override
    public long countAllActive(Specification<T> spec) {
        return super.count(where(spec).and(isNotDeleted()));
    }

    //Helper Criteria For SoftDelete
    private static <T> Specification <T> isNotDeleted() {
        return (root, query, cb) -> cb.equal(root.get(DELETED_FIELD), false);
    }

    @Override
    public void insert(T entity) {
        if (entity.getLastModifiedDate() == null) {
            entity.setCreatedDate(new Date());
            entity.setDeleted(false);
            entityManager.persist(entity);
        }
    }

    @Override
    public Optional<T> softDeleteById(I id) {
        var entity = entityManager.find(this.getDomainClass(), id);
        Optional<T> returned = Optional.empty();
        if (entity != null) {
            entity.setLastModifiedDate(new Date());
            entity.setDeleted(true);
            entityManager.persist(entity);
            returned = Optional.of(entity);
        }
        return returned;
    }

    @Override
    public Page<T> queryByExampleWithRange(Example<T> example, List<Range<T>> ranges, Pageable pageable) {
        Specification<T> byExample = new ExampleCriteria<>(example);
        Specification<T> byRanges = new RangeCriteria<>(ranges);
        return findAll(where(byExample).and(byRanges),pageable);
    }

    @Override
    public List<T> queryByExampleWithRange(Example<T> example, List<Range<T>> ranges) {
        Specification<T> byExample = new ExampleCriteria<>(example);
        Specification<T> byRanges = new RangeCriteria<>(ranges);
        return findAll(where(byExample).and(byRanges));
        /*return findAll(byRanges.and(Specification.<T>where(byExample)))*/
    }

    @Override
    public List<T> queryByExampleWithRange(Example<T> example, List<Range<T>> ranges, Sort sort) {
        Specification<T> byExample = new ExampleCriteria<>(example);
        Specification<T> byRanges = new RangeCriteria<>(ranges);
        return findAll(where(byExample).and(byRanges),sort);
    }

    /** Use entity combined with Specification default settings for paging
     * @Description
     * @param t
     * @param pageable
     * @return org.springframework.data.domain.Page<T>
     * @throws
     */
    @Override
    public Page<T> find(T t, Pageable pageable) {
        Specification<T> spec = GenericSpecification.getSpecification(t);
        return findAll(spec, pageable);
    }

    @Override
    public void updateBySql(String sql, Object... args) {
        var query = entityManager.createNativeQuery(sql);
        var i = 0;
        for (Object arg : args) {
            query.setParameter(++i, arg);
        }
        query.executeUpdate();
    }

    @Override
    public void updateByHql(String hql, Object... args) {
        var query = entityManager.createQuery(hql);
        var i = 0;
        for (Object arg : args) {
            query.setParameter(++i, arg);
        }
        query.executeUpdate();
    }

    @Override
    public T initDomain() {
        try {
            return getDomainClass().getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new AirwayException(ResultType.DOMAIN_NULL);
        }
    }

    /**
     * Finds all domain by id list and the specified sort.
     *
     * @param ids id list of domain must not be null
     * @param sort the specified sort must not be null
     * @return a list of domains
     */
    @Override
    public List<T> findAllByIdIn(Collection<I> ids, Sort sort) {
        Assert.notNull(ids, "The given Collection of Id's must not be null!");
        Assert.notNull(sort, "Sort info must not be null");

        if (!ids.iterator().hasNext()) {
            return Collections.emptyList();
        }
        if (entityInformation.hasCompositeId()) {
            List<T> results = new ArrayList<>();
            ids.forEach(id -> super.findById(id).ifPresent(results::add));
            return results;
        }

        ByIdsSpecification<T> specification = new ByIdsSpecification<>(entityInformation);
        TypedQuery<T> query = super.getQuery(specification, sort);
        return query.setParameter(String.valueOf(specification.parameter), ids).getResultList();
    }


    /**
     * Finds all domain by id list and the specified pageable.
     *
     * @param ids id list of domain must not be null
     * @param pageable the specified pageable must not be null
     * @return a list of domains
     */
    @Override
    public Page<T> findAllByIdIn(Collection<I> ids, Pageable pageable) {
        Assert.notNull(ids, "The given Collection of Id's must not be null!");
        Assert.notNull(pageable, "Page info must not be null");

        if (!ids.iterator().hasNext()) {
            return new PageImpl<>(Collections.emptyList());
        }

        if (entityInformation.hasCompositeId()) {
            throw new UnsupportedOperationException(
                    "Unsupported find all by composite id with page info");
        }

        ByIdsSpecification<T> specification = new ByIdsSpecification<>(entityInformation);
        TypedQuery<T> query =
                super.getQuery(specification, pageable).setParameter(String.valueOf(specification.parameter), ids);
        TypedQuery<Long> countQuery = getCountQuery(specification, getDomainClass())
                .setParameter(String.valueOf(specification.parameter), ids);

        return pageable.isUnpaged()
                ? new PageImpl<>(query.getResultList())
                : readPage(query, pageable, countQuery);
    }

    /**
     * Deletes by id list.
     *
     * @param ids id list of domain must not be null
     * @return number of rows affected
     */
    @Override
    @Transactional
    public long deleteByIdIn(Collection<I> ids) {

        // Find all domains
        List<T> domains = findAllById(ids);

        // Delete in batch
        deleteInBatch(domains);

        // Return the size of domain deleted
        return domains.size();
    }

    /**
     * Count with grouping by a string field in an entity
     * @param entityClass
     * @param fieldName
     * @return
     */
    @Override
    public List<StringFieldCount> groupingByStringFieldAndCount(Class<T> entityClass, String fieldName) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<StringFieldCount> query = criteriaBuilder.createQuery(StringFieldCount.class);

        Root<T> entityRoot = query.from(entityClass);
        Expression<String> stringFieldExp = entityRoot.get(fieldName);
        Expression<Long> countExp = criteriaBuilder.count(entityRoot);

        query.multiselect(stringFieldExp, countExp);
        query.groupBy(stringFieldExp);

        TypedQuery<StringFieldCount> typedQuery = entityManager.createQuery(query);
        return typedQuery.getResultList();
    }

    @Override
    public List<StringFieldCount> groupingByStringFieldAndCount(Class<T> entityClass, String joinFieldName, String groupByFieldName) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<StringFieldCount> query = criteriaBuilder.createQuery(StringFieldCount.class);

        Root<T> entityRoot = query.from(entityClass);
        Join<Object, Object> joinEntity = entityRoot.join(joinFieldName);

        Expression<String> groupByFieldExp = joinEntity.get(groupByFieldName);
        Expression<Long> countExp = criteriaBuilder.count(entityRoot);

        query.multiselect(groupByFieldExp, countExp);
        query.groupBy(groupByFieldExp);

        TypedQuery<StringFieldCount> typedQuery = entityManager.createQuery(query);
        return typedQuery.getResultList();
    }


    @Override
    public <S extends T, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        return null;
    }

    private static final class ByIdsSpecification<T> implements Specification<T>, Serializable{
        private static final long serialVersionUID = 1L;

        private final transient JpaEntityInformation<T, ?> entityInformation;

        @Nullable
        public transient ParameterExpression<? extends Collection> parameter;

        ByIdsSpecification(JpaEntityInformation<T, ?> entityInformation) {
            this.entityInformation = entityInformation;
        }

        @Override
        public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
            Path<?> path = root.get(this.entityInformation.getIdAttribute());
            this.parameter = cb.parameter(Collection.class);
            return path.in(this.parameter);
        }
    }

    protected <S extends T> Page<S> readPage(TypedQuery<S> query,
                                                  Pageable pageable, TypedQuery<Long> countQuery) {

        if (pageable.isPaged()) {
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
        }

        List<S> content = query.getResultList();
        return new PageImpl<>(content, pageable, executeCountQuery(countQuery));
    }
    /**
     * Executes a count query and transparently sums up all values returned.
     *
     * @param query must not be {@literal null}.
     * @return count
     */
    private static long executeCountQuery(TypedQuery<Long> query) {

        Assert.notNull(query, "TypedQuery must not be null!");

        List<Long> totals = query.getResultList();
        return totals.stream().mapToLong(element -> element == null ? 0 : element).sum();
    }

}
