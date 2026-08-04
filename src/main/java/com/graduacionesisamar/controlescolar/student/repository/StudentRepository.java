package com.graduacionesisamar.controlescolar.student.repository;

import com.graduacionesisamar.controlescolar.student.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Provides database operations for students.
 */
public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsBySchool_IdAndEnrollmentNumberIgnoreCase(
            Long schoolId,
            String enrollmentNumber
    );

    List<Student> findAllBySchool_IdOrderByLastNameAscFirstNameAsc(
            Long schoolId
    );
}