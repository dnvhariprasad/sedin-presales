package com.sedin.presales.application.service;

import com.sedin.presales.application.dto.CompareViewDto;
import com.sedin.presales.application.dto.CreateDocumentMetadataRequest;
import com.sedin.presales.application.dto.CreateDocumentRequest;
import com.sedin.presales.application.dto.DocumentDetailDto;
import com.sedin.presales.application.dto.DocumentDownloadDto;
import com.sedin.presales.application.dto.DocumentDto;
import com.sedin.presales.application.dto.DocumentVersionDto;
import com.sedin.presales.application.dto.DocumentViewDto;
import com.sedin.presales.application.dto.IndexToggleResponseDto;
import com.sedin.presales.application.dto.UpdateDocumentRequest;
import com.sedin.presales.application.exception.AccessDeniedException;
import com.sedin.presales.application.exception.BadRequestException;
import com.sedin.presales.application.exception.ResourceNotFoundException;
import com.sedin.presales.application.mapper.DocumentMapper;
import com.sedin.presales.domain.entity.Document;
import com.sedin.presales.domain.entity.DocumentMetadata;
import com.sedin.presales.domain.entity.DocumentType;
import com.sedin.presales.domain.entity.DocumentVersion;
import com.sedin.presales.domain.entity.Domain;
import com.sedin.presales.domain.entity.Industry;
import com.sedin.presales.domain.entity.Rendition;
import com.sedin.presales.domain.entity.Technology;
import com.sedin.presales.domain.enums.DocumentStatus;
import com.sedin.presales.domain.enums.RenditionStatus;
import com.sedin.presales.domain.enums.RenditionType;
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
import java.time.Duration;
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

    @Mock
    private CaseStudyValidationService caseStudyValidationService;

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
        mockAdminUser();
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
        mockAdminUser();
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
        DocumentType proposalType = DocumentType.builder()
                .name("Proposal")
                .isActive(true)
                .build();
        Document document = Document.builder()
                .title("Test Document")
                .status(DocumentStatus.ACTIVE)
                .currentVersionNumber(2)
                .documentType(proposalType)
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
        mockAdminUser();
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
        mockAdminUser();
        UUID docId = UUID.randomUUID();
        when(documentRepository.existsById(docId)).thenReturn(false);

        assertThatThrownBy(() -> documentService.getVersions(docId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("downloadVersion should return input stream")
    void downloadVersion_shouldReturnInputStream() {
        mockAdminUser();
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

        IndexToggleResponseDto result = documentService.toggleRagIndex(id);

        assertThat(result).isNotNull();
        assertThat(result.getDocumentId()).isEqualTo(id);
        assertThat(result.isRagIndexed()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Document queued for indexing");
        verify(indexingService).indexDocument(id);
        verify(indexingService, never()).removeFromIndex(any(UUID.class));
        // ragIndexed is NOT set eagerly — IndexingService sets it on success
        verify(documentRepository, never()).save(any(Document.class));
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
        DocumentType proposalType = DocumentType.builder()
                .name("Proposal")
                .isActive(true)
                .build();
        Document document = Document.builder()
                .title("Test Document")
                .status(DocumentStatus.ACTIVE)
                .currentVersionNumber(1)
                .ragIndexed(true)
                .documentType(proposalType)
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

    // ==================== Testing Gap 1: Negative ACL tests for write operations ====================

    @Test
    @DisplayName("update should throw AccessDeniedException when non-admin user lacks WRITE permission")
    void update_shouldThrowAccessDenied_whenUserLacksWritePermission() {
        UUID docId = UUID.randomUUID();
        UserPrincipal viewer = UserPrincipal.builder()
                .userId(UUID.randomUUID().toString())
                .email("viewer@sedin.com")
                .role("VIEWER")
                .build();
        when(currentUserService.getCurrentUser()).thenReturn(viewer);
        when(aclService.hasPermission(any(UUID.class), eq(ResourceType.DOCUMENT), eq(docId), eq(Permission.WRITE)))
                .thenReturn(false);

        UpdateDocumentRequest request = UpdateDocumentRequest.builder().title("New Title").build();

        assertThatThrownBy(() -> documentService.update(docId, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("delete should throw AccessDeniedException when non-admin user lacks WRITE permission")
    void delete_shouldThrowAccessDenied_whenUserLacksWritePermission() {
        UUID docId = UUID.randomUUID();
        UserPrincipal viewer = UserPrincipal.builder()
                .userId(UUID.randomUUID().toString())
                .email("viewer@sedin.com")
                .role("VIEWER")
                .build();
        when(currentUserService.getCurrentUser()).thenReturn(viewer);
        when(aclService.hasPermission(any(UUID.class), eq(ResourceType.DOCUMENT), eq(docId), eq(Permission.WRITE)))
                .thenReturn(false);

        assertThatThrownBy(() -> documentService.delete(docId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("uploadNewVersion should throw AccessDeniedException when non-admin user lacks WRITE permission")
    void uploadNewVersion_shouldThrowAccessDenied_whenUserLacksWritePermission() {
        UUID docId = UUID.randomUUID();
        UserPrincipal viewer = UserPrincipal.builder()
                .userId(UUID.randomUUID().toString())
                .email("viewer@sedin.com")
                .role("VIEWER")
                .build();
        when(currentUserService.getCurrentUser()).thenReturn(viewer);

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);

        when(aclService.hasPermission(any(UUID.class), eq(ResourceType.DOCUMENT), eq(docId), eq(Permission.WRITE)))
                .thenReturn(false);

        assertThatThrownBy(() -> documentService.uploadNewVersion(docId, file, "notes"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("update should succeed for ADMIN user without explicit ACL entry (admin bypass)")
    void update_shouldSucceedForAdmin_withoutExplicitAclEntry() {
        mockAdminUser();
        UUID id = UUID.randomUUID();
        Document document = Document.builder()
                .title("Old Title")
                .status(DocumentStatus.ACTIVE)
                .build();
        document.setId(id);
        document.setCreatedAt(Instant.now());
        document.setUpdatedAt(Instant.now());

        UpdateDocumentRequest request = UpdateDocumentRequest.builder()
                .title("Admin Updated Title")
                .build();

        DocumentDto documentDto = DocumentDto.builder()
                .id(id)
                .title("Admin Updated Title")
                .build();

        when(documentRepository.findById(id)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(documentMapper.toDto(document)).thenReturn(documentDto);

        DocumentDto result = documentService.update(id, request);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Admin Updated Title");
        // Admin should NOT trigger ACL permission check
        verify(aclService, never()).hasPermission(any(UUID.class), any(ResourceType.class), any(UUID.class), any(Permission.class));
    }

    // ==================== Testing Gap 2: Metadata-path tests for populateMetadata ====================

    @Test
    @DisplayName("upload with valid metadata should save document metadata")
    void upload_withValidMetadata_shouldSaveDocumentMetadata() throws IOException {
        UUID docTypeId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        UUID domainId = UUID.randomUUID();
        UUID industryId = UUID.randomUUID();
        UUID techId1 = UUID.randomUUID();
        UUID techId2 = UUID.randomUUID();

        DocumentType documentType = DocumentType.builder()
                .name("Proposal")
                .isActive(true)
                .build();
        documentType.setId(docTypeId);

        Domain domain = Domain.builder().name("Cloud").build();
        domain.setId(domainId);

        Industry industry = Industry.builder().name("Healthcare").build();
        industry.setId(industryId);

        Technology tech1 = Technology.builder().name("Java").build();
        tech1.setId(techId1);
        Technology tech2 = Technology.builder().name("React").build();
        tech2.setId(techId2);

        CreateDocumentMetadataRequest metadataRequest = CreateDocumentMetadataRequest.builder()
                .domainId(domainId)
                .industryId(industryId)
                .technologyIds(List.of(techId1, techId2))
                .build();

        CreateDocumentRequest request = CreateDocumentRequest.builder()
                .title("Test Document")
                .customerName("Acme Corp")
                .documentDate(LocalDate.now())
                .documentTypeId(docTypeId)
                .metadata(metadataRequest)
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

        DocumentVersion savedVersion = DocumentVersion.builder()
                .document(savedDocument)
                .versionNumber(1)
                .filePath("documents/" + docId + "/1/test.pdf")
                .fileName("test.pdf")
                .fileSize(1024L)
                .contentType("application/pdf")
                .build();
        savedVersion.setId(UUID.randomUUID());

        DocumentDto documentDto = DocumentDto.builder()
                .id(docId)
                .title("Test Document")
                .status(DocumentStatus.ACTIVE)
                .build();

        when(documentTypeRepository.findById(docTypeId)).thenReturn(Optional.of(documentType));
        when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);
        when(blobStorageService.upload(anyString(), anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenReturn("https://blob.storage/documents/test.pdf");
        when(documentVersionRepository.save(any(DocumentVersion.class))).thenReturn(savedVersion);
        when(documentMapper.toDto(savedDocument)).thenReturn(documentDto);
        when(domainRepository.findById(domainId)).thenReturn(Optional.of(domain));
        when(industryRepository.findById(industryId)).thenReturn(Optional.of(industry));
        when(technologyRepository.findAllById(List.of(techId1, techId2))).thenReturn(List.of(tech1, tech2));

        DocumentDto result = documentService.upload(file, request);

        assertThat(result).isNotNull();
        verify(documentMetadataRepository).save(any(DocumentMetadata.class));
    }

    @Test
    @DisplayName("upload with invalid technologyIds should throw BadRequestException")
    void upload_withInvalidTechnologyIds_shouldThrowBadRequest() throws IOException {
        UUID docTypeId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        UUID techId1 = UUID.randomUUID();
        UUID techId2 = UUID.randomUUID();

        DocumentType documentType = DocumentType.builder()
                .name("Proposal")
                .isActive(true)
                .build();
        documentType.setId(docTypeId);

        Technology tech1 = Technology.builder().name("Java").build();
        tech1.setId(techId1);

        CreateDocumentMetadataRequest metadataRequest = CreateDocumentMetadataRequest.builder()
                .technologyIds(List.of(techId1, techId2))
                .build();

        CreateDocumentRequest request = CreateDocumentRequest.builder()
                .title("Test Document")
                .customerName("Acme Corp")
                .documentDate(LocalDate.now())
                .documentTypeId(docTypeId)
                .metadata(metadataRequest)
                .build();

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("test.pdf");
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        Document savedDocument = Document.builder()
                .title("Test Document")
                .status(DocumentStatus.ACTIVE)
                .currentVersionNumber(1)
                .documentType(documentType)
                .build();
        savedDocument.setId(docId);
        savedDocument.setCreatedAt(Instant.now());
        savedDocument.setUpdatedAt(Instant.now());

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
        // Return only 1 technology when 2 were requested - should trigger BadRequestException
        when(technologyRepository.findAllById(List.of(techId1, techId2))).thenReturn(List.of(tech1));

        assertThatThrownBy(() -> documentService.upload(file, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("technology IDs are invalid");
    }

    @Test
    @DisplayName("update with metadata when existing metadata exists should update metadata")
    void update_withMetadata_whenExistingMetadataExists_shouldUpdateMetadata() {
        mockAdminUser();
        UUID id = UUID.randomUUID();
        UUID domainId = UUID.randomUUID();

        Document document = Document.builder()
                .title("Old Title")
                .status(DocumentStatus.ACTIVE)
                .build();
        document.setId(id);
        document.setCreatedAt(Instant.now());
        document.setUpdatedAt(Instant.now());

        Domain domain = Domain.builder().name("Cloud").build();
        domain.setId(domainId);

        DocumentMetadata existingMetadata = DocumentMetadata.builder()
                .document(document)
                .build();
        existingMetadata.setId(UUID.randomUUID());

        CreateDocumentMetadataRequest metadataRequest = CreateDocumentMetadataRequest.builder()
                .domainId(domainId)
                .build();

        UpdateDocumentRequest request = UpdateDocumentRequest.builder()
                .title("New Title")
                .metadata(metadataRequest)
                .build();

        DocumentDto documentDto = DocumentDto.builder()
                .id(id)
                .title("New Title")
                .build();

        when(documentRepository.findById(id)).thenReturn(Optional.of(document));
        when(documentMetadataRepository.findByDocumentId(id)).thenReturn(Optional.of(existingMetadata));
        when(domainRepository.findById(domainId)).thenReturn(Optional.of(domain));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(documentMapper.toDto(document)).thenReturn(documentDto);

        DocumentDto result = documentService.update(id, request);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("New Title");
        verify(documentMetadataRepository).save(any(DocumentMetadata.class));
    }

    @Test
    @DisplayName("upload without metadata (null) should NOT save document metadata")
    void upload_withoutMetadata_shouldNotSaveDocumentMetadata() throws IOException {
        UUID docTypeId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();

        DocumentType documentType = DocumentType.builder()
                .name("Proposal")
                .isActive(true)
                .build();
        documentType.setId(docTypeId);

        CreateDocumentRequest request = CreateDocumentRequest.builder()
                .title("Test Document")
                .customerName("Acme Corp")
                .documentDate(LocalDate.now())
                .documentTypeId(docTypeId)
                .build(); // No metadata

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("test.pdf");
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        Document savedDocument = Document.builder()
                .title("Test Document")
                .status(DocumentStatus.ACTIVE)
                .currentVersionNumber(1)
                .documentType(documentType)
                .build();
        savedDocument.setId(docId);
        savedDocument.setCreatedAt(Instant.now());
        savedDocument.setUpdatedAt(Instant.now());

        DocumentVersion savedVersion = DocumentVersion.builder()
                .document(savedDocument)
                .versionNumber(1)
                .filePath("documents/" + docId + "/1/test.pdf")
                .fileName("test.pdf")
                .fileSize(1024L)
                .contentType("application/pdf")
                .build();
        savedVersion.setId(UUID.randomUUID());

        DocumentDto documentDto = DocumentDto.builder()
                .id(docId)
                .title("Test Document")
                .status(DocumentStatus.ACTIVE)
                .build();

        when(documentTypeRepository.findById(docTypeId)).thenReturn(Optional.of(documentType));
        when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);
        when(blobStorageService.upload(anyString(), anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenReturn("https://blob.storage/documents/test.pdf");
        when(documentVersionRepository.save(any(DocumentVersion.class))).thenReturn(savedVersion);
        when(documentMapper.toDto(savedDocument)).thenReturn(documentDto);

        DocumentDto result = documentService.upload(file, request);

        assertThat(result).isNotNull();
        verify(documentMetadataRepository, never()).save(any(DocumentMetadata.class));
    }

    // ==================== Testing Gap 3: Viewer/download/compare URL flow tests ====================

    @Test
    @DisplayName("getViewUrl should return correct DTO when PDF rendition is COMPLETED")
    void getViewUrl_shouldReturnCompletedDto_whenRenditionCompleted() {
        mockAdminUser();
        UUID docId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();

        Document document = Document.builder()
                .title("Test Doc")
                .status(DocumentStatus.ACTIVE)
                .currentVersionNumber(1)
                .build();
        document.setId(docId);

        DocumentVersion version = DocumentVersion.builder()
                .document(document)
                .versionNumber(1)
                .filePath("documents/" + docId + "/1/test.pdf")
                .fileName("test.pdf")
                .build();
        version.setId(versionId);

        Rendition rendition = Rendition.builder()
                .renditionType(RenditionType.PDF)
                .status(RenditionStatus.COMPLETED)
                .filePath("renditions/" + docId + "/1/test.pdf")
                .documentVersion(version)
                .build();
        rendition.setId(UUID.randomUUID());

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(documentVersionRepository.findByDocumentIdAndVersionNumber(docId, 1))
                .thenReturn(Optional.of(version));
        when(renditionRepository.findByDocumentVersionIdAndRenditionType(versionId, RenditionType.PDF))
                .thenReturn(Optional.of(rendition));
        when(blobStorageService.generateSasUrl(eq("renditions"), anyString(), any(Duration.class)))
                .thenReturn("https://blob.storage/renditions/test.pdf?sas=token");

        DocumentViewDto result = documentService.getViewUrl(docId);

        assertThat(result).isNotNull();
        assertThat(result.getDocumentId()).isEqualTo(docId);
        assertThat(result.getVersionNumber()).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getViewUrl()).contains("sas=token");
        assertThat(result.getContentType()).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("getViewUrl should return status PENDING when rendition is still processing")
    void getViewUrl_shouldReturnPendingStatus_whenRenditionProcessing() {
        mockAdminUser();
        UUID docId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();

        Document document = Document.builder()
                .title("Test Doc")
                .status(DocumentStatus.ACTIVE)
                .currentVersionNumber(1)
                .build();
        document.setId(docId);

        DocumentVersion version = DocumentVersion.builder()
                .document(document)
                .versionNumber(1)
                .filePath("documents/" + docId + "/1/test.pdf")
                .fileName("test.pdf")
                .build();
        version.setId(versionId);

        Rendition rendition = Rendition.builder()
                .renditionType(RenditionType.PDF)
                .status(RenditionStatus.PENDING)
                .filePath("renditions/" + docId + "/1/test.pdf")
                .documentVersion(version)
                .build();
        rendition.setId(UUID.randomUUID());

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(documentVersionRepository.findByDocumentIdAndVersionNumber(docId, 1))
                .thenReturn(Optional.of(version));
        when(renditionRepository.findByDocumentVersionIdAndRenditionType(versionId, RenditionType.PDF))
                .thenReturn(Optional.of(rendition));

        DocumentViewDto result = documentService.getViewUrl(docId);

        assertThat(result).isNotNull();
        assertThat(result.getDocumentId()).isEqualTo(docId);
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getViewUrl()).isNull();
        assertThat(result.getMessage()).contains("pending");
    }

    @Test
    @DisplayName("getViewUrl should return NOT_AVAILABLE when no rendition exists")
    void getViewUrl_shouldReturnNotAvailable_whenNoRenditionExists() {
        mockAdminUser();
        UUID docId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();

        Document document = Document.builder()
                .title("Test Doc")
                .status(DocumentStatus.ACTIVE)
                .currentVersionNumber(1)
                .build();
        document.setId(docId);

        DocumentVersion version = DocumentVersion.builder()
                .document(document)
                .versionNumber(1)
                .filePath("documents/" + docId + "/1/test.pdf")
                .fileName("test.pdf")
                .build();
        version.setId(versionId);

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(documentVersionRepository.findByDocumentIdAndVersionNumber(docId, 1))
                .thenReturn(Optional.of(version));
        when(renditionRepository.findByDocumentVersionIdAndRenditionType(versionId, RenditionType.PDF))
                .thenReturn(Optional.empty());

        DocumentViewDto result = documentService.getViewUrl(docId);

        assertThat(result).isNotNull();
        assertThat(result.getDocumentId()).isEqualTo(docId);
        assertThat(result.getStatus()).isEqualTo("NOT_AVAILABLE");
        assertThat(result.getViewUrl()).isNull();
        assertThat(result.getMessage()).contains("not available");
    }

    @Test
    @DisplayName("getDownloadUrl should return correct download URL with SAS token")
    void getDownloadUrl_shouldReturnCorrectDownloadUrl() {
        mockAdminUser();
        UUID docId = UUID.randomUUID();

        Document document = Document.builder()
                .title("Test Doc")
                .status(DocumentStatus.ACTIVE)
                .currentVersionNumber(1)
                .build();
        document.setId(docId);

        DocumentVersion version = DocumentVersion.builder()
                .document(document)
                .versionNumber(1)
                .filePath("documents/" + docId + "/1/test.pdf")
                .fileName("test.pdf")
                .fileSize(2048L)
                .contentType("application/pdf")
                .build();
        version.setId(UUID.randomUUID());

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(documentVersionRepository.findByDocumentIdAndVersionNumber(docId, 1))
                .thenReturn(Optional.of(version));
        when(blobStorageService.generateSasUrl(eq("documents"), anyString(), any(Duration.class)))
                .thenReturn("https://blob.storage/documents/test.pdf?sas=download-token");

        DocumentDownloadDto result = documentService.getDownloadUrl(docId);

        assertThat(result).isNotNull();
        assertThat(result.getDocumentId()).isEqualTo(docId);
        assertThat(result.getVersionNumber()).isEqualTo(1);
        assertThat(result.getDownloadUrl()).contains("sas=download-token");
        assertThat(result.getFileName()).isEqualTo("test.pdf");
        assertThat(result.getContentType()).isEqualTo("application/pdf");
        assertThat(result.getFileSize()).isEqualTo(2048L);
    }

    @Test
    @DisplayName("getCompareUrls should return URLs for both versions when both renditions are completed")
    void getCompareUrls_shouldReturnBothUrls_whenBothRenditionsCompleted() {
        mockAdminUser();
        UUID docId = UUID.randomUUID();
        UUID versionId1 = UUID.randomUUID();
        UUID versionId2 = UUID.randomUUID();

        DocumentVersion version1 = DocumentVersion.builder()
                .versionNumber(1)
                .filePath("documents/" + docId + "/1/test.pdf")
                .build();
        version1.setId(versionId1);

        DocumentVersion version2 = DocumentVersion.builder()
                .versionNumber(2)
                .filePath("documents/" + docId + "/2/test.pdf")
                .build();
        version2.setId(versionId2);

        Rendition rendition1 = Rendition.builder()
                .renditionType(RenditionType.PDF)
                .status(RenditionStatus.COMPLETED)
                .filePath("renditions/" + docId + "/1/test.pdf")
                .build();

        Rendition rendition2 = Rendition.builder()
                .renditionType(RenditionType.PDF)
                .status(RenditionStatus.COMPLETED)
                .filePath("renditions/" + docId + "/2/test.pdf")
                .build();

        when(documentRepository.existsById(docId)).thenReturn(true);
        when(documentVersionRepository.findByDocumentIdAndVersionNumber(docId, 1))
                .thenReturn(Optional.of(version1));
        when(documentVersionRepository.findByDocumentIdAndVersionNumber(docId, 2))
                .thenReturn(Optional.of(version2));
        when(renditionRepository.findByDocumentVersionIdAndRenditionType(versionId1, RenditionType.PDF))
                .thenReturn(Optional.of(rendition1));
        when(renditionRepository.findByDocumentVersionIdAndRenditionType(versionId2, RenditionType.PDF))
                .thenReturn(Optional.of(rendition2));
        when(blobStorageService.generateSasUrl(eq("renditions"), eq("renditions/" + docId + "/1/test.pdf"), any(Duration.class)))
                .thenReturn("https://blob.storage/renditions/v1.pdf?sas=token1");
        when(blobStorageService.generateSasUrl(eq("renditions"), eq("renditions/" + docId + "/2/test.pdf"), any(Duration.class)))
                .thenReturn("https://blob.storage/renditions/v2.pdf?sas=token2");

        CompareViewDto result = documentService.getCompareUrls(docId, 1, 2);

        assertThat(result).isNotNull();
        assertThat(result.getDocumentId()).isEqualTo(docId);
        assertThat(result.getVersion1Number()).isEqualTo(1);
        assertThat(result.getVersion2Number()).isEqualTo(2);
        assertThat(result.getVersion1Status()).isEqualTo("COMPLETED");
        assertThat(result.getVersion2Status()).isEqualTo("COMPLETED");
        assertThat(result.getVersion1Url()).contains("sas=token1");
        assertThat(result.getVersion2Url()).contains("sas=token2");
    }

    @Test
    @DisplayName("getCompareUrls should handle one version completed and other pending")
    void getCompareUrls_shouldHandleMixedStatuses() {
        mockAdminUser();
        UUID docId = UUID.randomUUID();
        UUID versionId1 = UUID.randomUUID();
        UUID versionId2 = UUID.randomUUID();

        DocumentVersion version1 = DocumentVersion.builder()
                .versionNumber(1)
                .filePath("documents/" + docId + "/1/test.pdf")
                .build();
        version1.setId(versionId1);

        DocumentVersion version2 = DocumentVersion.builder()
                .versionNumber(2)
                .filePath("documents/" + docId + "/2/test.pdf")
                .build();
        version2.setId(versionId2);

        Rendition rendition1 = Rendition.builder()
                .renditionType(RenditionType.PDF)
                .status(RenditionStatus.COMPLETED)
                .filePath("renditions/" + docId + "/1/test.pdf")
                .build();

        Rendition rendition2 = Rendition.builder()
                .renditionType(RenditionType.PDF)
                .status(RenditionStatus.PENDING)
                .filePath("renditions/" + docId + "/2/test.pdf")
                .build();

        when(documentRepository.existsById(docId)).thenReturn(true);
        when(documentVersionRepository.findByDocumentIdAndVersionNumber(docId, 1))
                .thenReturn(Optional.of(version1));
        when(documentVersionRepository.findByDocumentIdAndVersionNumber(docId, 2))
                .thenReturn(Optional.of(version2));
        when(renditionRepository.findByDocumentVersionIdAndRenditionType(versionId1, RenditionType.PDF))
                .thenReturn(Optional.of(rendition1));
        when(renditionRepository.findByDocumentVersionIdAndRenditionType(versionId2, RenditionType.PDF))
                .thenReturn(Optional.of(rendition2));
        when(blobStorageService.generateSasUrl(eq("renditions"), eq("renditions/" + docId + "/1/test.pdf"), any(Duration.class)))
                .thenReturn("https://blob.storage/renditions/v1.pdf?sas=token1");

        CompareViewDto result = documentService.getCompareUrls(docId, 1, 2);

        assertThat(result).isNotNull();
        assertThat(result.getVersion1Status()).isEqualTo("COMPLETED");
        assertThat(result.getVersion1Url()).contains("sas=token1");
        assertThat(result.getVersion2Status()).isEqualTo("PENDING");
        assertThat(result.getVersion2Url()).isNull();
    }

    @Test
    @DisplayName("downloadVersion should return InputStream from blob storage with ACL check")
    void downloadVersion_shouldReturnInputStream_withAclCheck() {
        mockAdminUser();
        UUID docId = UUID.randomUUID();
        int versionNumber = 2;
        String filePath = "documents/" + docId + "/2/file.pdf";

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
}
