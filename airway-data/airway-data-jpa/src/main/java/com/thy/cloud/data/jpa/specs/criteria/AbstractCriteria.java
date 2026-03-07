package com.thy.cloud.data.jpa.specs.criteria;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import java.io.Serializable;

abstract class AbstractCriteria<T> implements Specification<T>, Serializable {

    /**
     * Extracts the nested property name from a dot-separated path.
     * e.g. "address.city" → "city"
     */
    public String getProperty(String property) {
        if (property.contains(".")) {
            String[] parts = property.split("\\.", 2);
            return parts[1];
        }
        return property;
    }

    /**
     * Resolves the JPA root/join for a dot-separated property path.
     * e.g. "address.city" → root.join("address", LEFT)
     */
    @SuppressWarnings("rawtypes")
    public From getRoot(String property, Root<T> root) {
        if (property.contains(".")) {
            String joinProperty = property.split("\\.", 2)[0];
            return root.join(joinProperty, JoinType.LEFT);
        }
        return root;
    }
}
