package com.devops.stratusvault.controller;

import com.devops.stratusvault.dto.DocumentMapper;
import com.devops.stratusvault.dto.DocumentResponseDTO;
import com.devops.stratusvault.dto.ShareRequestDTO;
import com.devops.stratusvault.model.Document;
import com.devops.stratusvault.service.DocumentService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

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
            String uid = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            UserRecord decodedToken = FirebaseAuth.getInstance().getUser(uid);
            Document saved = documentService.uploadDocument(file, uid, decodedToken.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(DocumentMapper.toResponse(saved));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing file: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error authenticating user: " + e.getMessage());
        }
    }

    @GetMapping
    public List<DocumentResponseDTO> list() {
        String uid = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<Document> docs = documentService.findForUser(uid);
        return docs.stream().map(DocumentMapper::toResponse).toList();
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

    @PostMapping(path = "/{id}/share")
    public ResponseEntity<?> shareDocument(@PathVariable long id, @RequestBody ShareRequestDTO shareRequestDTO) {
        try{
            System.out.println("Share Yeyyy ::::::: " + shareRequestDTO);
            String firebaseUid = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            documentService.shareDocument(id, firebaseUid, shareRequestDTO.getEmail());
            return ResponseEntity.ok().body(java.util.Map.of("message", "Document shared successfully"));
        } catch (Exception e) {
            System.out.println("Error over here!!");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        String uid = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        documentService.deleteOwnedDocument(id, uid);
        return ResponseEntity.noContent().build();
    }
}
