package com.devops.stratusvault.service;

import com.devops.stratusvault.exceptionhandler.errors.ForbiddenException;
import com.devops.stratusvault.exceptionhandler.errors.NotFoundException;
import com.devops.stratusvault.exceptionhandler.errors.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

// your existing imports for MultipartFile, GZIP, etc.
import org.springframework.web.multipart.MultipartFile;

// your project types:
import com.devops.stratusvault.model.Document;
import com.devops.stratusvault.model.DocumentPermission;
import com.devops.stratusvault.model.PermissionLevel;
import com.devops.stratusvault.model.User;
import com.devops.stratusvault.repository.DocumentPermissionRepository;
import com.devops.stratusvault.repository.DocumentRepository;
import com.devops.stratusvault.repository.UserRepository;
import org.springframework.web.server.ResponseStatusException;


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
        newDocument.setContentType(multipartFile.getContentType());
        newDocument.setOwner(user);

        return documentRepository.save(newDocument);
    }

    public List<Document> findForUser(String firebaseUid) {
        return documentRepository.findDocumentsOwnedByOrSharedWithUser(firebaseUid);
    }

    public Optional<DownloadableFile> downloadDocument(long documentId, String firebaseUid) throws IOException {
        Optional<Document> documentOptional = documentRepository.findById(documentId);

        // 1. Check if document exists
        if (documentOptional.isEmpty()) {
            return Optional.empty();
        }
        Document document = documentOptional.get();

        boolean isOwner = document.getOwner().getFirebaseUid().equals(firebaseUid);
        boolean hasPermission = documentPermissionRepository.existsPermissionWithFirebaseUid(documentId, firebaseUid);

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

    public void shareDocument(Long documentId, String requesterFirebaseUid, String recipientEmail) {
        System.out.println("Documetn IDD ::::: " + documentId);
        // 1) Load the document or 404
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document not found"));

        // 2) Only the owner may share → 403
//        User owner = doc.getOwner();
//        if (owner == null || !requesterFirebaseUid.equals(owner.getFirebaseUid())) {
//            throw new ResponseStatusException(
//                    HttpStatus.FORBIDDEN, "Only the owner can share this document");
//        }

        // 3) Resolve recipient by email (must have signed in at least once) → 404
        User recipient = userRepository.findByEmail(recipientEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User with email '%s' is not registered yet. Ask them to sign in once."
                                .formatted(recipientEmail)));

        // 4) Disallow sharing to self → 400
        if (requesterFirebaseUid.equals(recipient.getFirebaseUid())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "You cannot share a document with yourself");
        }

        // 5) Idempotency: if already shared, do nothing
        boolean alreadyShared =
                documentPermissionRepository.existsPermissionWithUserId(doc.getId(), recipient.getId());
        if (alreadyShared) return;

        // 6) Persist permission (race-safe against duplicates)
        try {
            DocumentPermission perm = new DocumentPermission();
            perm.setDocument(doc);
            perm.setSharedWithUser(recipient);
            perm.setPermissionLevel(PermissionLevel.READER);
            documentPermissionRepository.save(perm);
        } catch (DataIntegrityViolationException ignored) {
            // another request inserted the same row concurrently; treat as success
        }
    }

    @Transactional
    public void deleteOwnedDocument(Long id, String requesterUid) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Document not found"));
        if (doc.getOwner() == null || !requesterUid.equals(doc.getOwner().getFirebaseUid())) {
            throw new ForbiddenException("Only the owner can delete this document");
        }
        // optional: also delete from GCS
        if (doc.getGcsPath() != null) {
            gcsService.deleteFile(gcsBucketName, doc.getGcsPath());
        }
        documentRepository.delete(doc);
    }

}
