package com.graduacionesisamar.controlescolar.academiccycle.repository;

import com.graduacionesisamar.controlescolar.academiccycle.entity.AcademicCycle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Provides database operations for academic cycles.
 */
public interface AcademicCycleRepository
        extends JpaRepository<AcademicCycle, Long> {

    boolean existsBySchool_IdAndNameIgnoreCase(
            Long schoolId,
            String name
    );

    List<AcademicCycle> findAllBySchool_IdOrderByStartDateDesc(
            Long schoolId
    );
}
