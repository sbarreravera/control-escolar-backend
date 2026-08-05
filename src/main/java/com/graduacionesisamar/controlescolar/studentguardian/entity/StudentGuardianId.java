package com.graduacionesisamar.controlescolar.studentguardian.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Represents the composite identifier of a student-guardian relationship.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class StudentGuardianId implements Serializable {

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "guardian_id")
    private Long guardianId;
}