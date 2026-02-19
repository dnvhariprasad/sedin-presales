package com.sedin.presales.infrastructure.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlobStorageServiceTest {

    @Mock
    private BlobServiceClient blobServiceClient;

    @Mock
    private BlobContainerClient containerClient;

    @Mock
    private BlobClient blobClient;

    @InjectMocks
    private BlobStorageService blobStorageService;

    private void setupBlobClientChain(String container, String blobName) {
        when(blobServiceClient.getBlobContainerClient(container)).thenReturn(containerClient);
        when(containerClient.getBlobClient(blobName)).thenReturn(blobClient);
    }

    @Test
    @DisplayName("upload should upload blob and return URL")
    void upload_shouldUploadBlobAndReturnUrl() {
        String container = "test-container";
        String blobName = "test-blob.pdf";
        InputStream data = new ByteArrayInputStream("content".getBytes());
        long length = 7L;
        String contentType = "application/pdf";

        setupBlobClientChain(container, blobName);
        when(blobClient.getBlobUrl()).thenReturn("https://storage.blob.core.windows.net/test-container/test-blob.pdf");

        String result = blobStorageService.upload(container, blobName, data, length, contentType);

        assertThat(result).isEqualTo("https://storage.blob.core.windows.net/test-container/test-blob.pdf");
        verify(blobClient).upload(eq(data), eq(length), eq(true));
        verify(blobClient).setHttpHeaders(any(BlobHttpHeaders.class));
        verify(blobClient).getBlobUrl();
    }

    @Test
    @DisplayName("download should return InputStream from blob")
    void download_shouldReturnInputStream() {
        String container = "test-container";
        String blobName = "test-blob.pdf";
        InputStream mockStream = mock(InputStream.class);

        setupBlobClientChain(container, blobName);
        when(blobClient.openInputStream()).thenReturn(mock(com.azure.storage.blob.specialized.BlobInputStream.class));

        InputStream result = blobStorageService.download(container, blobName);

        assertThat(result).isNotNull();
        verify(blobClient).openInputStream();
    }

    @Test
    @DisplayName("delete should call deleteIfExists on blob client")
    void delete_shouldCallDeleteIfExists() {
        String container = "test-container";
        String blobName = "test-blob.pdf";

        setupBlobClientChain(container, blobName);

        blobStorageService.delete(container, blobName);

        verify(blobClient).deleteIfExists();
    }

    @Test
    @DisplayName("generateSasUrl should return URL with SAS token appended")
    void generateSasUrl_shouldReturnUrlWithSasToken() {
        String container = "test-container";
        String blobName = "test-blob.pdf";
        Duration validity = Duration.ofHours(1);

        setupBlobClientChain(container, blobName);
        when(blobClient.generateSas(any(BlobServiceSasSignatureValues.class))).thenReturn("token");
        when(blobClient.getBlobUrl()).thenReturn("https://blob.url");

        String result = blobStorageService.generateSasUrl(container, blobName, validity);

        assertThat(result).isEqualTo("https://blob.url?token");
    }

    @Test
    @DisplayName("exists should return true when blob exists")
    void exists_shouldReturnTrue() {
        String container = "test-container";
        String blobName = "test-blob.pdf";

        setupBlobClientChain(container, blobName);
        when(blobClient.exists()).thenReturn(true);

        boolean result = blobStorageService.exists(container, blobName);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("exists should return false when blob does not exist")
    void exists_shouldReturnFalse() {
        String container = "test-container";
        String blobName = "nonexistent-blob.pdf";

        setupBlobClientChain(container, blobName);
        when(blobClient.exists()).thenReturn(false);

        boolean result = blobStorageService.exists(container, blobName);

        assertThat(result).isFalse();
    }
}
