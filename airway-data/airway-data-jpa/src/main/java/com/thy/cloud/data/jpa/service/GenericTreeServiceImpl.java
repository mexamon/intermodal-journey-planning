package com.thy.cloud.data.jpa.service;

import com.thy.cloud.data.jpa.entity.AbstractTreeEntity;
import com.thy.cloud.data.jpa.entity.support.Node;
import com.thy.cloud.data.jpa.entity.support.Tree;
import com.thy.cloud.data.jpa.repository.GenericTreeRepository;
import org.springframework.data.jpa.domain.Specification;

import java.io.Serializable;
import java.util.List;

/**
 * Generic Tree Services Implementation Class
 *
 * @author Engin Mahmut
 */
public abstract class GenericTreeServiceImpl<T extends AbstractTreeEntity<?, I, T>, I extends Serializable>
    extends GenericServiceImpl<T, I>
        implements GenericTreeService<T , I> {

    public GenericTreeRepository<T, I> getRepository() {
        return (GenericTreeRepository<T, I>) super.getGenericRepository();
    }
/*
    @Autowired
    protected GenericRepository<T, I> genericRepository;

    public GenericRepository<T, I> getGenericRepository() {
        return genericRepository;
    }

    public void setGenericRepository(GenericRepository<T, I> genericRepository) {
        this.genericRepository = genericRepository;
    }*/

    @Override
    public List<T> findRoots() {
        return getRepository().findRoots();
    }

    @Override
    public List<T> findAllChildren(T root) {
        return getRepository().findAllChildren(root);
    }

    @Override
    public Tree<T> findByRoot(T root) {
        return getRepository().findByRoot(root);
    }

    @Override
    public Tree<T> findTree(Specification<T> predicate) {
        return getRepository().findTree(predicate);
    }

    @Override
    public Tree<T> findTree(Specification<T> predicate, Node<T> singleRoot) {
        return getRepository().findTree(predicate, singleRoot);
    }

    @Override
    public T sort(T source, T target, String action) {
        return getRepository().sort(source, target, action);
    }
}
