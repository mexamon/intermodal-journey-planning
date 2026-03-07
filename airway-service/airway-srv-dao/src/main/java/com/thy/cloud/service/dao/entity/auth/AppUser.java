package com.thy.cloud.service.dao.entity.auth;

import com.thy.cloud.data.jpa.entity.AbstractAuditionGuidKeyEntity;
import com.thy.cloud.service.dao.enums.EnumUserRole;
import lombok.*;

import jakarta.persistence.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Table(name = "app_user")
public class AppUser extends AbstractAuditionGuidKeyEntity {

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "role", nullable = false)
    private EnumUserRole role;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
