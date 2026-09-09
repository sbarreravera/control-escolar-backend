package com.graduacionesisamar.controlescolar.accessevent.repository;

import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEvent;
import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEventType;
import com.graduacionesisamar.controlescolar.accessevent.entity.CaptureMethod;
import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.student.entity.Student;
import com.graduacionesisamar.controlescolar.studentguardian.entity.StudentGuardian;
import com.graduacionesisamar.controlescolar.studentguardian.entity.StudentGuardianId;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Runs against the PostgreSQL service configured by CI and reproduces the
 * no-filter request that previously failed with SQLState 42P18.
 */
@SpringBootTest
@Transactional
class GuardianAccessEventRepositoryPostgresTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AccessEventRepository accessEventRepository;

    @Test
    void findsHistoryWhenEveryOptionalFilterIsNull() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        School school = new School();
        school.setName("Escuela de Prueba 1" + suffix);
        school.setCode("ESC-TEST-1" + suffix);
        entityManager.persist(school);

        Guardian guardian = new Guardian();
        guardian.setSchool(school);
        guardian.setExternalReference("TUT-" + suffix);
        guardian.setFullName("Tutor de Prueba 1");
        entityManager.persist(guardian);

        Student student = new Student();
        student.setSchool(school);
        student.setEnrollmentNumber("MAT-" + suffix);
        student.setFirstName("Alumno1");
        student.setLastName("Ejemplo1");
        entityManager.persist(student);

        StudentGuardian relationship = new StudentGuardian();
        relationship.setId(new StudentGuardianId(
                student.getId(),
                guardian.getId()
        ));
        relationship.setStudent(student);
        relationship.setGuardian(guardian);
        relationship.setRelationship("Tutor");
        entityManager.persist(relationship);

        AccessEvent event = new AccessEvent();
        event.setStudent(student);
        event.setEventType(AccessEventType.ENTRY);
        event.setCaptureMethod(CaptureMethod.MANUAL);
        event.setOccurredAt(OffsetDateTime.now());
        entityManager.persist(event);
        entityManager.flush();
        entityManager.clear();

        Page<AccessEvent> result = accessEventRepository.findAll(
                GuardianAccessEventSpecifications.visibleToGuardian(
                        guardian.getId(),
                        school.getId(),
                        null,
                        null,
                        null,
                        null
                ),
                PageRequest.of(0, 20)
        );

        assertEquals(1, result.getTotalElements());
        assertEquals(event.getId(), result.getContent().getFirst().getId());
    }
}
