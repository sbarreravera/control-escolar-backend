package com.graduacionesisamar.controlescolar.student.repository;

import com.graduacionesisamar.controlescolar.student.entity.Student;
import jakarta.persistence.LockModeType;
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

    List<Student> findAllBySchool_IdOrderByLastNameAscFirstNameAsc(
            Long schoolId
    );

    /**
     * Finds and locks a student during credential operations.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT student FROM Student student WHERE student.id = :id")
    Optional<Student> findByIdForUpdate(@Param("id") Long id);
}
