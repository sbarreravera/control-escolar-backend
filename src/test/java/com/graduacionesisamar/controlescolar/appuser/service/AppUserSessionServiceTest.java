package com.graduacionesisamar.controlescolar.appuser.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserSessionServiceTest {

    @Mock
    private FindByIndexNameSessionRepository<Session>
            sessionRepository;

    @Test
    void invalidateAllForPrincipalDeletesEverySession() {
        Session first = mock(Session.class);
        Session second = mock(Session.class);

        when(sessionRepository.findByIndexNameAndIndexValue(
                FindByIndexNameSessionRepository
                        .PRINCIPAL_NAME_INDEX_NAME,
                "prefecto@escuela.mx"
        )).thenReturn(Map.of(
                "session-1",
                first,
                "session-2",
                second
        ));

        AppUserSessionService service =
                new AppUserSessionService(sessionRepository);

        service.invalidateAllForPrincipal(
                "prefecto@escuela.mx"
        );

        verify(sessionRepository).deleteById("session-1");
        verify(sessionRepository).deleteById("session-2");
    }
}
