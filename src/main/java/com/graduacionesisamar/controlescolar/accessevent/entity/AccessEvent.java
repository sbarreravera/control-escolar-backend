package com.graduacionesisamar.controlescolar.accessevent.entity;

import com.graduacionesisamar.controlescolar.credential.entity.Credential;
import com.graduacionesisamar.controlescolar.student.entity.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Represents a student entry or exit event.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "access_events")
public class AccessEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credential_id")
    private Credential credential;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private AccessEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "capture_method", nullable = false, length = 20)
    private CaptureMethod captureMethod;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "recorded_by")
    private Long recordedBy;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /**
     * Sets server-controlled timestamps before inserting the event.
     */
    @PrePersist
    public void beforeInsert() {
        OffsetDateTime now = OffsetDateTime.now();

        if (occurredAt == null) {
            occurredAt = now;
        }

        if (createdAt == null) {
            createdAt = now;
        }
    }
}