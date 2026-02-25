package ktb.billage.infra.fcm;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

@Slf4j
@Configuration
@ConditionalOnProperty(name = {"fcm.json_base64", "fcm.project_id"})
public class FcmConfig {
    private final byte[] firebaseResource;
    private final String projectId;

    public FcmConfig(
            @Value("${fcm.json_base64}") String firebaseKey,
            @Value("${fcm.project_id}") String projectId
    ) {
        this.firebaseResource = Base64.getDecoder().decode(firebaseKey);
        this.projectId = projectId;
        log.info("FCM config loaded. projectId={}, jsonBytes={}", projectId, this.firebaseResource.length);
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() throws IOException {
        log.info("Creating FirebaseMessaging bean. projectId={}", projectId);
        return FirebaseMessaging.getInstance(firebaseApp());
    }

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        try {
            log.info("Using existing FirebaseApp instance. projectId={}", projectId);
            return FirebaseApp.getInstance();
        } catch (IllegalStateException ignored) {
            ByteArrayInputStream in = new ByteArrayInputStream(firebaseResource);
            GoogleCredentials credentials = GoogleCredentials.fromStream(in);

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setProjectId(projectId)
                    .build();
            log.info("Initializing new FirebaseApp instance. projectId={}", projectId);
            return FirebaseApp.initializeApp(options);
        }
    }
}
