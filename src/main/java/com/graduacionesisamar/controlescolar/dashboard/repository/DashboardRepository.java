package com.graduacionesisamar.controlescolar.dashboard.repository;

import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEvent;
import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEventType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Read-only aggregate queries used by the school operational dashboard.
 */
@Repository
public class DashboardRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public long countActiveStudents(Long schoolId) {
        return entityManager.createQuery(
                        """
                        SELECT COUNT(student)
                        FROM Student student
                        WHERE student.school.id = :schoolId
                          AND student.active = true
                        """,
                        Long.class
                )
                .setParameter("schoolId", schoolId)
                .getSingleResult();
    }

    public long countActiveGuardians(Long schoolId) {
        return entityManager.createQuery(
                        """
                        SELECT COUNT(guardian)
                        FROM Guardian guardian
                        WHERE guardian.school.id = :schoolId
                          AND guardian.active = true
                        """,
                        Long.class
                )
                .setParameter("schoolId", schoolId)
                .getSingleResult();
    }

    public long countStudentsWithGuardian(Long schoolId) {
        return entityManager.createQuery(
                        """
                        SELECT COUNT(DISTINCT link.student.id)
                        FROM StudentGuardian link
                        WHERE link.student.school.id = :schoolId
                          AND link.student.active = true
                          AND link.guardian.active = true
                        """,
                        Long.class
                )
                .setParameter("schoolId", schoolId)
                .getSingleResult();
    }

    public long countActiveCredentials(Long schoolId) {
        return entityManager.createQuery(
                        """
                        SELECT COUNT(DISTINCT credential.student.id)
                        FROM Credential credential
                        WHERE credential.student.school.id = :schoolId
                          AND credential.student.active = true
                          AND credential.active = true
                        """,
                        Long.class
                )
                .setParameter("schoolId", schoolId)
                .getSingleResult();
    }

    public long countActivatedGuardianAccounts(Long schoolId) {
        return entityManager.createQuery(
                        """
                        SELECT COUNT(account)
                        FROM GuardianAccount account
                        WHERE account.school.id = :schoolId
                          AND account.guardian.active = true
                          AND account.active = true
                          AND account.passwordHash IS NOT NULL
                          AND TRIM(account.passwordHash) <> ''
                        """,
                        Long.class
                )
                .setParameter("schoolId", schoolId)
                .getSingleResult();
    }

    public long countGuardiansWithNotifications(Long schoolId) {
        return entityManager.createQuery(
                        """
                        SELECT COUNT(DISTINCT device.guardian.id)
                        FROM GuardianDevice device
                        WHERE device.guardian.school.id = :schoolId
                          AND device.guardian.active = true
                          AND device.active = true
                        """,
                        Long.class
                )
                .setParameter("schoolId", schoolId)
                .getSingleResult();
    }

    public long countAccessEvents(
            Long schoolId,
            AccessEventType eventType,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        return entityManager.createQuery(
                        """
                        SELECT COUNT(event)
                        FROM AccessEvent event
                        WHERE event.student.school.id = :schoolId
                          AND event.eventType = :eventType
                          AND event.occurredAt >= :from
                          AND event.occurredAt < :to
                        """,
                        Long.class
                )
                .setParameter("schoolId", schoolId)
                .setParameter("eventType", eventType)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    public List<AccessEvent> findRecentAccessEvents(
            Long schoolId,
            OffsetDateTime from,
            OffsetDateTime to,
            int limit
    ) {
        return entityManager.createQuery(
                        """
                        SELECT event
                        FROM AccessEvent event
                        JOIN FETCH event.student student
                        LEFT JOIN FETCH student.schoolGroup schoolGroup
                        WHERE student.school.id = :schoolId
                          AND event.occurredAt >= :from
                          AND event.occurredAt < :to
                        ORDER BY event.occurredAt DESC, event.id DESC
                        """,
                        AccessEvent.class
                )
                .setParameter("schoolId", schoolId)
                .setParameter("from", from)
                .setParameter("to", to)
                .setMaxResults(limit)
                .getResultList();
    }
}
