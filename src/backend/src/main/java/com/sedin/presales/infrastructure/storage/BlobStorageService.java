package com.sedin.presales.infrastructure.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Duration;
import java.time.OffsetDateTime;

@Slf4j
@Service
public class BlobStorageService {

    private final BlobServiceClient blobServiceClient;

    public BlobStorageService(BlobServiceClient blobServiceClient) {
        this.blobServiceClient = blobServiceClient;
    }

    public String upload(String container, String blobName, InputStream data, long length, String contentType) {
        log.info("Uploading blob '{}' to container '{}'", blobName, container);
        BlobClient blobClient = getBlobClient(container, blobName);
        BlobHttpHeaders headers = new BlobHttpHeaders().setContentType(contentType);
        blobClient.upload(data, length, true);
        blobClient.setHttpHeaders(headers);
        return blobClient.getBlobUrl();
    }

    public InputStream download(String container, String blobName) {
        log.info("Downloading blob '{}' from container '{}'", blobName, container);
        BlobClient blobClient = getBlobClient(container, blobName);
        return blobClient.openInputStream();
    }

    public void delete(String container, String blobName) {
        log.info("Deleting blob '{}' from container '{}'", blobName, container);
        BlobClient blobClient = getBlobClient(container, blobName);
        blobClient.deleteIfExists();
    }

    public String generateSasUrl(String container, String blobName, Duration validity) {
        log.info("Generating SAS URL for blob '{}' in container '{}' with validity {}", blobName, container, validity);
        BlobClient blobClient = getBlobClient(container, blobName);

        BlobSasPermission permissions = new BlobSasPermission().setReadPermission(true);
        BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(
                OffsetDateTime.now().plus(validity), permissions);

        String sasToken = blobClient.generateSas(sasValues);
        return blobClient.getBlobUrl() + "?" + sasToken;
    }

    public boolean exists(String container, String blobName) {
        BlobClient blobClient = getBlobClient(container, blobName);
        return blobClient.exists();
    }

    private BlobClient getBlobClient(String container, String blobName) {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(container);
        return containerClient.getBlobClient(blobName);
    }
}
