package com.graduacionesisamar.controlescolar.notification.repository;

import com.graduacionesisamar.controlescolar.notification.entity.NotificationLog;
import com.graduacionesisamar.controlescolar.notification.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Provides database operations for queued notifications.
 */
public interface NotificationLogRepository
        extends JpaRepository<NotificationLog, Long> {

    List<NotificationLog> findAllByAccessEvent_IdOrderByCreatedAtAsc(
            Long accessEventId
    );

    List<NotificationLog> findTop50ByStatusOrderByCreatedAtAsc(
            NotificationStatus status
    );

    long countByGuardian_Id(Long guardianId);

    @Modifying
    @Query("DELETE FROM NotificationLog log WHERE log.guardian.id = :guardianId")
    int deleteAllForGuardian(@Param("guardianId") Long guardianId);
}
