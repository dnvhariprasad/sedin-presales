package com.sedin.presales.application.service;

import com.sedin.presales.application.dto.CreateDocumentRequest;
import com.sedin.presales.application.dto.DocumentDetailDto;
import com.sedin.presales.application.dto.DocumentDto;
import com.sedin.presales.application.dto.DocumentVersionDto;
import com.sedin.presales.application.dto.IndexToggleResponseDto;
import com.sedin.presales.application.dto.UpdateDocumentRequest;
import com.sedin.presales.application.exception.BadRequestException;
import com.sedin.presales.application.exception.ResourceNotFoundException;
import com.sedin.presales.application.mapper.DocumentMapper;
import com.sedin.presales.domain.entity.Document;
import com.sedin.presales.domain.entity.DocumentType;
import com.sedin.presales.domain.entity.DocumentVersion;
import com.sedin.presales.domain.enums.DocumentStatus;
import com.sedin.presales.domain.repository.BusinessUnitRepository;
import com.sedin.presales.domain.repository.DocumentMetadataRepository;
import com.sedin.presales.domain.repository.DocumentRepository;
import com.sedin.presales.domain.repository.DocumentTypeRepository;
import com.sedin.presales.domain.repository.DocumentVersionRepository;
import com.sedin.presales.domain.repository.DomainRepository;
import com.sedin.presales.domain.repository.FolderRepository;
import com.sedin.presales.domain.repository.IndustryRepository;
import com.sedin.presales.domain.repository.RenditionRepository;
import com.sedin.presales.domain.repository.SbuRepository;
import com.sedin.presales.domain.repository.TechnologyRepository;
import com.sedin.presales.config.CurrentUserService;
import com.sedin.presales.infrastructure.storage.BlobStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.sedin.presales.application.dto.PagedResponse;
import com.sedin.presales.config.UserPrincipal;
import com.sedin.presales.domain.enums.Permission;
import com.sedin.presales.domain.enums.ResourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentMetadataRepository documentMetadataRepository;

    @Mock
    private DocumentVersionRepository documentVersionRepository;

    @Mock
    private DocumentTypeRepository documentTypeRepository;

    @Mock
    private FolderRepository folderRepository;

    @Mock
    private DomainRepository domainRepository;

    @Mock
    private IndustryRepository industryRepository;

    @Mock
    private TechnologyRepository technologyRepository;

    @Mock
    private BusinessUnitRepository businessUnitRepository;

    @Mock
    private SbuRepository sbuRepository;

    @Mock
    private RenditionRepository renditionRepository;

    @Mock
    private BlobStorageService blobStorageService;

    @Mock
    private DocumentMapper documentMapper;

    @Mock
    private RenditionService renditionService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AclService aclService;

    @Mock
    private IndexingService indexingService;

    @InjectMocks
    private DocumentService documentService;

    @Test
    @DisplayName("upload should create document and version")
    void upload_shouldCreateDocumentAndVersion() throws IOException {
        UUID docTypeId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();

        DocumentType documentType = DocumentType.builder()
                .name("Proposal")
                .description("Proposal type")
                .isActive(true)
                .build();
        documentType.setId(docTypeId);

        CreateDocumentRequest request = CreateDocumentRequest.builder()
                .title("Test Document")
                .customerName("Acme Corp")
                .documentDate(LocalDate.now())
                .documentTypeId(docTypeId)
                .build();

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("test.pdf");
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        Document savedDocument = Document.builder()
                .title("Test Document")
                .customerName("Acme Corp")
                .status(DocumentStatus.ACTIVE)
                .currentVersionNumber(1)
                .documentType(documentType)
                .build();
        savedDocument.setId(docId);
        savedDocument.setCreatedAt(Instant.now());
        savedDocument.setUpdatedAt(Instant.now());

        DocumentDto documentDto = DocumentDto.builder()
                .id(docId)
                .title("Test Document")
                .status(DocumentStatus.ACTIVE)
                .build();

        DocumentVersion savedVersion = DocumentVersion.builder()
                .document(savedDocument)
                .versionNumber(1)
                .filePath("documents/" + docId + "/1/test.pdf")
                .fileName("test.pdf")
                .fileSize(1024L)
                .contentType("application/pdf")
                .build();
        savedVersion.setId(UUID.randomUUID());

        when(documentTypeRepository.findById(docTypeId)).thenReturn(Optional.of(documentType));
        when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);
        when(blobStorageService.upload(anyString(), anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenReturn("https://blob.storage/documents/test.pdf");
        when(documentVersionRepository.save(any(DocumentVersion.class))).thenReturn(savedVersion);
        when(documentMapper.toDto(savedDocument)).thenReturn(documentDto);

        DocumentDto result = documentService.upload(file, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(docId);
        assertThat(result.getTitle()).isEqualTo("Test Document");
        verify(documentRepository).save(any(Document.class));
        verify(documentVersionRepository).save(any(DocumentVersion.class));
        verify(blobStorageService).upload(anyString(), anyString(), any(InputStream.class), anyLong(), anyString());
    }

    @Test
    @DisplayName("upload should throw BadRequestException when file is empty")
    void upload_shouldThrowWhenFileIsEmpty() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        CreateDocumentRequest request = CreateDocumentRequest.builder()
                .title("Test")
                .documentTypeId(UUID.randomUUID())
                .build();

        assertThatThrownBy(() -> documentService.upload(file, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("File is required");
    }

    @Test
    @DisplayName("upload should throw ResourceNotFoundException when document type not found")
    void upload_shouldThrowWhenDocumentTypeNotFound() {
        UUID docTypeId = UUID.randomUUID();

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);

        CreateDocumentRequest request = CreateDocumentRequest.builder()
                .title("Test")
                .documentTypeId(docTypeId)
                .build();

        when(documentTypeRepository.findById(docTypeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.upload(file, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getById should return DocumentDetailDto when document exists")
    void getById_shouldReturnDetailDto() {
        UUID id = UUID.randomUUID();
        Document document = Document.builder()
                .title("Test Document")
                .status(DocumentStatus.ACTIVE)
                .build();
        document.setId(id);

        DocumentDetailDto detailDto = DocumentDetailDto.builder()
                .id(id)
                .title("Test Document")
                .status(DocumentStatus.ACTIVE)
                .build();

        when(documentRepository.findById(id)).thenReturn(Optional.of(document));
        when(documentMapper.toDetailDto(document)).thenReturn(detailDto);

        DocumentDetailDto result = documentService.getById(id);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getTitle()).isEqualTo("Test Document");
    }

    @Test
    @DisplayName("getById should throw ResourceNotFoundException when document not found")
    void getById_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(documentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private void mockAdminUser() {
        UserPrincipal admin = UserPrincipal.builder()
                .userId(UUID.randomUUID().toString())
                .email("admin@test.com")
                .role("ADMIN")
                .build();
        when(currentUserService.getCurrentUser()).thenReturn(admin);
    }

    @Test
    @DisplayName("update should update document fields partially")
    void update_shouldUpdateFields() {
        mockAdminUser();
        UUID id = UUID.randomUUID();
        Document document = Document.builder()
                .title("Old Title")
                .customerName("Old Customer")
                .status(DocumentStatus.ACTIVE)
                .build();
        document.setId(id);
        document.setCreatedAt(Instant.now());
        document.setUpdatedAt(Instant.now());

        UpdateDocumentRequest request = UpdateDocumentRequest.builder()
                .title("New Title")
                .build();

        DocumentDto documentDto = DocumentDto.builder()
                .id(id)
                .title("New Title")
                .build();

        when(documentRepository.findById(id)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(documentMapper.toDto(document)).thenReturn(documentDto);

        DocumentDto result = documentService.update(id, request);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("New Title");
        verify(documentRepository).save(any(Document.class));
    }

    @Test
    @DisplayName("delete should set document status to ARCHIVED")
    void delete_shouldSetStatusToArchived() {
        mockAdminUser();
        UUID id = UUID.randomUUID();
        Document document = Document.builder()
                .title("To Be Archived")
                .status(DocumentStatus.ACTIVE)
                .build();
        document.setId(id);

        when(documentRepository.findById(id)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(document);

        documentService.delete(id);

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.ARCHIVED);
        verify(documentRepository).save(document);
    }

    @Test
    @DisplayName("uploadNewVersion should increment version number")
    void uploadNewVersion_shouldIncrementVersionNumber() throws IOException {
        mockAdminUser();
        UUID docId = UUID.randomUUID();
        Document document = Document.builder()
                .title("Test Document")
                .status(DocumentStatus.ACTIVE)
                .currentVersionNumber(2)
                .build();
        document.setId(docId);

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("updated.pdf");
        when(file.getSize()).thenReturn(2048L);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        DocumentVersion savedVersion = DocumentVersion.builder()
                .document(document)
                .versionNumber(3)
                .filePath("documents/" + docId + "/3/updated.pdf")
                .fileName("updated.pdf")
                .fileSize(2048L)
                .contentType("application/pdf")
                .build();
        savedVersion.setId(UUID.randomUUID());

        DocumentVersionDto versionDto = DocumentVersionDto.builder()
                .id(savedVersion.getId())
                .versionNumber(3)
                .fileName("updated.pdf")
                .build();

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(blobStorageService.upload(anyString(), anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenReturn("https://blob.storage/documents/updated.pdf");
        when(documentVersionRepository.save(any(DocumentVersion.class))).thenReturn(savedVersion);
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(documentMapper.toVersionDto(savedVersion)).thenReturn(versionDto);

        DocumentVersionDto result = documentService.uploadNewVersion(docId, file, "Updated content");

        assertThat(result).isNotNull();
        assertThat(result.getVersionNumber()).isEqualTo(3);
        assertThat(document.getCurrentVersionNumber()).isEqualTo(3);
        verify(documentVersionRepository).save(any(DocumentVersion.class));
    }

    @Test
    @DisplayName("uploadNewVersion should throw BadRequestException when file is empty")
    void uploadNewVersion_shouldThrowWhenFileIsEmpty() {
        UUID docId = UUID.randomUUID();
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> documentService.uploadNewVersion(docId, file, "notes"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("File is required");
    }

    @Test
    @DisplayName("getVersions should return list of version DTOs")
    void getVersions_shouldReturnVersionList() {
        UUID docId = UUID.randomUUID();

        DocumentVersion v1 = DocumentVersion.builder()
                .versionNumber(1)
                .fileName("v1.pdf")
                .build();
        v1.setId(UUID.randomUUID());

        DocumentVersion v2 = DocumentVersion.builder()
                .versionNumber(2)
                .fileName("v2.pdf")
                .build();
        v2.setId(UUID.randomUUID());

        DocumentVersionDto dto1 = DocumentVersionDto.builder()
                .versionNumber(1)
                .fileName("v1.pdf")
                .build();

        DocumentVersionDto dto2 = DocumentVersionDto.builder()
                .versionNumber(2)
                .fileName("v2.pdf")
                .build();

        when(documentRepository.existsById(docId)).thenReturn(true);
        when(documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(docId))
                .thenReturn(List.of(v2, v1));
        when(documentMapper.toVersionDto(v2)).thenReturn(dto2);
        when(documentMapper.toVersionDto(v1)).thenReturn(dto1);

        List<DocumentVersionDto> result = documentService.getVersions(docId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getVersionNumber()).isEqualTo(2);
        assertThat(result.get(1).getVersionNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("getVersions should throw ResourceNotFoundException when document not found")
    void getVersions_shouldThrowWhenDocumentNotFound() {
        UUID docId = UUID.randomUUID();
        when(documentRepository.existsById(docId)).thenReturn(false);

        assertThatThrownBy(() -> documentService.getVersions(docId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("downloadVersion should return input stream")
    void downloadVersion_shouldReturnInputStream() {
        UUID docId = UUID.randomUUID();
        int versionNumber = 1;
        String filePath = "documents/" + docId + "/1/file.pdf";

        DocumentVersion version = DocumentVersion.builder()
                .document(Document.builder().build())
                .versionNumber(versionNumber)
                .filePath(filePath)
                .fileName("file.pdf")
                .build();
        version.setId(UUID.randomUUID());

        InputStream expectedStream = new ByteArrayInputStream("file-content".getBytes());

        when(documentVersionRepository.findByDocumentIdAndVersionNumber(docId, versionNumber))
                .thenReturn(Optional.of(version));
        when(blobStorageService.download("documents", filePath)).thenReturn(expectedStream);

        InputStream result = documentService.downloadVersion(docId, versionNumber);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expectedStream);
        verify(blobStorageService).download("documents", filePath);
    }

    @Test
    @DisplayName("list should return all documents for admin user without ACL filtering")
    void list_shouldReturnAllDocumentsForAdmin() {
        Pageable pageable = PageRequest.of(0, 10);

        UserPrincipal adminUser = UserPrincipal.builder()
                .userId(UUID.randomUUID().toString())
                .email("admin@test.com")
                .displayName("Admin")
                .role("ADMIN")
                .build();
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);

        Document doc = Document.builder()
                .title("Admin Document")
                .status(DocumentStatus.ACTIVE)
                .build();
        doc.setId(UUID.randomUUID());

        DocumentDto dto = DocumentDto.builder()
                .id(doc.getId())
                .title("Admin Document")
                .status(DocumentStatus.ACTIVE)
                .build();

        Page<Document> page = new PageImpl<>(List.of(doc), pageable, 1);
        when(documentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(documentMapper.toDto(doc)).thenReturn(dto);

        PagedResponse<DocumentDto> result = documentService.list(pageable, null, null, null, null);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Admin Document");
        // Admin should NOT trigger ACL lookup
        verify(aclService, never()).getAccessibleResourceIds(any(UUID.class), any(ResourceType.class), any(Permission.class));
    }

    @Test
    @DisplayName("list should filter by ACL for non-admin user")
    void list_shouldFilterByAclForNonAdminUser() {
        Pageable pageable = PageRequest.of(0, 10);
        UUID userId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();

        UserPrincipal editorUser = UserPrincipal.builder()
                .userId(userId.toString())
                .email("editor@test.com")
                .displayName("Editor")
                .role("EDITOR")
                .build();
        when(currentUserService.getCurrentUser()).thenReturn(editorUser);

        Set<UUID> accessibleIds = Set.of(docId);
        when(aclService.getAccessibleResourceIds(userId, ResourceType.DOCUMENT, Permission.READ))
                .thenReturn(accessibleIds);

        Document doc = Document.builder()
                .title("Editor Document")
                .status(DocumentStatus.ACTIVE)
                .build();
        doc.setId(docId);

        DocumentDto dto = DocumentDto.builder()
                .id(docId)
                .title("Editor Document")
                .status(DocumentStatus.ACTIVE)
                .build();

        Page<Document> page = new PageImpl<>(List.of(doc), pageable, 1);
        when(documentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(documentMapper.toDto(doc)).thenReturn(dto);

        PagedResponse<DocumentDto> result = documentService.list(pageable, null, null, null, null);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(aclService).getAccessibleResourceIds(userId, ResourceType.DOCUMENT, Permission.READ);
        verify(documentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("list should return empty for non-admin with no access")
    void list_shouldReturnEmptyForNonAdminWithNoAccess() {
        Pageable pageable = PageRequest.of(0, 10);
        UUID userId = UUID.randomUUID();

        UserPrincipal viewerUser = UserPrincipal.builder()
                .userId(userId.toString())
                .email("viewer@test.com")
                .displayName("Viewer")
                .role("VIEWER")
                .build();
        when(currentUserService.getCurrentUser()).thenReturn(viewerUser);

        when(aclService.getAccessibleResourceIds(userId, ResourceType.DOCUMENT, Permission.READ))
                .thenReturn(Collections.emptySet());

        PagedResponse<DocumentDto> result = documentService.list(pageable, null, null, null, null);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getTotalPages()).isEqualTo(0);
        assertThat(result.isLast()).isTrue();
        // Should NOT hit the database when user has no access
        verify(documentRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("toggleRagIndex should enable indexing and trigger async indexDocument")
    void toggleRagIndex_shouldEnableIndexingAndTriggerAsync() {
        UUID id = UUID.randomUUID();
        Document document = Document.builder()
                .title("Test Document")
                .status(DocumentStatus.ACTIVE)
                .ragIndexed(false)
                .build();
        document.setId(id);

        when(documentRepository.findById(id)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(document);

        IndexToggleResponseDto result = documentService.toggleRagIndex(id);

        assertThat(result).isNotNull();
        assertThat(result.getDocumentId()).isEqualTo(id);
        assertThat(result.isRagIndexed()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Document queued for indexing");
        verify(indexingService).indexDocument(id);
        verify(indexingService, never()).removeFromIndex(any(UUID.class));
    }

    @Test
    @DisplayName("toggleRagIndex should disable indexing and remove from index")
    void toggleRagIndex_shouldDisableIndexingAndRemoveFromIndex() {
        UUID id = UUID.randomUUID();
        Document document = Document.builder()
                .title("Test Document")
                .status(DocumentStatus.ACTIVE)
                .ragIndexed(true)
                .build();
        document.setId(id);

        when(documentRepository.findById(id)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(document);

        IndexToggleResponseDto result = documentService.toggleRagIndex(id);

        assertThat(result).isNotNull();
        assertThat(result.getDocumentId()).isEqualTo(id);
        assertThat(result.isRagIndexed()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Document removed from search index");
        verify(indexingService).removeFromIndex(id);
        verify(indexingService, never()).indexDocument(any(UUID.class));
    }

    @Test
    @DisplayName("toggleRagIndex should throw when document not found")
    void toggleRagIndex_shouldThrowWhenDocumentNotFound() {
        UUID id = UUID.randomUUID();
        when(documentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.toggleRagIndex(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("uploadNewVersion should re-index when document is RAG indexed")
    void uploadNewVersion_shouldReindexWhenRagIndexed() throws IOException {
        mockAdminUser();
        UUID docId = UUID.randomUUID();
        Document document = Document.builder()
                .title("Test Document")
                .status(DocumentStatus.ACTIVE)
                .currentVersionNumber(1)
                .ragIndexed(true)
                .build();
        document.setId(docId);

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("updated.pdf");
        when(file.getSize()).thenReturn(2048L);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        DocumentVersion savedVersion = DocumentVersion.builder()
                .document(document)
                .versionNumber(2)
                .filePath("documents/" + docId + "/2/updated.pdf")
                .fileName("updated.pdf")
                .fileSize(2048L)
                .contentType("application/pdf")
                .build();
        savedVersion.setId(UUID.randomUUID());

        DocumentVersionDto versionDto = DocumentVersionDto.builder()
                .id(savedVersion.getId())
                .versionNumber(2)
                .fileName("updated.pdf")
                .build();

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(blobStorageService.upload(anyString(), anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenReturn("https://blob.storage/documents/updated.pdf");
        when(documentVersionRepository.save(any(DocumentVersion.class))).thenReturn(savedVersion);
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(documentMapper.toVersionDto(savedVersion)).thenReturn(versionDto);

        documentService.uploadNewVersion(docId, file, "Updated content");

        verify(indexingService).indexDocument(docId);
    }
}
