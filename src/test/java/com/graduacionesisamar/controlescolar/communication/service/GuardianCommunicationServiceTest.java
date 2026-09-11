package com.graduacionesisamar.controlescolar.communication.service;

import com.graduacionesisamar.controlescolar.communication.entity.CommunicationCategory;
import com.graduacionesisamar.controlescolar.communication.entity.CommunicationPriority;
import com.graduacionesisamar.controlescolar.communication.entity.CommunicationStatus;
import com.graduacionesisamar.controlescolar.communication.entity.CommunicationType;
import com.graduacionesisamar.controlescolar.communication.entity.SchoolCommunication;
import com.graduacionesisamar.controlescolar.communication.entity.SchoolCommunicationRecipient;
import com.graduacionesisamar.controlescolar.communication.repository.SchoolCommunicationRecipientRepository;
import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardiansession.security.GuardianPrincipal;
import com.graduacionesisamar.controlescolar.school.entity.School;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuardianCommunicationServiceTest {

    @Mock
    private SchoolCommunicationRecipientRepository recipientRepository;

    @InjectMocks
    private GuardianCommunicationService communicationService;

    @Test
    void viewMarksCommunicationAsViewedOnlyOnce() {
        GuardianPrincipal principal = principal();
        SchoolCommunicationRecipient recipient = recipient(true);

        when(recipientRepository.findGuardianCommunication(
                50L,
                principal.guardianId(),
                principal.schoolId()
        )).thenReturn(Optional.of(recipient));

        var response = communicationService.view(principal, 50L);

        assertNotNull(recipient.getViewedAt());
        assertNull(recipient.getAcknowledgedAt());
        assertNotNull(response.viewedAt());
        assertNull(response.acknowledgedAt());
        verify(recipientRepository).save(recipient);
    }

    @Test
    void acknowledgeMarksViewedAndAcknowledged() {
        GuardianPrincipal principal = principal();
        SchoolCommunicationRecipient recipient = recipient(true);

        when(recipientRepository.findGuardianCommunication(
                50L,
                principal.guardianId(),
                principal.schoolId()
        )).thenReturn(Optional.of(recipient));

        var response = communicationService.acknowledge(principal, 50L);

        assertNotNull(recipient.getViewedAt());
        assertNotNull(recipient.getAcknowledgedAt());
        assertNotNull(response.acknowledgedAt());
        verify(recipientRepository).save(recipient);
    }

    @Test
    void acknowledgeRejectsCommunicationThatDoesNotRequireIt() {
        GuardianPrincipal principal = principal();
        SchoolCommunicationRecipient recipient = recipient(false);

        when(recipientRepository.findGuardianCommunication(
                50L,
                principal.guardianId(),
                principal.schoolId()
        )).thenReturn(Optional.of(recipient));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> communicationService.acknowledge(principal, 50L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertNull(recipient.getViewedAt());
        assertNull(recipient.getAcknowledgedAt());
    }

    private GuardianPrincipal principal() {
        return new GuardianPrincipal(
                1L,
                10L,
                20L,
                30L,
                "Tutor Prueba",
                "Colegio Prueba",
                OffsetDateTime.now().plusDays(30)
        );
    }

    private SchoolCommunicationRecipient recipient(boolean requiresAck) {
        School school = new School();
        school.setId(30L);
        school.setName("Colegio Prueba");

        Guardian guardian = new Guardian();
        guardian.setId(10L);
        guardian.setSchool(school);
        guardian.setFullName("Tutor Prueba");

        SchoolCommunication communication = new SchoolCommunication();
        communication.setId(50L);
        communication.setSchool(school);
        communication.setType(CommunicationType.GENERAL_NOTICE);
        communication.setCategory(CommunicationCategory.GENERAL);
        communication.setPriority(CommunicationPriority.IMPORTANT);
        communication.setStatus(CommunicationStatus.PUBLISHED);
        communication.setTitle("Aviso de prueba");
        communication.setMessage("Contenido del aviso");
        communication.setRequiresAcknowledgement(requiresAck);
        communication.setPublishedAt(OffsetDateTime.now());

        SchoolCommunicationRecipient recipient =
                new SchoolCommunicationRecipient();
        recipient.setId(60L);
        recipient.setGuardian(guardian);
        recipient.setCommunication(communication);
        return recipient;
    }
}
