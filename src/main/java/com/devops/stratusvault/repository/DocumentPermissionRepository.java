package com.devops.stratusvault.repository;

import com.devops.stratusvault.model.Document;
import com.devops.stratusvault.model.DocumentPermission;
import com.devops.stratusvault.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentPermissionRepository extends JpaRepository<DocumentPermission, Long> {
    boolean existsByDocument_IdAndSharedWithUser_FirebaseUid(long documentId, String firebaseUid);
    boolean existsByDocumentAndSharedWithUser(Document document, User user);
}
