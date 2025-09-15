package com.devops.stratusvault.controller;

import com.devops.stratusvault.dto.DocumentDTO;
import com.devops.stratusvault.dto.ShareRequestDTO;
import com.devops.stratusvault.model.Document;
import com.devops.stratusvault.service.DocumentService;
import com.devops.stratusvault.service.GcsService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> fileUpload(@RequestParam("file") MultipartFile file) {
        try {
            String fireBaseUid = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            UserRecord decodedToken = FirebaseAuth.getInstance().getUser(fireBaseUid);
            String email = decodedToken.getEmail();

            Document savedDocument = documentService.uploadDocument(file,fireBaseUid,email);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedDocument);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing file: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error authenticating user: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<DocumentDTO>> getDocumentsForUser() {
        String firebaseUid = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<Document> documents = documentService.getDocumentsForUser(firebaseUid);

        List<DocumentDTO> dtos = documents.stream()
                .map(doc -> new DocumentDTO(doc.getId(), doc.getFileName(), doc.getOriginalSize(), doc.getCompressedSize(), doc.getUploadTimeStamp()))
                .collect(Collectors.toList());
        System.out.println("DTOSSS :::: " + dtos);
        System.out.println("Documents :::: " + documents);

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable long id) {
        try {
            String firebaseUid = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            // The service now handles the security check and decompression
            Optional<DocumentService.DownloadableFile> downloadableFileOptional = documentService.downloadDocument(id, firebaseUid);

            if (downloadableFileOptional.isEmpty()) {
                // If the optional is empty, it means the file doesn't exist OR the user doesn't own it.
                // In either case, return a 404 Not Found for security.
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            DocumentService.DownloadableFile file = downloadableFileOptional.get();

            HttpHeaders headers = new HttpHeaders();
            // This header tells the browser to prompt a download with the original filename
            headers.setContentDisposition(ContentDisposition.attachment().filename(file.fileName()).build());
            // Handle cases where contentType might be null
            if (file.contentType() != null && !file.contentType().isBlank()) {
                headers.setContentType(MediaType.parseMediaType(file.contentType()));
            } else {
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM); // A generic default
            }

            return new ResponseEntity<>(file.data(), headers, HttpStatus.OK);

        } catch (IOException e) {
            e.printStackTrace(); // Good for server-side logging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<?> shareDocument(@PathVariable long id, @RequestBody ShareRequestDTO shareRequestDTO) {
        try{
            String firebaseUid = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String email = shareRequestDTO.getEmail();
            documentService.shareDocument(id, firebaseUid, email);
            return ResponseEntity.ok(java.util.Map.of("message", "Document shared successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
