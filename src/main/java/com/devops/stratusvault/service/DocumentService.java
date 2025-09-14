package com.devops.stratusvault.service;

import com.devops.stratusvault.model.Document;
import com.devops.stratusvault.model.User;
import com.devops.stratusvault.repository.DocumentRepository;
import com.devops.stratusvault.repository.UserRepository;
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

    @Value("${app.gcs.bucket-name}")
    private String gcsBucketName;

    public record DownloadableFile(byte[] data, String fileName, String contentType) {}

    public DocumentService(DocumentRepository documentRepository, UserRepository userRepository, GcsService gcsService) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.gcsService = gcsService;
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
        return documentRepository.findByOwner_FirebaseUid(firebaseUid);
    }

    public Optional<DownloadableFile> downloadDocument(long documentId, String firebaseUid) throws IOException {
        Optional<Document> documentOptional = documentRepository.findById(documentId);

        // 1. Check if document exists
        if (documentOptional.isEmpty()) {
            return Optional.empty();
        }
        Document document = documentOptional.get();

        // 2. CRITICAL SECURITY CHECK: Verify the user owns this document
        if (!document.getOwner().getFirebaseUid().equals(firebaseUid)) {
            return Optional.empty(); // User is not the owner, act as if file doesn't exist
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
}
