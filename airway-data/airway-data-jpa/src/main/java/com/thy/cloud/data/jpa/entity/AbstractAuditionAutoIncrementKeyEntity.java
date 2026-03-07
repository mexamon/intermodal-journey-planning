package com.thy.cloud.data.jpa.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;

/**
 *
 * <h2>AbstractAuditionAutoIncrementKeyEntity</h2>
 *
 * @author Engin Mahmut
 *
 * MappedSuperclass that extends the {@link AbstractAuditionEntity} class
 * and is extended by entityold classes that have ID field of type Integer
 *
 */

@EqualsAndHashCode(callSuper = true)
@Data
@MappedSuperclass
public abstract class AbstractAuditionAutoIncrementKeyEntity extends AbstractAuditionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private int id;

}