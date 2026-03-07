package com.thy.cloud.data.jpa.entity;

import com.thy.cloud.data.jpa.entity.listener.GuidEntityListener;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.io.Serializable;
import java.util.UUID;

/**
 *
 * <h2>AbstractAuditionGuidKeyEntity</h2>
 *
 * @author Engin Mahmut
 *
 * MappedSuperclass that extends the {@link AbstractAuditionEntity} class
 * and is extended by entityold classes that have ID field of type String
 *
 */

@Getter
@Setter
@MappedSuperclass
@EntityListeners({AuditingEntityListener.class, GuidEntityListener.class })
public abstract class AbstractAuditionGuidKeyEntity extends AbstractAuditionEntity implements Serializable {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "UUID")
    /* @GeneratedValue(strategy = GenerationType.AUTO) */
    /* @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator") */
    /* @Type(type = "uuid-char") */
    private UUID id;

}