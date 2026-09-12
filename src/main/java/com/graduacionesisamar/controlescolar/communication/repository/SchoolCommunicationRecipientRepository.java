package com.graduacionesisamar.controlescolar.communication.repository;

import com.graduacionesisamar.controlescolar.communication.entity.CommunicationRecipientPushStatus;
import com.graduacionesisamar.controlescolar.communication.entity.SchoolCommunicationRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SchoolCommunicationRecipientRepository
        extends JpaRepository<SchoolCommunicationRecipient, Long> {

    List<SchoolCommunicationRecipient>
    findAllByCommunication_IdOrderByGuardian_FullNameAsc(Long communicationId);

    @Modifying
    void deleteAllByCommunication_Id(Long communicationId);

    long countByCommunication_IdAndPushStatus(
            Long communicationId,
            CommunicationRecipientPushStatus pushStatus
    );

    long countByCommunication_IdAndViewedAtIsNotNull(Long communicationId);

    long countByCommunication_IdAndAcknowledgedAtIsNotNull(Long communicationId);

    long countByGuardian_Id(Long guardianId);

    @Modifying
    @Query("DELETE FROM SchoolCommunicationRecipient recipient WHERE recipient.guardian.id = :guardianId")
    int deleteAllForGuardian(@Param("guardianId") Long guardianId);

    @Query("""
            SELECT recipient
            FROM SchoolCommunicationRecipient recipient
            JOIN FETCH recipient.communication communication
            JOIN FETCH communication.school school
            JOIN FETCH recipient.guardian guardian
            WHERE guardian.id = :guardianId
              AND school.id = :schoolId
              AND communication.status = com.graduacionesisamar.controlescolar.communication.entity.CommunicationStatus.PUBLISHED
            ORDER BY communication.publishedAt DESC, communication.id DESC
            """)
    List<SchoolCommunicationRecipient> findForGuardianPortal(
            @Param("guardianId") Long guardianId,
            @Param("schoolId") Long schoolId
    );

    @Query("""
            SELECT recipient
            FROM SchoolCommunicationRecipient recipient
            JOIN FETCH recipient.communication communication
            JOIN FETCH communication.school school
            JOIN FETCH recipient.guardian guardian
            WHERE communication.id = :communicationId
              AND guardian.id = :guardianId
              AND school.id = :schoolId
              AND communication.status = com.graduacionesisamar.controlescolar.communication.entity.CommunicationStatus.PUBLISHED
            """)
    Optional<SchoolCommunicationRecipient> findGuardianCommunication(
            @Param("communicationId") Long communicationId,
            @Param("guardianId") Long guardianId,
            @Param("schoolId") Long schoolId
    );
}
