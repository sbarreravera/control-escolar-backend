package com.graduacionesisamar.controlescolar.schoolgroup.repository;

import com.graduacionesisamar.controlescolar.schoolgroup.entity.SchoolGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Provides database operations for school groups.
 */
public interface SchoolGroupRepository extends JpaRepository<SchoolGroup, Long> {

    boolean existsByAcademicCycle_IdAndGradeNameIgnoreCaseAndGroupNameIgnoreCase(
            Long academicCycleId,
            String gradeName,
            String groupName
    );

    List<SchoolGroup> findAllByAcademicCycle_IdOrderByGradeNameAscGroupNameAsc(
            Long academicCycleId
    );
}
