package com.graduacionesisamar.controlescolar.communication.entity;

import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "school_communication_recipients",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_communication_recipient",
                columnNames = {"communication_id", "guardian_id"}
        )
)
public class SchoolCommunicationRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "communication_id", nullable = false)
    private SchoolCommunication communication;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guardian_id", nullable = false)
    private Guardian guardian;

    @Enumerated(EnumType.STRING)
    @Column(name = "push_status", nullable = false, length = 20)
    private CommunicationRecipientPushStatus pushStatus =
            CommunicationRecipientPushStatus.NOT_ENABLED;

    @Column(name = "push_sent_at")
    private OffsetDateTime pushSentAt;

    @Column(name = "push_error", length = 1000)
    private String pushError;

    @Column(name = "viewed_at")
    private OffsetDateTime viewedAt;

    @Column(name = "acknowledged_at")
    private OffsetDateTime acknowledgedAt;
}
