package com.graduacionesisamar.controlescolar.guardiansession.repository;

import com.graduacionesisamar.controlescolar.guardiansession.entity.GuardianSession;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence operations for guardian portal sessions.
 */
public interface GuardianSessionRepository
        extends JpaRepository<GuardianSession, Long> {

    @EntityGraph(attributePaths = {
            "guardian",
            "guardian.school"
    })
    Optional<GuardianSession> findByTokenHash(String tokenHash);

    List<GuardianSession> findAllByGuardian_School_Id(Long schoolId);

    List<GuardianSession> findAllByGuardian_IdIn(List<Long> guardianIds);

    List<GuardianSession>
    findAllByGuardian_IdInAndRevokedAtIsNull(List<Long> guardianIds);
}
