package com.devops.stratusvault.repository;

import com.devops.stratusvault.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    @Query("""
      SELECT DISTINCT d
      FROM Document d
      JOIN FETCH d.owner o
      WHERE o.firebaseUid = :firebaseUid
         OR EXISTS (
              SELECT 1
              FROM DocumentPermission p
              JOIN p.sharedWithUser su
              WHERE p.document = d
                AND su.firebaseUid = :firebaseUid
         )
      """)
    List<Document> findDocumentsOwnedByOrSharedWithUser(@Param("firebaseUid") String firebaseUid);

//        List<Document> findDocumentsVisibleTo(String firebaseUid);
}
