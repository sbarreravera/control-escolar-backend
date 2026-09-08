package com.graduacionesisamar.controlescolar.guardian.repository;

import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Provides database operations for guardians.
 */
public interface GuardianRepository extends JpaRepository<Guardian, Long> {

    List<Guardian> findAllBySchool_IdOrderByFullNameAsc(Long schoolId);
}