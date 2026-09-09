package com.graduacionesisamar.controlescolar.guardian.repository;

import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Provides database operations for guardians.
 */
public interface GuardianRepository extends JpaRepository<Guardian, Long> {

    List<Guardian> findAllBySchool_IdOrderByFullNameAsc(Long schoolId);

    List<Guardian> findAllBySchool_IdAndIdInOrderByFullNameAsc(
            Long schoolId,
            List<Long> ids
    );

    Optional<Guardian> findBySchool_IdAndExternalReferenceIgnoreCase(
            Long schoolId,
            String externalReference
    );

    boolean existsBySchool_IdAndExternalReferenceIgnoreCase(
            Long schoolId,
            String externalReference
    );

    /**
     * Finds distinct guardians related to active students in one cycle.
     */
    @Query("""
            SELECT DISTINCT guardian
            FROM StudentGuardian link
            JOIN link.guardian guardian
            JOIN link.student student
            JOIN student.schoolGroup schoolGroup
            JOIN schoolGroup.academicCycle academicCycle
            WHERE guardian.school.id = :schoolId
              AND student.school.id = :schoolId
              AND student.active = true
              AND academicCycle.id = :academicCycleId
              AND (:schoolGroupId IS NULL OR schoolGroup.id = :schoolGroupId)
              AND (:gradeName = '' OR LOWER(schoolGroup.gradeName) = :gradeName)
              AND (
                  :contact = 'ALL'
                  OR :contact = 'MISSING'
                     AND (guardian.phone IS NULL OR TRIM(guardian.phone) = '')
                     AND (guardian.email IS NULL OR TRIM(guardian.email) = '')
                  OR :contact = 'AVAILABLE'
                     AND (guardian.phone IS NOT NULL AND TRIM(guardian.phone) <> ''
                          OR guardian.email IS NOT NULL AND TRIM(guardian.email) <> '')
              )
              AND (
                  :search = ''
                  OR LOWER(guardian.fullName) LIKE CONCAT('%', :search, '%')
                  OR LOWER(COALESCE(guardian.externalReference, ''))
                     LIKE CONCAT('%', :search, '%')
                  OR LOWER(COALESCE(guardian.phone, ''))
                     LIKE CONCAT('%', :search, '%')
                  OR LOWER(COALESCE(guardian.email, ''))
                     LIKE CONCAT('%', :search, '%')
                  OR LOWER(student.enrollmentNumber)
                     LIKE CONCAT('%', :search, '%')
                  OR LOWER(CONCAT(CONCAT(student.firstName, ' '), student.lastName))
                     LIKE CONCAT('%', :search, '%')
              )
            ORDER BY guardian.fullName, guardian.id
            """)
    List<Guardian> findForActivation(
            @Param("schoolId") Long schoolId,
            @Param("academicCycleId") Long academicCycleId,
            @Param("schoolGroupId") Long schoolGroupId,
            @Param("gradeName") String gradeName,
            @Param("contact") String contact,
            @Param("search") String search
    );
}
