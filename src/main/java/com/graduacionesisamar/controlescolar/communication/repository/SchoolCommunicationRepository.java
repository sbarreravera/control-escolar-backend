package com.graduacionesisamar.controlescolar.communication.repository;

import com.graduacionesisamar.controlescolar.communication.entity.CommunicationStatus;
import com.graduacionesisamar.controlescolar.communication.entity.SchoolCommunication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface SchoolCommunicationRepository
        extends JpaRepository<SchoolCommunication, Long> {

    List<SchoolCommunication> findTop100BySchool_IdOrderByCreatedAtDesc(
            Long schoolId
    );

    Optional<SchoolCommunication> findByIdAndSchool_Id(
            Long id,
            Long schoolId
    );

    List<SchoolCommunication>
    findAllByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
            CommunicationStatus status,
            OffsetDateTime scheduledAt
    );
}
