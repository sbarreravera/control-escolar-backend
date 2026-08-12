package com.graduacionesisamar.controlescolar.accessevent.repository;

import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides database operations for student access events.
 */
public interface AccessEventRepository
        extends JpaRepository<AccessEvent, Long> {
}