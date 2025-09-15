package com.devops.stratusvault.dto;

import java.time.LocalDateTime;

public class DocumentDTO {
    private long id;
    private String fileName;
    private long originalSize;
    private long compressedSize;
    private LocalDateTime uploadTimeStamp;

    public DocumentDTO(long id, String fileName, long originalSize, long compressedSize, LocalDateTime uploadTimeStamp) {
        this.id = id;
        this.fileName = fileName;
        this.originalSize = originalSize;
        this.compressedSize = compressedSize;
        this.uploadTimeStamp = uploadTimeStamp;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getOriginalSize() {
        return originalSize;
    }

    public void setOriginalSize(long originalSize) {
        this.originalSize = originalSize;
    }

    public long getCompressedSize() {
        return compressedSize;
    }

    public void setCompressedSize(long compressedSize) {
        this.compressedSize = compressedSize;
    }

    public LocalDateTime getUploadTimeStamp() {
        return uploadTimeStamp;
    }

    public void setUploadTimeStamp(LocalDateTime uploadTimeStamp) {
        this.uploadTimeStamp = uploadTimeStamp;
    }

//    @Override
//    public String toString() {
//        return "DocumentDTO{" +
//                "id=" + id +
//                ", fileName='" + fileName + '\'' +
//                ", originalSize=" + originalSize +
//                ", compressedSize=" + compressedSize +
//                ", uploadTimeStamp=" + uploadTimeStamp +
//                '}';
//    }
}
