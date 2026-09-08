package com.graduacionesisamar.controlescolar.schoolgroup.entity;

import com.graduacionesisamar.controlescolar.academiccycle.entity.AcademicCycle;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Represents a grade and group within an academic cycle.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "school_groups")
public class SchoolGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_cycle_id", nullable = false)
    private AcademicCycle academicCycle;

    @Column(name = "grade_name", nullable = false, length = 50)
    private String gradeName;

    @Column(name = "group_name", nullable = false, length = 50)
    private String groupName;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void beforeInsert() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void beforeUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
