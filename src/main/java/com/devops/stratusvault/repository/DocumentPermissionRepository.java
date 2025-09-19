package com.devops.stratusvault.repository;

import com.devops.stratusvault.model.DocumentPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentPermissionRepository extends JpaRepository<DocumentPermission, Long> {

    // When you only have the caller's Firebase UID (e.g., download path)
    @Query("""
           select (count(p) > 0)
           from DocumentPermission p
           where p.document.id = :docId
             and p.sharedWithUser.firebaseUid = :uid
           """)
    boolean existsPermissionWithFirebaseUid(@Param("docId") Long documentId,
                                            @Param("uid") String firebaseUid);

    // When you have the recipient numeric id (e.g., share path)
    @Query("""
           select (count(p) > 0)
           from DocumentPermission p
           where p.document.id = :docId
             and p.sharedWithUser.id = :userId
           """)
    boolean existsPermissionWithUserId(@Param("docId") Long documentId,
                                       @Param("userId") Long userId);
}