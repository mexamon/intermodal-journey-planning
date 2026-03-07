package com.thy.cloud.data.jpa.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.*;
import java.util.Date;

/**
 *
 * <h2>AbstractAuditionEntity</h2>
 *
 * @author Engin Mahmut
 *
 *         MappedSuperclass that contains all the necessary fields
 *
 */

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(value = { "created_date", "last_modified_date" }, allowGetters = true)
public abstract class AbstractAuditionEntity {

    private static final long serialVersionUID = 1L;

    @JsonIgnore
    @Version
    @Column(name = "version", nullable = false)
    protected Long version;

    /* @JsonIgnore */
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_date", updatable = false, nullable = false)
    @CreatedDate
    protected Date createdDate;

    @JsonIgnore
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "last_modified_date", nullable = true)
    @LastModifiedDate
    protected Date lastModifiedDate;

    @JsonIgnore
    @Column(name = "deleted")
    private boolean deleted;

    /*
     * PrePersist
     *
     * When saving or updating an entity with the Prepersist notation, the following
     * method takes action.
     */
    @PrePersist
    protected void onCreate() {
        this.version = 1L;
    }

}
