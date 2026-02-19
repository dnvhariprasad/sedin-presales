package com.sedin.presales.application.mapper;

import com.sedin.presales.application.dto.DocumentDetailDto;
import com.sedin.presales.application.dto.DocumentDto;
import com.sedin.presales.application.dto.DocumentMetadataDto;
import com.sedin.presales.application.dto.DocumentVersionDto;
import com.sedin.presales.application.dto.FolderDto;
import com.sedin.presales.domain.entity.BusinessUnit;
import com.sedin.presales.domain.entity.Document;
import com.sedin.presales.domain.entity.DocumentMetadata;
import com.sedin.presales.domain.entity.DocumentType;
import com.sedin.presales.domain.entity.DocumentVersion;
import com.sedin.presales.domain.entity.Domain;
import com.sedin.presales.domain.entity.Folder;
import com.sedin.presales.domain.entity.Industry;
import com.sedin.presales.domain.entity.Sbu;
import com.sedin.presales.domain.entity.Technology;
import com.sedin.presales.domain.enums.DocumentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentMapperTest {

    private DocumentMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new DocumentMapper();
    }

    @Test
    @DisplayName("toDto should map Document to DocumentDto with folder and documentType")
    void toDto_shouldMapDocumentToDto() {
        UUID docId = UUID.randomUUID();
        UUID folderId = UUID.randomUUID();
        UUID docTypeId = UUID.randomUUID();
        Instant now = Instant.now();

        Folder folder = Folder.builder()
                .id(folderId)
                .name("Test Folder")
                .build();

        DocumentType docType = DocumentType.builder()
                .id(docTypeId)
                .name("Proposal")
                .build();

        Document document = Document.builder()
                .id(docId)
                .title("Test Document")
                .customerName("Acme Corp")
                .documentDate(LocalDate.of(2025, 6, 15))
                .status(DocumentStatus.ACTIVE)
                .ragIndexed(true)
                .currentVersionNumber(2)
                .folder(folder)
                .documentType(docType)
                .createdAt(now)
                .updatedAt(now)
                .createdBy("admin")
                .build();

        DocumentDto dto = mapper.toDto(document);

        assertThat(dto.getId()).isEqualTo(docId);
        assertThat(dto.getTitle()).isEqualTo("Test Document");
        assertThat(dto.getCustomerName()).isEqualTo("Acme Corp");
        assertThat(dto.getDocumentDate()).isEqualTo(LocalDate.of(2025, 6, 15));
        assertThat(dto.getStatus()).isEqualTo(DocumentStatus.ACTIVE);
        assertThat(dto.getRagIndexed()).isTrue();
        assertThat(dto.getCurrentVersionNumber()).isEqualTo(2);
        assertThat(dto.getFolderId()).isEqualTo(folderId);
        assertThat(dto.getFolderName()).isEqualTo("Test Folder");
        assertThat(dto.getDocumentTypeId()).isEqualTo(docTypeId);
        assertThat(dto.getDocumentTypeName()).isEqualTo("Proposal");
        assertThat(dto.getCreatedAt()).isEqualTo(now);
        assertThat(dto.getUpdatedAt()).isEqualTo(now);
        assertThat(dto.getCreatedBy()).isEqualTo("admin");
    }

    @Test
    @DisplayName("toDto should handle null folder and documentType gracefully")
    void toDto_shouldHandleNullFolder() {
        UUID docId = UUID.randomUUID();

        Document document = Document.builder()
                .id(docId)
                .title("Orphan Document")
                .status(DocumentStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        DocumentDto dto = mapper.toDto(document);

        assertThat(dto.getId()).isEqualTo(docId);
        assertThat(dto.getTitle()).isEqualTo("Orphan Document");
        assertThat(dto.getFolderId()).isNull();
        assertThat(dto.getFolderName()).isNull();
        assertThat(dto.getDocumentTypeId()).isNull();
        assertThat(dto.getDocumentTypeName()).isNull();
    }

    @Test
    @DisplayName("toDetailDto should include metadata and versions")
    void toDetailDto_shouldIncludeMetadataAndVersions() {
        UUID docId = UUID.randomUUID();
        UUID metadataId = UUID.randomUUID();
        UUID domainId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        Instant now = Instant.now();

        Domain domain = Domain.builder()
                .id(domainId)
                .name("Healthcare")
                .build();

        DocumentMetadata metadata = DocumentMetadata.builder()
                .id(metadataId)
                .domain(domain)
                .build();

        DocumentVersion version = DocumentVersion.builder()
                .id(versionId)
                .versionNumber(1)
                .fileName("doc.pdf")
                .fileSize(1024L)
                .contentType("application/pdf")
                .filePath("/docs/doc.pdf")
                .uploadedBy("admin")
                .createdAt(now)
                .build();

        Document document = Document.builder()
                .id(docId)
                .title("Detail Document")
                .status(DocumentStatus.ACTIVE)
                .ragIndexed(false)
                .currentVersionNumber(1)
                .documentMetadata(metadata)
                .versions(List.of(version))
                .createdAt(now)
                .updatedAt(now)
                .createdBy("admin")
                .build();

        DocumentDetailDto dto = mapper.toDetailDto(document);

        assertThat(dto.getId()).isEqualTo(docId);
        assertThat(dto.getTitle()).isEqualTo("Detail Document");
        assertThat(dto.getMetadata()).isNotNull();
        assertThat(dto.getMetadata().getId()).isEqualTo(metadataId);
        assertThat(dto.getMetadata().getDomain()).isNotNull();
        assertThat(dto.getMetadata().getDomain().getId()).isEqualTo(domainId);
        assertThat(dto.getMetadata().getDomain().getName()).isEqualTo("Healthcare");
        assertThat(dto.getVersions()).hasSize(1);
        assertThat(dto.getVersions().get(0).getId()).isEqualTo(versionId);
        assertThat(dto.getVersions().get(0).getFileName()).isEqualTo("doc.pdf");
    }

    @Test
    @DisplayName("toVersionDto should map all version fields")
    void toVersionDto_shouldMapVersionFields() {
        UUID versionId = UUID.randomUUID();
        Instant now = Instant.now();

        DocumentVersion version = DocumentVersion.builder()
                .id(versionId)
                .versionNumber(3)
                .fileName("report.docx")
                .fileSize(2048L)
                .contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .filePath("/uploads/report.docx")
                .uploadedBy("john")
                .createdAt(now)
                .build();

        DocumentVersionDto dto = mapper.toVersionDto(version);

        assertThat(dto.getId()).isEqualTo(versionId);
        assertThat(dto.getVersionNumber()).isEqualTo(3);
        assertThat(dto.getFileName()).isEqualTo("report.docx");
        assertThat(dto.getFileSize()).isEqualTo(2048L);
        assertThat(dto.getContentType()).isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        assertThat(dto.getFilePath()).isEqualTo("/uploads/report.docx");
        assertThat(dto.getUploadedBy()).isEqualTo("john");
        assertThat(dto.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("toMetadataDto should map all metadata fields including technologies")
    void toMetadataDto_shouldMapMetadataFields() {
        UUID metadataId = UUID.randomUUID();
        UUID domainId = UUID.randomUUID();
        UUID industryId = UUID.randomUUID();
        UUID buId = UUID.randomUUID();
        UUID sbuId = UUID.randomUUID();
        UUID techId1 = UUID.randomUUID();
        UUID techId2 = UUID.randomUUID();

        Domain domain = Domain.builder().id(domainId).name("Finance").build();
        Industry industry = Industry.builder().id(industryId).name("Banking").build();
        BusinessUnit bu = BusinessUnit.builder().id(buId).name("Digital").build();
        Sbu sbu = Sbu.builder().id(sbuId).name("Cloud").build();
        Technology tech1 = Technology.builder().id(techId1).name("Java").build();
        Technology tech2 = Technology.builder().id(techId2).name("Spring").build();

        DocumentMetadata metadata = DocumentMetadata.builder()
                .id(metadataId)
                .domain(domain)
                .industry(industry)
                .businessUnit(bu)
                .sbu(sbu)
                .technologies(Set.of(tech1, tech2))
                .build();

        DocumentMetadataDto dto = mapper.toMetadataDto(metadata);

        assertThat(dto.getId()).isEqualTo(metadataId);
        assertThat(dto.getDomain()).isNotNull();
        assertThat(dto.getDomain().getId()).isEqualTo(domainId);
        assertThat(dto.getDomain().getName()).isEqualTo("Finance");
        assertThat(dto.getIndustry()).isNotNull();
        assertThat(dto.getIndustry().getId()).isEqualTo(industryId);
        assertThat(dto.getIndustry().getName()).isEqualTo("Banking");
        assertThat(dto.getBusinessUnit()).isNotNull();
        assertThat(dto.getBusinessUnit().getId()).isEqualTo(buId);
        assertThat(dto.getBusinessUnit().getName()).isEqualTo("Digital");
        assertThat(dto.getSbu()).isNotNull();
        assertThat(dto.getSbu().getId()).isEqualTo(sbuId);
        assertThat(dto.getSbu().getName()).isEqualTo("Cloud");
        assertThat(dto.getTechnologies()).hasSize(2);
        assertThat(dto.getTechnologies())
                .extracting("name")
                .containsExactlyInAnyOrder("Java", "Spring");
    }

    @Test
    @DisplayName("toFolderDto should map folder fields including parent info and counts")
    void toFolderDto_shouldMapFolderFields() {
        UUID folderId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        Instant now = Instant.now();

        Folder parent = Folder.builder()
                .id(parentId)
                .name("Parent Folder")
                .build();

        Folder child1 = Folder.builder().id(UUID.randomUUID()).name("Child 1").build();
        Folder child2 = Folder.builder().id(UUID.randomUUID()).name("Child 2").build();

        Document doc1 = Document.builder().id(UUID.randomUUID()).title("Doc 1").status(DocumentStatus.ACTIVE).build();

        Folder folder = Folder.builder()
                .id(folderId)
                .name("My Folder")
                .description("A test folder")
                .parent(parent)
                .children(List.of(child1, child2))
                .documents(List.of(doc1))
                .createdAt(now)
                .updatedAt(now)
                .createdBy("admin")
                .build();

        FolderDto dto = mapper.toFolderDto(folder);

        assertThat(dto.getId()).isEqualTo(folderId);
        assertThat(dto.getName()).isEqualTo("My Folder");
        assertThat(dto.getDescription()).isEqualTo("A test folder");
        assertThat(dto.getParentId()).isEqualTo(parentId);
        assertThat(dto.getParentName()).isEqualTo("Parent Folder");
        assertThat(dto.getChildCount()).isEqualTo(2);
        assertThat(dto.getDocumentCount()).isEqualTo(1);
        assertThat(dto.getCreatedAt()).isEqualTo(now);
        assertThat(dto.getUpdatedAt()).isEqualTo(now);
        assertThat(dto.getCreatedBy()).isEqualTo("admin");
    }
}
