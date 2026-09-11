package com.graduacionesisamar.controlescolar.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables scheduling for communication publication and optional push delivery.
 */
@Configuration
@EnableScheduling
public class NotificationSchedulingConfiguration {
}
