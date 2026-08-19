package com.graduacionesisamar.controlescolar.guardiandevice.entity;

import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
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
 * Represents a device registered to receive notifications
 * for a guardian.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "guardian_devices")
public class GuardianDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guardian_id", nullable = false)
    private Guardian guardian;

    @Column(
            name = "fcm_token",
            nullable = false,
            unique = true,
            columnDefinition = "TEXT"
    )
    private String fcmToken;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(nullable = false)
    private boolean active = true;

    @Column(
            name = "registered_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime registeredAt;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;

    @PrePersist
    void prePersist() {
        if (registeredAt == null) {
            registeredAt = OffsetDateTime.now();
        }
    }
}