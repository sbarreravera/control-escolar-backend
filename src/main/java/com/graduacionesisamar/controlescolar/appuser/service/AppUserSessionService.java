package com.graduacionesisamar.controlescolar.appuser.service;

import lombok.RequiredArgsConstructor;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AppUserSessionService {

    private final FindByIndexNameSessionRepository<? extends Session>
            sessionRepository;

    public void invalidateAllForPrincipal(String principalName) {
        if (principalName == null || principalName.isBlank()) {
            return;
        }

        Map<String, ? extends Session> sessions =
                sessionRepository.findByIndexNameAndIndexValue(
                        FindByIndexNameSessionRepository
                                .PRINCIPAL_NAME_INDEX_NAME,
                        principalName.trim()
                );

        Set<String> sessionIds =
                new LinkedHashSet<>(sessions.keySet());

        sessionIds.forEach(sessionRepository::deleteById);
    }
}
