package com.graduacionesisamar.controlescolar.communication.service;

import com.graduacionesisamar.controlescolar.communication.dto.GuardianCommunicationResponse;
import com.graduacionesisamar.controlescolar.communication.entity.SchoolCommunication;
import com.graduacionesisamar.controlescolar.communication.entity.SchoolCommunicationRecipient;
import com.graduacionesisamar.controlescolar.communication.repository.SchoolCommunicationRecipientRepository;
import com.graduacionesisamar.controlescolar.guardiansession.security.GuardianPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GuardianCommunicationService {

    private final SchoolCommunicationRecipientRepository recipientRepository;

    @Transactional(readOnly = true)
    public List<GuardianCommunicationResponse> findAll(
            GuardianPrincipal principal
    ) {
        return recipientRepository.findForGuardianPortal(
                        principal.guardianId(),
                        principal.schoolId()
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public GuardianCommunicationResponse view(
            GuardianPrincipal principal,
            Long communicationId
    ) {
        SchoolCommunicationRecipient recipient = requireRecipient(
                principal,
                communicationId
        );
        if (recipient.getViewedAt() == null) {
            recipient.setViewedAt(OffsetDateTime.now());
            recipientRepository.save(recipient);
        }
        return toResponse(recipient);
    }

    public GuardianCommunicationResponse acknowledge(
            GuardianPrincipal principal,
            Long communicationId
    ) {
        SchoolCommunicationRecipient recipient = requireRecipient(
                principal,
                communicationId
        );
        SchoolCommunication communication = recipient.getCommunication();
        if (!Boolean.TRUE.equals(communication.getRequiresAcknowledgement())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Este aviso no requiere confirmación."
            );
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (recipient.getViewedAt() == null) {
            recipient.setViewedAt(now);
        }
        if (recipient.getAcknowledgedAt() == null) {
            recipient.setAcknowledgedAt(now);
        }
        recipientRepository.save(recipient);
        return toResponse(recipient);
    }

    private SchoolCommunicationRecipient requireRecipient(
            GuardianPrincipal principal,
            Long communicationId
    ) {
        return recipientRepository.findGuardianCommunication(
                        communicationId,
                        principal.guardianId(),
                        principal.schoolId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aviso no encontrado."
                ));
    }

    private GuardianCommunicationResponse toResponse(
            SchoolCommunicationRecipient recipient
    ) {
        SchoolCommunication communication = recipient.getCommunication();
        return new GuardianCommunicationResponse(
                communication.getId(),
                communication.getType(),
                communication.getCategory(),
                communication.getPriority(),
                communication.getTitle(),
                communication.getMessage(),
                Boolean.TRUE.equals(communication.getRequiresAcknowledgement()),
                communication.getPublishedAt(),
                recipient.getViewedAt(),
                recipient.getAcknowledgedAt()
        );
    }
}
