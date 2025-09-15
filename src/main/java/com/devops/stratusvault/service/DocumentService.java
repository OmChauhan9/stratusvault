package com.devops.stratusvault.service;

import com.devops.stratusvault.model.Document;
import com.devops.stratusvault.model.DocumentPermission;
import com.devops.stratusvault.model.PermissionLevel;
import com.devops.stratusvault.model.User;
import com.devops.stratusvault.repository.DocumentPermissionRepository;
import com.devops.stratusvault.repository.DocumentRepository;
import com.devops.stratusvault.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final GcsService gcsService;
    private final DocumentPermissionRepository documentPermissionRepository;

    @Value("${app.gcs.bucket-name}")
    private String gcsBucketName;

    public record DownloadableFile(byte[] data, String fileName, String contentType) {}

    public DocumentService(DocumentRepository documentRepository, UserRepository userRepository, GcsService gcsService, DocumentPermissionRepository documentPermissionRepository) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.gcsService = gcsService;
        this.documentPermissionRepository = documentPermissionRepository;
    }

    public Document uploadDocument(MultipartFile multipartFile, String firebaseUid, String email) throws IOException {
        // Find or create the User
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setFirebaseUid(firebaseUid);
                    newUser.setEmail(email);
                    return userRepository.save(newUser);
                });

        // Read, compress, and get the size of the file
        byte[] compressedData;
        try (InputStream inputStream = multipartFile.getInputStream()){
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);

            // Copy the original stream to the compressing stream
            inputStream.transferTo(gzipOutputStream);
            gzipOutputStream.finish();
            compressedData = byteArrayOutputStream.toByteArray();
        }

        //Generate a unique filename and upload to GCS
        String gcsObjectName = UUID.randomUUID().toString() + ".gz";   // Creates a unique ID string
        gcsService.uploadFile(compressedData, gcsBucketName, gcsObjectName, multipartFile.getContentType());  // Assume this returns the full path

        //Create the new Document entity
        Document newDocument = new Document();

        newDocument.setFileName(multipartFile.getOriginalFilename());
        newDocument.setGcsPath(gcsObjectName);
        newDocument.setOriginalSize(multipartFile.getSize());
        newDocument.setCompressedSize(compressedData.length);
        newDocument.setUploadTimeStamp(new Timestamp(System.currentTimeMillis()).toLocalDateTime());
        newDocument.setOwner(user);

        return documentRepository.save(newDocument);
    }

    public List<Document> getDocumentsForUser(String firebaseUid) {
        System.out.println("firebaseUId :::: " + firebaseUid);
        return documentRepository.findDocumentsVisibleTo(firebaseUid);
    }

    public Optional<DownloadableFile> downloadDocument(long documentId, String firebaseUid) throws IOException {
        Optional<Document> documentOptional = documentRepository.findById(documentId);

        // 1. Check if document exists
        if (documentOptional.isEmpty()) {
            return Optional.empty();
        }
        Document document = documentOptional.get();

        boolean isOwner = document.getOwner().getFirebaseUid().equals(firebaseUid);
        boolean hasPermission = documentPermissionRepository.existsByDocument_IdAndSharedWithUser_FirebaseUid(documentId, firebaseUid);

        if (!isOwner && !hasPermission) {
            return Optional.empty();
        }

        // 3. Download the compressed file from GCS
        byte[] compressedBytes = gcsService.downloadFile(gcsBucketName, document.getGcsPath());
        // 4. Decompress the file data
        byte[] decompressedBytes = decompressGzip(compressedBytes);
        // 5. Return the file data in our wrapper object
        return Optional.of(new DownloadableFile(decompressedBytes, document.getFileName(), document.getContentType()));
    }

    private byte[] decompressGzip(byte[] compressedData) throws IOException {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressedData);
             GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

            gzipInputStream.transferTo(byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        }
    }

    public Optional<Document> findDocumentById(long id) {
        return documentRepository.findById(id);
    }

    @Transactional
    public void shareDocument(long documentId, String ownerFirebaseUid, String shareWithEmail) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));

        // Security Check: Verify ownership
        if(!document.getOwner().getFirebaseUid().equals(ownerFirebaseUid)) {
            throw new SecurityException("Unauthorized: You do not own this document.");
        }

        User sharedWithUser = userRepository.findByEmail(shareWithEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + shareWithEmail));

        // Self-Share Check
        if(sharedWithUser.getFirebaseUid().equals(ownerFirebaseUid)) {
            throw new IllegalArgumentException("Cannot share a document with yourself.");
        }

        // Prevent duplicate shares
        boolean alreadyExists = documentPermissionRepository.existsByDocumentAndSharedWithUser(document,sharedWithUser);
        if(alreadyExists) {
            throw new IllegalArgumentException("This document is already shared with that user.");
        }

        DocumentPermission documentPermission = new DocumentPermission();
        documentPermission.setDocument(document);
        documentPermission.setSharedWithUser(sharedWithUser);
        documentPermission.setPermissionLevel(PermissionLevel.READER);

        documentPermissionRepository.save(documentPermission);
    }
}
