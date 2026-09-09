package com.graduacionesisamar.controlescolar.guardianaccount.repository;

import com.graduacionesisamar.controlescolar.guardianaccount.entity.GuardianAccount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence operations for guardian login accounts.
 */
public interface GuardianAccountRepository
        extends JpaRepository<GuardianAccount, Long> {

    @EntityGraph(attributePaths = {
            "guardian",
            "guardian.school",
            "school"
    })
    Optional<GuardianAccount> findByGuardian_Id(Long guardianId);

    @EntityGraph(attributePaths = {
            "guardian",
            "guardian.school",
            "school"
    })
    Optional<GuardianAccount>
    findBySchool_CodeIgnoreCaseAndUsernameIgnoreCase(
            String schoolCode,
            String username
    );

    boolean existsBySchool_IdAndUsernameIgnoreCase(
            Long schoolId,
            String username
    );

    @EntityGraph(attributePaths = "guardian")
    List<GuardianAccount> findAllByGuardian_IdIn(
            List<Long> guardianIds
    );

    List<GuardianAccount> findAllBySchool_Id(Long schoolId);
}
