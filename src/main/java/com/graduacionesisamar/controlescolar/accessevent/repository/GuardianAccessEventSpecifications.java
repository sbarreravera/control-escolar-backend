package com.graduacionesisamar.controlescolar.accessevent.repository;

import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEvent;
import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEventType;
import com.graduacionesisamar.controlescolar.studentguardian.entity.StudentGuardian;
import com.graduacionesisamar.controlescolar.student.entity.Student;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds guardian history predicates only for filters that were supplied.
 * This avoids untyped null timestamp parameters in PostgreSQL.
 */
public final class GuardianAccessEventSpecifications {

    private GuardianAccessEventSpecifications() {
    }

    public static Specification<AccessEvent> visibleToGuardian(
            Long guardianId,
            Long schoolId,
            Long studentId,
            AccessEventType eventType,
            OffsetDateTime occurredFrom,
            OffsetDateTime occurredTo
    ) {
        return (root, query, criteriaBuilder) -> {
            if (query.getResultType() != Long.class
                    && query.getResultType() != long.class) {
                root.fetch("student");
                query.distinct(true);
            }

            Join<AccessEvent, Student> student = root.join("student");
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(
                    student.get("school").get("id"),
                    schoolId
            ));

            Subquery<Long> relationshipQuery = query.subquery(Long.class);
            Root<StudentGuardian> relationship = relationshipQuery.from(
                    StudentGuardian.class
            );
            relationshipQuery.select(
                    relationship.get("student").get("id")
            );
            relationshipQuery.where(
                    criteriaBuilder.equal(
                            relationship.get("student").get("id"),
                            student.get("id")
                    ),
                    criteriaBuilder.equal(
                            relationship.get("guardian").get("id"),
                            guardianId
                    ),
                    criteriaBuilder.equal(
                            relationship.get("guardian")
                                    .get("school").get("id"),
                            schoolId
                    )
            );
            predicates.add(criteriaBuilder.exists(relationshipQuery));

            if (studentId != null) {
                predicates.add(criteriaBuilder.equal(
                        student.get("id"),
                        studentId
                ));
            }
            if (eventType != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("eventType"),
                        eventType
                ));
            }
            if (occurredFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("occurredAt"),
                        occurredFrom
                ));
            }
            if (occurredTo != null) {
                predicates.add(criteriaBuilder.lessThan(
                        root.get("occurredAt"),
                        occurredTo
                ));
            }

            return criteriaBuilder.and(
                    predicates.toArray(Predicate[]::new)
            );
        };
    }
}
