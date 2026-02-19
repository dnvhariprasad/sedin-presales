package com.sedin.presales.application.service;

import com.sedin.presales.application.dto.CreateDocumentMetadataRequest;
import com.sedin.presales.application.dto.CreateDocumentRequest;
import com.sedin.presales.application.dto.DocumentDetailDto;
import com.sedin.presales.application.dto.DocumentDto;
import com.sedin.presales.application.dto.DocumentVersionDto;
import com.sedin.presales.application.dto.PagedResponse;
import com.sedin.presales.application.dto.UpdateDocumentRequest;
import com.sedin.presales.application.exception.BadRequestException;
import com.sedin.presales.application.exception.ResourceNotFoundException;
import com.sedin.presales.application.mapper.DocumentMapper;
import com.sedin.presales.domain.entity.Document;
import com.sedin.presales.domain.entity.DocumentMetadata;
import com.sedin.presales.domain.entity.DocumentType;
import com.sedin.presales.domain.entity.DocumentVersion;
import com.sedin.presales.domain.entity.Domain;
import com.sedin.presales.domain.entity.Folder;
import com.sedin.presales.domain.entity.Industry;
import com.sedin.presales.domain.entity.Technology;
import com.sedin.presales.domain.entity.BusinessUnit;
import com.sedin.presales.domain.entity.Sbu;
import com.sedin.presales.domain.enums.DocumentStatus;
import com.sedin.presales.domain.repository.DocumentMetadataRepository;
import com.sedin.presales.domain.repository.DocumentRepository;
import com.sedin.presales.domain.repository.DocumentTypeRepository;
import com.sedin.presales.domain.repository.DocumentVersionRepository;
import com.sedin.presales.domain.repository.DomainRepository;
import com.sedin.presales.domain.repository.FolderRepository;
import com.sedin.presales.domain.repository.IndustryRepository;
import com.sedin.presales.domain.repository.TechnologyRepository;
import com.sedin.presales.domain.repository.BusinessUnitRepository;
import com.sedin.presales.domain.repository.SbuRepository;
import com.sedin.presales.infrastructure.storage.BlobStorageService;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class DocumentService {

    private static final String CONTAINER_NAME = "documents";

    private final DocumentRepository documentRepository;
    private final DocumentMetadataRepository documentMetadataRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final FolderRepository folderRepository;
    private final DomainRepository domainRepository;
    private final IndustryRepository industryRepository;
    private final TechnologyRepository technologyRepository;
    private final BusinessUnitRepository businessUnitRepository;
    private final SbuRepository sbuRepository;
    private final BlobStorageService blobStorageService;
    private final DocumentMapper documentMapper;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentMetadataRepository documentMetadataRepository,
                           DocumentVersionRepository documentVersionRepository,
                           DocumentTypeRepository documentTypeRepository,
                           FolderRepository folderRepository,
                           DomainRepository domainRepository,
                           IndustryRepository industryRepository,
                           TechnologyRepository technologyRepository,
                           BusinessUnitRepository businessUnitRepository,
                           SbuRepository sbuRepository,
                           BlobStorageService blobStorageService,
                           DocumentMapper documentMapper) {
        this.documentRepository = documentRepository;
        this.documentMetadataRepository = documentMetadataRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.documentTypeRepository = documentTypeRepository;
        this.folderRepository = folderRepository;
        this.domainRepository = domainRepository;
        this.industryRepository = industryRepository;
        this.technologyRepository = technologyRepository;
        this.businessUnitRepository = businessUnitRepository;
        this.sbuRepository = sbuRepository;
        this.blobStorageService = blobStorageService;
        this.documentMapper = documentMapper;
    }

    @Transactional
    public DocumentDto upload(MultipartFile file, CreateDocumentRequest request) {
        log.info("Uploading document with title: {}", request.getTitle());

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }

        DocumentType documentType = documentTypeRepository.findById(request.getDocumentTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("DocumentType", "id", request.getDocumentTypeId()));

        Document document = Document.builder()
                .title(request.getTitle())
                .customerName(request.getCustomerName())
                .documentDate(request.getDocumentDate())
                .status(DocumentStatus.ACTIVE)
                .ragIndexed(false)
                .currentVersionNumber(1)
                .documentType(documentType)
                .build();

        if (request.getFolderId() != null) {
            Folder folder = folderRepository.findById(request.getFolderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", request.getFolderId()));
            document.setFolder(folder);
        }

        Document savedDocument = documentRepository.save(document);

        // Upload file to blob storage
        String blobPath = buildBlobPath(savedDocument.getId(), 1, file.getOriginalFilename());
        String blobUrl;
        try {
            blobUrl = blobStorageService.upload(
                    CONTAINER_NAME,
                    blobPath,
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType()
            );
        } catch (IOException e) {
            log.error("Failed to upload file to blob storage", e);
            throw new BadRequestException("Failed to upload file: " + e.getMessage());
        }

        // Create document version
        DocumentVersion version = DocumentVersion.builder()
                .document(savedDocument)
                .versionNumber(1)
                .filePath(blobPath)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .build();
        documentVersionRepository.save(version);

        // Create document metadata if provided
        if (request.getMetadata() != null) {
            createDocumentMetadata(savedDocument, request.getMetadata());
        }

        log.info("Uploaded document with id: {}", savedDocument.getId());
        return documentMapper.toDto(savedDocument);
    }

    public PagedResponse<DocumentDto> list(Pageable pageable, UUID folderId, UUID documentTypeId,
                                           DocumentStatus status, String search) {
        log.debug("Listing documents with filters - folderId: {}, documentTypeId: {}, status: {}, search: {}",
                folderId, documentTypeId, status, search);

        Specification<Document> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Exclude archived/deleted by default unless explicitly filtering by status
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            } else {
                predicates.add(criteriaBuilder.notEqual(root.get("status"), DocumentStatus.ARCHIVED));
            }

            if (folderId != null) {
                predicates.add(criteriaBuilder.equal(root.get("folder").get("id"), folderId));
            }

            if (documentTypeId != null) {
                predicates.add(criteriaBuilder.equal(root.get("documentType").get("id"), documentTypeId));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                Predicate titleMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern);
                Predicate customerMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("customerName")), pattern);
                predicates.add(criteriaBuilder.or(titleMatch, customerMatch));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<Document> page = documentRepository.findAll(spec, pageable);

        return PagedResponse.<DocumentDto>builder()
                .content(page.getContent().stream().map(documentMapper::toDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    public DocumentDetailDto getById(UUID id) {
        log.debug("Getting document with id: {}", id);
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));
        return documentMapper.toDetailDto(document);
    }

    @Transactional
    public DocumentDto update(UUID id, UpdateDocumentRequest request) {
        log.info("Updating document with id: {}", id);
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));

        if (request.getTitle() != null) {
            document.setTitle(request.getTitle());
        }

        if (request.getCustomerName() != null) {
            document.setCustomerName(request.getCustomerName());
        }

        if (request.getDocumentDate() != null) {
            document.setDocumentDate(request.getDocumentDate());
        }

        if (request.getDocumentTypeId() != null) {
            DocumentType documentType = documentTypeRepository.findById(request.getDocumentTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("DocumentType", "id", request.getDocumentTypeId()));
            document.setDocumentType(documentType);
        }

        if (request.getFolderId() != null) {
            Folder folder = folderRepository.findById(request.getFolderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", request.getFolderId()));
            document.setFolder(folder);
        }

        // Update metadata if provided
        if (request.getMetadata() != null) {
            DocumentMetadata existingMetadata = documentMetadataRepository.findByDocumentId(id).orElse(null);
            if (existingMetadata != null) {
                updateDocumentMetadata(existingMetadata, request.getMetadata());
            } else {
                createDocumentMetadata(document, request.getMetadata());
            }
        }

        Document saved = documentRepository.save(document);
        log.info("Updated document with id: {}", saved.getId());
        return documentMapper.toDto(saved);
    }

    @Transactional
    public void delete(UUID id) {
        log.info("Soft deleting document with id: {}", id);
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));

        document.setStatus(DocumentStatus.ARCHIVED);
        documentRepository.save(document);
        log.info("Soft deleted (archived) document with id: {}", id);
    }

    @Transactional
    public DocumentVersionDto uploadNewVersion(UUID documentId, MultipartFile file, String changeNotes) {
        log.info("Uploading new version for document: {}", documentId);

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));

        int newVersionNumber = document.getCurrentVersionNumber() + 1;

        // Upload file to blob storage
        String blobPath = buildBlobPath(documentId, newVersionNumber, file.getOriginalFilename());
        try {
            blobStorageService.upload(
                    CONTAINER_NAME,
                    blobPath,
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType()
            );
        } catch (IOException e) {
            log.error("Failed to upload file to blob storage", e);
            throw new BadRequestException("Failed to upload file: " + e.getMessage());
        }

        // Create new document version
        DocumentVersion version = DocumentVersion.builder()
                .document(document)
                .versionNumber(newVersionNumber)
                .filePath(blobPath)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .build();
        DocumentVersion savedVersion = documentVersionRepository.save(version);

        // Update document's current version number
        document.setCurrentVersionNumber(newVersionNumber);
        documentRepository.save(document);

        log.info("Uploaded version {} for document: {}", newVersionNumber, documentId);
        return documentMapper.toVersionDto(savedVersion);
    }

    public List<DocumentVersionDto> getVersions(UUID documentId) {
        log.debug("Getting versions for document: {}", documentId);
        if (!documentRepository.existsById(documentId)) {
            throw new ResourceNotFoundException("Document", "id", documentId);
        }
        List<DocumentVersion> versions = documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId);
        return versions.stream()
                .map(documentMapper::toVersionDto)
                .toList();
    }

    public InputStream downloadVersion(UUID documentId, Integer versionNumber) {
        log.info("Downloading version {} for document: {}", versionNumber, documentId);
        DocumentVersion version = documentVersionRepository.findByDocumentIdAndVersionNumber(documentId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("DocumentVersion", "versionNumber", versionNumber));

        return blobStorageService.download(CONTAINER_NAME, version.getFilePath());
    }

    public DocumentVersion getDocumentVersion(UUID documentId, Integer versionNumber) {
        return documentVersionRepository.findByDocumentIdAndVersionNumber(documentId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("DocumentVersion", "versionNumber", versionNumber));
    }

    private String buildBlobPath(UUID documentId, int versionNumber, String fileName) {
        return String.format("documents/%s/%d/%s", documentId, versionNumber, fileName);
    }

    private void createDocumentMetadata(Document document, CreateDocumentMetadataRequest request) {
        DocumentMetadata metadata = DocumentMetadata.builder()
                .document(document)
                .build();

        populateMetadata(metadata, request);
        documentMetadataRepository.save(metadata);
    }

    private void updateDocumentMetadata(DocumentMetadata metadata, CreateDocumentMetadataRequest request) {
        populateMetadata(metadata, request);
        documentMetadataRepository.save(metadata);
    }

    private void populateMetadata(DocumentMetadata metadata, CreateDocumentMetadataRequest request) {
        if (request.getDomainId() != null) {
            Domain domain = domainRepository.findById(request.getDomainId())
                    .orElseThrow(() -> new ResourceNotFoundException("Domain", "id", request.getDomainId()));
            metadata.setDomain(domain);
        } else {
            metadata.setDomain(null);
        }

        if (request.getIndustryId() != null) {
            Industry industry = industryRepository.findById(request.getIndustryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Industry", "id", request.getIndustryId()));
            metadata.setIndustry(industry);
        } else {
            metadata.setIndustry(null);
        }

        if (request.getBusinessUnitId() != null) {
            BusinessUnit businessUnit = businessUnitRepository.findById(request.getBusinessUnitId())
                    .orElseThrow(() -> new ResourceNotFoundException("BusinessUnit", "id", request.getBusinessUnitId()));
            metadata.setBusinessUnit(businessUnit);
        } else {
            metadata.setBusinessUnit(null);
        }

        if (request.getSbuId() != null) {
            Sbu sbu = sbuRepository.findById(request.getSbuId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sbu", "id", request.getSbuId()));
            metadata.setSbu(sbu);
        } else {
            metadata.setSbu(null);
        }

        if (request.getTechnologyIds() != null && !request.getTechnologyIds().isEmpty()) {
            Set<Technology> technologies = new HashSet<>(technologyRepository.findAllById(request.getTechnologyIds()));
            if (technologies.size() != request.getTechnologyIds().size()) {
                throw new BadRequestException("One or more technology IDs are invalid");
            }
            metadata.setTechnologies(technologies);
        } else {
            metadata.setTechnologies(new HashSet<>());
        }
    }
}
