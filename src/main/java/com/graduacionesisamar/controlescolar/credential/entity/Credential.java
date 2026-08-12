package com.graduacionesisamar.controlescolar.credential.entity;

import com.graduacionesisamar.controlescolar.student.entity.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Represents a QR credential assigned to a student.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "credentials")
public class Credential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "qr_token", nullable = false, unique = true, length = 100)
    private String qrToken;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "issued_at", nullable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "deactivated_at")
    private OffsetDateTime deactivatedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /**
     * Sets the initial values before inserting the credential.
     */
    @PrePersist
    public void beforeInsert() {
        OffsetDateTime now = OffsetDateTime.now();

        if (issuedAt == null) {
            issuedAt = now;
        }

        if (createdAt == null) {
            createdAt = now;
        }

        if (active == null) {
            active = true;
        }
    }

    /**
     * Deactivates the credential and records the operation time.
     */
    public void deactivate() {
        if (!Boolean.TRUE.equals(active)) {
            return;
        }

        active = false;
        deactivatedAt = OffsetDateTime.now();
    }
}