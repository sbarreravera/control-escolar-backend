package com.graduacionesisamar.controlescolar.student.repository;

import com.graduacionesisamar.controlescolar.student.entity.Student;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Provides database operations for students.
 */
public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsBySchool_IdAndEnrollmentNumberIgnoreCase(
            Long schoolId,
            String enrollmentNumber
    );

    boolean existsBySchool_IdAndEnrollmentNumberIgnoreCaseAndIdNot(
            Long schoolId,
            String enrollmentNumber,
            Long studentId
    );

    boolean existsBySchoolGroup_Id(Long schoolGroupId);

    List<Student> findAllBySchool_IdOrderByLastNameAscFirstNameAsc(
            Long schoolId
    );

    /**
     * Searches a school's students without loading the complete roster.
     */
    @Query(
            value = """
                    SELECT student
                    FROM Student student
                    LEFT JOIN FETCH student.schoolGroup schoolGroup
                    LEFT JOIN FETCH schoolGroup.academicCycle academicCycle
                    WHERE student.school.id = :schoolId
                      AND (:academicCycleId IS NULL
                           OR academicCycle.id = :academicCycleId)
                      AND (:schoolGroupId IS NULL
                           OR schoolGroup.id = :schoolGroupId)
                      AND (:active IS NULL OR student.active = :active)
                      AND (
                          :search = ''
                          OR LOWER(student.enrollmentNumber)
                             LIKE CONCAT('%', :search, '%')
                          OR LOWER(student.firstName)
                             LIKE CONCAT('%', :search, '%')
                          OR LOWER(student.lastName)
                             LIKE CONCAT('%', :search, '%')
                          OR LOWER(CONCAT(CONCAT(student.firstName, ' '), student.lastName))
                             LIKE CONCAT('%', :search, '%')
                          OR LOWER(CONCAT(CONCAT(student.lastName, ' '), student.firstName))
                             LIKE CONCAT('%', :search, '%')
                      )
                    """,
            countQuery = """
                    SELECT COUNT(student)
                    FROM Student student
                    LEFT JOIN student.schoolGroup schoolGroup
                    LEFT JOIN schoolGroup.academicCycle academicCycle
                    WHERE student.school.id = :schoolId
                      AND (:academicCycleId IS NULL
                           OR academicCycle.id = :academicCycleId)
                      AND (:schoolGroupId IS NULL
                           OR schoolGroup.id = :schoolGroupId)
                      AND (:active IS NULL OR student.active = :active)
                      AND (
                          :search = ''
                          OR LOWER(student.enrollmentNumber)
                             LIKE CONCAT('%', :search, '%')
                          OR LOWER(student.firstName)
                             LIKE CONCAT('%', :search, '%')
                          OR LOWER(student.lastName)
                             LIKE CONCAT('%', :search, '%')
                          OR LOWER(CONCAT(CONCAT(student.firstName, ' '), student.lastName))
                             LIKE CONCAT('%', :search, '%')
                          OR LOWER(CONCAT(CONCAT(student.lastName, ' '), student.firstName))
                             LIKE CONCAT('%', :search, '%')
                      )
                    """
    )
    Page<Student> searchBySchool(
            @Param("schoolId") Long schoolId,
            @Param("search") String search,
            @Param("academicCycleId") Long academicCycleId,
            @Param("schoolGroupId") Long schoolGroupId,
            @Param("active") Boolean active,
            Pageable pageable
    );

    /**
     * Finds and locks a student during credential operations.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT student FROM Student student WHERE student.id = :id")
    Optional<Student> findByIdForUpdate(@Param("id") Long id);
}
