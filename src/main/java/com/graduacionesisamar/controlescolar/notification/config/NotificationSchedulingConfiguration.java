package com.graduacionesisamar.controlescolar.notification.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables scheduled notification delivery when explicitly configured.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(
        name = "app.notifications.delivery-enabled",
        havingValue = "true"
)
public class NotificationSchedulingConfiguration {

}