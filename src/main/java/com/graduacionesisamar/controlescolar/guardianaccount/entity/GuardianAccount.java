package com.graduacionesisamar.controlescolar.guardianaccount.entity;

import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.school.entity.School;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Recoverable credentials for a school-scoped guardian identity.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "guardian_accounts")
public class GuardianAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guardian_id", nullable = false, unique = true)
    private Guardian guardian;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "failed_attempts", nullable = false)
    private Integer failedAttempts = 0;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @Column(name = "activated_at")
    private OffsetDateTime activatedAt;

    @Column(name = "password_changed_at")
    private OffsetDateTime passwordChangedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public boolean isActivated() {
        return passwordHash != null && !passwordHash.isBlank();
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
