package com.graduacionesisamar.controlescolar.school.repository;

import com.graduacionesisamar.controlescolar.school.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides database operations for schools.
 */
public interface SchoolRepository extends JpaRepository<School, Long> {

    boolean existsByCodeIgnoreCase(String code);
}