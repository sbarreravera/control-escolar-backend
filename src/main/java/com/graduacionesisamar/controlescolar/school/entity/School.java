package com.graduacionesisamar.controlescolar.school.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents a school registered in the system.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "schools")
public class School {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "guardian_self_registration_enabled", nullable = false)
    private Boolean guardianSelfRegistrationEnabled = false;

    @Column(
            name = "guardian_registration_token",
            nullable = false,
            unique = true,
            length = 64
    )
    private String guardianRegistrationToken;

    @Column(name = "guardian_min_per_student", nullable = false)
    private Integer guardianMinPerStudent = 1;

    @Column(name = "guardian_max_per_student", nullable = false)
    private Integer guardianMaxPerStudent = 99;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Sets initial timestamps and registration token before insertion.
     */
    @PrePersist
    public void beforeInsert() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (guardianRegistrationToken == null
                || guardianRegistrationToken.isBlank()) {
            guardianRegistrationToken = generateRegistrationToken();
        }
    }

    /**
     * Updates the modification timestamp.
     */
    @PreUpdate
    public void beforeUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public void rotateGuardianRegistrationToken() {
        guardianRegistrationToken = generateRegistrationToken();
    }

    private String generateRegistrationToken() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }
}
