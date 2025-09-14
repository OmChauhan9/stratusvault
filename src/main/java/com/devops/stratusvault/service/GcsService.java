package com.devops.stratusvault.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.stereotype.Service;

@Service
public class GcsService {

    private final Storage storage;

    public GcsService() {
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

    public void uploadFile(byte[] fileBytes, String bucketName, String destinationObjectName, String contentType) {
        BlobId blobId = BlobId.of(bucketName, destinationObjectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build();
        storage.create(blobInfo, fileBytes);
    }

    public byte[] downloadFile(String bucketName, String objectName) {
        BlobId blobId = BlobId.of(bucketName, objectName);
        return storage.readAllBytes(BlobId.of(bucketName, objectName));
    }
}
