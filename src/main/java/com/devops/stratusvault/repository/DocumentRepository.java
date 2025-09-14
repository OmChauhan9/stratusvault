package com.devops.stratusvault.repository;

import com.devops.stratusvault.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByOwner_FirebaseUid(String firebaseUid);
}
