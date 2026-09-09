package com.graduacionesisamar.controlescolar.guardiandevice.repository;

import com.graduacionesisamar.controlescolar.guardiandevice.entity.GuardianDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Provides database operations for guardian devices.
 */
public interface GuardianDeviceRepository
        extends JpaRepository<GuardianDevice, Long> {

    Optional<GuardianDevice> findByFcmToken(String fcmToken);

    Optional<GuardianDevice> findByIdAndGuardian_Id(
            Long id,
            Long guardianId
    );

    List<GuardianDevice>
    findAllByGuardian_IdAndActiveTrueOrderByRegisteredAtDesc(
            Long guardianId
    );

    List<GuardianDevice> findAllByGuardian_School_Id(Long schoolId);

    List<GuardianDevice>
    findAllByGuardian_IdInAndActiveTrue(List<Long> guardianIds);
}
