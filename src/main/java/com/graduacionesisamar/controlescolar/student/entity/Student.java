package com.graduacionesisamar.controlescolar.student.entity;

import com.graduacionesisamar.controlescolar.school.entity.School;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Represents a student registered in a school.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Column(name = "enrollment_number", nullable = false, length = 50)
    private String enrollmentNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 150)
    private String lastName;

    @Column(name = "grade_name", length = 50)
    private String gradeName;

    @Column(name = "group_name", length = 50)
    private String groupName;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Sets initial timestamps before inserting the student.
     */
    @PrePersist
    public void beforeInsert() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    /**
     * Updates the modification timestamp.
     */
    @PreUpdate
    public void beforeUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}