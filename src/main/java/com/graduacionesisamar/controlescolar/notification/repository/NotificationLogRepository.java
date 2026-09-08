package com.graduacionesisamar.controlescolar.notification.repository;

import com.graduacionesisamar.controlescolar.notification.entity.NotificationLog;
import com.graduacionesisamar.controlescolar.notification.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

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
}