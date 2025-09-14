package com.devops.stratusvault.config;

import com.google.api.client.util.Value;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

//    // Injects the path to the service account key from application.properties
//    @Value("${app.firebase.service-account-key-path}")
//    private Resource serviceAccountKeyResource;

    /**
     * This method initializes the Firebase Admin SDK when the application starts.
     * It reads the service account key (your serviceAccountKey.json) which gives
     * our backend administrative privileges to verify tokens.
     */
    @PostConstruct
    public void initializeFirebase() {
        try {
            ClassPathResource serviceAccountResource = new ClassPathResource("serviceAccountKey.json");
            try (InputStream serviceAccount = serviceAccountResource.getInputStream()) {
                // Check if a Firebase app has already been initialized to avoid errors on restart
                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .build();
                    FirebaseApp.initializeApp(options);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize Firebase Admin SDK", e);
        }
    }
}
