package com.graduacionesisamar.controlescolar.guardian.repository;

import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
