package com.graduacionesisamar.controlescolar.guardian.entity;

import com.graduacionesisamar.controlescolar.school.entity.School;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Represents a parent or guardian registered in a school.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "guardians")
public class Guardian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(length = 30)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Sets the initial timestamps before inserting the guardian.
     */
    @PrePersist
    public void beforeInsert() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    /**
     * Updates the modification timestamp.
     */
    @PreUpdate
    public void beforeUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}