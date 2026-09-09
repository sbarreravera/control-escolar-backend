package com.graduacionesisamar.controlescolar.accessevent.repository;

import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEvent;
import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Provides database operations for student access events.
 */
public interface AccessEventRepository
        extends JpaRepository<AccessEvent, Long> {

    /**
     * Returns one page of events visible to a guardian. Both guardian and
     * school come from the server-side guardian session.
     */
    @Query(
            value = """
                    SELECT event
                    FROM AccessEvent event
                    JOIN FETCH event.student student
                    WHERE student.school.id = :schoolId
                      AND EXISTS (
                          SELECT link.student.id
                          FROM StudentGuardian link
                          WHERE link.student.id = student.id
                            AND link.guardian.id = :guardianId
                            AND link.guardian.school.id = :schoolId
                      )
                      AND (:studentId IS NULL OR student.id = :studentId)
                      AND (:eventType IS NULL OR event.eventType = :eventType)
                      AND (:occurredFrom IS NULL OR event.occurredAt >= :occurredFrom)
                      AND (:occurredTo IS NULL OR event.occurredAt < :occurredTo)
                    ORDER BY event.occurredAt DESC, event.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(event)
                    FROM AccessEvent event
                    WHERE event.student.school.id = :schoolId
                      AND EXISTS (
                          SELECT link.student.id
                          FROM StudentGuardian link
                          WHERE link.student.id = event.student.id
                            AND link.guardian.id = :guardianId
                            AND link.guardian.school.id = :schoolId
                      )
                      AND (:studentId IS NULL OR event.student.id = :studentId)
                      AND (:eventType IS NULL OR event.eventType = :eventType)
                      AND (:occurredFrom IS NULL OR event.occurredAt >= :occurredFrom)
                      AND (:occurredTo IS NULL OR event.occurredAt < :occurredTo)
                    """
    )
    Page<AccessEvent> findGuardianHistory(
            @Param("guardianId") Long guardianId,
            @Param("schoolId") Long schoolId,
            @Param("studentId") Long studentId,
            @Param("eventType") AccessEventType eventType,
            @Param("occurredFrom") OffsetDateTime occurredFrom,
            @Param("occurredTo") OffsetDateTime occurredTo,
            Pageable pageable
    );

    /**
     * Resolves a deep-linked event without revealing events from another
     * guardian or school.
     */
    @Query("""
            SELECT event
            FROM AccessEvent event
            JOIN FETCH event.student student
            WHERE event.id = :eventId
              AND student.school.id = :schoolId
              AND EXISTS (
                  SELECT link.student.id
                  FROM StudentGuardian link
                  WHERE link.student.id = student.id
                    AND link.guardian.id = :guardianId
                    AND link.guardian.school.id = :schoolId
              )
            """)
    Optional<AccessEvent> findGuardianEvent(
            @Param("eventId") Long eventId,
            @Param("guardianId") Long guardianId,
            @Param("schoolId") Long schoolId
    );

    Optional<AccessEvent>
    findFirstByStudent_IdOrderByOccurredAtDescIdDesc(Long studentId);
}
