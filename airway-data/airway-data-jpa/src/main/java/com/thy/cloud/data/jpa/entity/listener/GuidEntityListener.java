package com.thy.cloud.data.jpa.entity.listener;

import com.thy.cloud.data.jpa.entity.AbstractAuditionGuidKeyEntity;

import jakarta.persistence.PrePersist;
import java.util.UUID;

/**
 *
 * <h2>GuidEntityListener</h2>
 *
 * @author Engin Mahmut
 *
 * GuidEntityListener generates UUID for created entities
 *
 */
public class GuidEntityListener {

    @PrePersist
    public void onGuidPrePersist(final AbstractAuditionGuidKeyEntity entity) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }
    }
}