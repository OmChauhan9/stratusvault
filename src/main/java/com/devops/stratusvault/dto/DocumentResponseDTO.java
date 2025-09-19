package com.devops.stratusvault.dto;

import java.time.LocalDateTime;

public record DocumentResponseDTO(Long id,
                                  String fileName,
                                  String gcsPath,
                                  Long originalSize,
                                  Long compressedSize,
                                  String contentType,
                                  LocalDateTime uploadTimeStamp,
                                  UserResponseDTO owner) {
}
