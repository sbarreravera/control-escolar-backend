package com.graduacionesisamar.controlescolar.communication.job;

import com.graduacionesisamar.controlescolar.communication.service.SchoolCommunicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommunicationPublishingJob {

    private final SchoolCommunicationService communicationService;

    @Scheduled(
            initialDelayString = "${app.communications.publish-delay-ms:5000}",
            fixedDelayString = "${app.communications.publish-delay-ms:5000}"
    )
    public void publishDueCommunications() {
        int published = communicationService.publishDue();
        if (published > 0) {
            log.info("Published {} scheduled communication(s)", published);
        }
    }
}
