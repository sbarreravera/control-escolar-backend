package com.graduacionesisamar.controlescolar.studentguardian.repository;

import com.graduacionesisamar.controlescolar.studentguardian.entity.StudentGuardian;
import com.graduacionesisamar.controlescolar.studentguardian.entity.StudentGuardianId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Provides database operations for student-guardian relationships.
 */
public interface StudentGuardianRepository
        extends JpaRepository<StudentGuardian, StudentGuardianId> {

    boolean existsByStudent_IdAndGuardian_Id(
            Long studentId,
            Long guardianId
    );

    boolean existsByStudent_IdAndPrimaryContactTrue(Long studentId);

    List<StudentGuardian>
    findAllByStudent_IdOrderByPrimaryContactDescGuardian_FullNameAsc(
            Long studentId
    );
}