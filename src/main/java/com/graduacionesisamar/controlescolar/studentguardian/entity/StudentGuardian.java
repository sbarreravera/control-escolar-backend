package com.graduacionesisamar.controlescolar.studentguardian.entity;

import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.student.entity.Student;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Represents the relationship between a student and a guardian.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "student_guardians")
public class StudentGuardian {

    @EmbeddedId
    private StudentGuardianId id;

    @MapsId("studentId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @MapsId("guardianId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guardian_id", nullable = false)
    private Guardian guardian;

    @Column(length = 50)
    private String relationship;

    @Column(name = "primary_contact", nullable = false)
    private Boolean primaryContact = false;

    @Column(name = "receives_notifications", nullable = false)
    private Boolean receivesNotifications = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /**
     * Sets the creation timestamp before inserting the relationship.
     */
    @PrePersist
    public void beforeInsert() {
        createdAt = OffsetDateTime.now();
    }
}