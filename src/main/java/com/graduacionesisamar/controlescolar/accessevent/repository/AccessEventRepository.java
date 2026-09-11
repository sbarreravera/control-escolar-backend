package com.graduacionesisamar.controlescolar.accessevent.repository;

import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Provides database operations for student access events.
 */
public interface AccessEventRepository
        extends JpaRepository<AccessEvent, Long>,
        JpaSpecificationExecutor<AccessEvent> {

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
