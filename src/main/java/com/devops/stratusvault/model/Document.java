package com.devops.stratusvault.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String fileName;
    private String gcsPath;
    private long originalSize;
    private long compressedSize;
    private LocalDateTime uploadTimeStamp;
    private String contentType;

    @ManyToOne(fetch = FetchType.EAGER)
    private User owner;

    public Document(long id, String fileName, String gcsPath, long originalSize, long compressedSize, LocalDateTime uploadTimeStamp, User owner) {
        this.id = id;
        this.fileName = fileName;
        this.gcsPath = gcsPath;
        this.originalSize = originalSize;
        this.compressedSize = compressedSize;
        this.uploadTimeStamp = uploadTimeStamp;
        this.owner = owner;
        this.contentType = contentType;
    }

    public Document() {

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

    public String getGcsPath() {
        return gcsPath;
    }

    public void setGcsPath(String gcsPath) {
        this.gcsPath = gcsPath;
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

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

//    @Override
//    public String toString() {
//        return "Document{" +
//                "id=" + id +
//                ", fileName='" + fileName + '\'' +
//                ", gcsPath='" + gcsPath + '\'' +
//                ", originalSize=" + originalSize +
//                ", compressedSize=" + compressedSize +
//                ", uploadTimeStamp=" + uploadTimeStamp +
//                ", contentType='" + contentType + '\'' +
//                ", owner=" + owner +
//                '}';
//    }
}
