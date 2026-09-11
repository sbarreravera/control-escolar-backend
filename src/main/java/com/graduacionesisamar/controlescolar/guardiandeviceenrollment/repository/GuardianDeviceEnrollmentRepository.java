package com.graduacionesisamar.controlescolar
        .guardiandeviceenrollment.repository;

import com.graduacionesisamar.controlescolar
        .guardiandeviceenrollment.entity.GuardianDeviceEnrollment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

/**
 * Provides persistence operations for guardian device
 * enrollment invitations.
 */
public interface GuardianDeviceEnrollmentRepository
        extends JpaRepository<GuardianDeviceEnrollment, Long> {

    /**
     * Locks the invitation while it is being completed,
     * preventing simultaneous reuse of the same token.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<GuardianDeviceEnrollment> findByTokenHash(
            String tokenHash
    );

    @EntityGraph(attributePaths = {
            "guardian",
            "guardian.school"
    })
    @Query("""
            SELECT invitation
            FROM GuardianDeviceEnrollment invitation
            WHERE invitation.tokenHash = :tokenHash
            """)
    Optional<GuardianDeviceEnrollment> findForStatusByTokenHash(
            @Param("tokenHash") String tokenHash
    );

    /**
     * Returns unused and non-revoked invitations
     * created for a guardian.
     */
    List<GuardianDeviceEnrollment>
    findAllByGuardian_IdAndUsedAtIsNullAndRevokedAtIsNull(
            Long guardianId
    );

    List<GuardianDeviceEnrollment>
    findAllByGuardian_IdInAndUsedAtIsNullAndRevokedAtIsNull(
            List<Long> guardianIds
    );

    List<GuardianDeviceEnrollment>
    findAllByGuardian_IdInOrderByCreatedAtDesc(List<Long> guardianIds);

    List<GuardianDeviceEnrollment>
    findAllByGuardian_School_IdOrderByCreatedAtDesc(Long schoolId);
}
