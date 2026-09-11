package com.graduacionesisamar.controlescolar.communication.entity;

import com.graduacionesisamar.controlescolar.appuser.entity.AppUser;
import com.graduacionesisamar.controlescolar.school.entity.School;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "school_communications")
public class SchoolCommunication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CommunicationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CommunicationCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommunicationPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommunicationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience_type", nullable = false, length = 30)
    private CommunicationAudienceType audienceType;

    @Column(name = "audience_summary", nullable = false, length = 500)
    private String audienceSummary;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 4000)
    private String message;

    @Column(name = "requires_acknowledgement", nullable = false)
    private Boolean requiresAcknowledgement = false;

    @Column(name = "student_count", nullable = false)
    private Integer studentCount = 0;

    @Column(name = "recipient_count", nullable = false)
    private Integer recipientCount = 0;

    @Column(name = "push_recipient_count", nullable = false)
    private Integer pushRecipientCount = 0;

    @Column(name = "scheduled_at")
    private OffsetDateTime scheduledAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void beforeInsert() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void beforeUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
