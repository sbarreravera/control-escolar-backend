package com.graduacionesisamar.controlescolar.notification.service;

import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEvent;
import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEventType;
import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.notification.entity.NotificationLog;
import com.graduacionesisamar.controlescolar.notification.repository.NotificationLogRepository;
import com.graduacionesisamar.controlescolar.student.entity.Student;
import com.graduacionesisamar.controlescolar.studentguardian.repository.StudentGuardianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Handles the creation of pending guardian notifications.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationLogService {

    private final NotificationLogRepository notificationLogRepository;
    private final StudentGuardianRepository studentGuardianRepository;

    /**
     * Queues one notification for each active subscribed guardian.
     */
    public int queueForEvent(AccessEvent event) {
        List<NotificationLog> notifications = studentGuardianRepository
                .findAllByStudent_IdAndReceivesNotificationsTrueAndGuardian_ActiveTrueOrderByPrimaryContactDescGuardian_FullNameAsc(
                        event.getStudent().getId()
                )
                .stream()
                .map(association -> buildNotification(
                        event,
                        association.getGuardian()
                ))
                .toList();

        notificationLogRepository.saveAll(notifications);
        return notifications.size();
    }

    private NotificationLog buildNotification(
            AccessEvent event,
            Guardian guardian
    ) {
        NotificationLog notification = new NotificationLog();
        notification.setAccessEvent(event);
        notification.setGuardian(guardian);
        notification.setTitle(buildTitle(event.getEventType()));
        notification.setMessage(buildMessage(event));
        return notification;
    }

    private String buildTitle(AccessEventType eventType) {
        return eventType == AccessEventType.ENTRY
                ? "Entrada registrada"
                : "Salida registrada";
    }

    private String buildMessage(AccessEvent event) {
        String movement = event.getEventType() == AccessEventType.ENTRY
                ? "una entrada"
                : "una salida";

        return "%s registró %s.".formatted(
                buildStudentName(event.getStudent()),
                movement
        );
    }

    private String buildStudentName(Student student) {
        return "%s %s".formatted(
                student.getFirstName(),
                student.getLastName()
        );
    }
}