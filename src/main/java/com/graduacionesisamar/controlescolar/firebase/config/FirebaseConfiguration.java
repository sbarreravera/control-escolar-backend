package com.graduacionesisamar.controlescolar.firebase.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * Configures the Firebase Admin SDK when Firebase integration
 * is explicitly enabled.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "app.firebase",
        name = "enabled",
        havingValue = "true"
)
public class FirebaseConfiguration {

    private final String projectId;

    public FirebaseConfiguration(
            @Value("${app.firebase.project-id}")
            String projectId
    ) {
        this.projectId = projectId;
    }

    /**
     * Initializes the default Firebase application using
     * Application Default Credentials.
     */
    @Bean(destroyMethod = "delete")
    public FirebaseApp firebaseApp() throws IOException {
        if (!StringUtils.hasText(projectId)) {
            throw new IllegalStateException(
                    "Firebase project ID is required"
            );
        }

        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(
                        GoogleCredentials.getApplicationDefault()
                )
                .setProjectId(projectId)
                .build();

        FirebaseApp firebaseApp =
                FirebaseApp.initializeApp(options);

        log.info(
                "Firebase Admin SDK initialized for project {}",
                firebaseApp.getOptions().getProjectId()
        );

        return firebaseApp;
    }

    /**
     * Exposes the Firebase Cloud Messaging client.
     */
    @Bean
    public FirebaseMessaging firebaseMessaging(
            FirebaseApp firebaseApp
    ) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}