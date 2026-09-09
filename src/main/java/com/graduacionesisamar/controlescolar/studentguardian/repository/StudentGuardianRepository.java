package com.graduacionesisamar.controlescolar.studentguardian.repository;

import com.graduacionesisamar.controlescolar.studentguardian.entity.StudentGuardian;
import com.graduacionesisamar.controlescolar.studentguardian.entity.StudentGuardianId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    List<StudentGuardian>
    findAllByStudent_IdAndReceivesNotificationsTrueAndGuardian_ActiveTrueOrderByPrimaryContactDescGuardian_FullNameAsc(
        Long studentId
    );

    List<StudentGuardian> findAllByStudent_School_Id(Long schoolId);

    @Query("""
            SELECT link
            FROM StudentGuardian link
            JOIN FETCH link.student student
            JOIN FETCH student.schoolGroup schoolGroup
            JOIN FETCH schoolGroup.academicCycle academicCycle
            WHERE link.guardian.id IN :guardianIds
              AND academicCycle.id = :academicCycleId
            ORDER BY student.lastName, student.firstName
            """)
    List<StudentGuardian> findForGuardianSummaries(
            @Param("guardianIds") List<Long> guardianIds,
            @Param("academicCycleId") Long academicCycleId
    );
}
