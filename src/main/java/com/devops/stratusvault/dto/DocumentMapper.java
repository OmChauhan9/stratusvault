package com.devops.stratusvault.dto;

import com.devops.stratusvault.model.Document;
import com.devops.stratusvault.model.User;

public final class DocumentMapper {
    private DocumentMapper() {}

    public static DocumentResponseDTO toResponse(Document d) {
        if (d == null) return null;
        User u = d.getOwner();
        UserResponseDTO owner = (u == null) ? null : new UserResponseDTO(u.getFirebaseUid(), u.getEmail());
        return new DocumentResponseDTO(
                d.getId(),
                d.getFileName(),
                d.getGcsPath(),
                d.getOriginalSize(),
                d.getCompressedSize(),
                d.getContentType(),
                d.getUploadTimeStamp(),
                owner
        );
    }
}