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

    /**
     * Returns every active guardian relation for active students in a school.
     * School communications are durable portal messages; the legacy
     * receivesNotifications flag continues to control access-event alerts,
     * while actual push eligibility for communications is determined by the
     * guardian's active registered devices.
     */
    @Query("""
            SELECT link
            FROM StudentGuardian link
            JOIN FETCH link.student student
            JOIN FETCH link.guardian guardian
            LEFT JOIN FETCH student.schoolGroup schoolGroup
            LEFT JOIN FETCH schoolGroup.academicCycle academicCycle
            WHERE student.school.id = :schoolId
              AND student.active = true
              AND guardian.active = true
            ORDER BY guardian.id, student.id
            """)
    List<StudentGuardian> findCommunicationCandidates(
            @Param("schoolId") Long schoolId
    );

    /**
     * Lists every student related to the guardian represented by the active
     * session, including inactive students whose historical events remain
     * visible.
     */
    @Query("""
            SELECT link
            FROM StudentGuardian link
            JOIN FETCH link.student student
            LEFT JOIN FETCH student.schoolGroup schoolGroup
            LEFT JOIN FETCH schoolGroup.academicCycle academicCycle
            WHERE link.guardian.id = :guardianId
              AND link.guardian.school.id = :schoolId
              AND student.school.id = :schoolId
            ORDER BY student.lastName, student.firstName, student.id
            """)
    List<StudentGuardian> findForGuardianPortal(
            @Param("guardianId") Long guardianId,
            @Param("schoolId") Long schoolId
    );

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
