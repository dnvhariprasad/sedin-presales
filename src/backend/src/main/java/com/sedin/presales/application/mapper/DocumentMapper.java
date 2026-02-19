package com.sedin.presales.application.mapper;

import com.sedin.presales.application.dto.DocumentDetailDto;
import com.sedin.presales.application.dto.DocumentDto;
import com.sedin.presales.application.dto.DocumentMetadataDto;
import com.sedin.presales.application.dto.DocumentVersionDto;
import com.sedin.presales.application.dto.FolderDto;
import com.sedin.presales.application.dto.IdNameDto;
import com.sedin.presales.domain.entity.Document;
import com.sedin.presales.domain.entity.DocumentMetadata;
import com.sedin.presales.domain.entity.DocumentVersion;
import com.sedin.presales.domain.entity.Folder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DocumentMapper {

    public DocumentDto toDto(Document document) {
        DocumentDto.DocumentDtoBuilder builder = DocumentDto.builder()
                .id(document.getId())
                .title(document.getTitle())
                .customerName(document.getCustomerName())
                .documentDate(document.getDocumentDate())
                .status(document.getStatus())
                .ragIndexed(document.getRagIndexed())
                .currentVersionNumber(document.getCurrentVersionNumber())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .createdBy(document.getCreatedBy());

        if (document.getFolder() != null) {
            builder.folderId(document.getFolder().getId())
                    .folderName(document.getFolder().getName());
        }

        if (document.getDocumentType() != null) {
            builder.documentTypeId(document.getDocumentType().getId())
                    .documentTypeName(document.getDocumentType().getName());
        }

        return builder.build();
    }

    public DocumentDetailDto toDetailDto(Document document) {
        DocumentDetailDto.DocumentDetailDtoBuilder builder = DocumentDetailDto.builder()
                .id(document.getId())
                .title(document.getTitle())
                .customerName(document.getCustomerName())
                .documentDate(document.getDocumentDate())
                .status(document.getStatus())
                .ragIndexed(document.getRagIndexed())
                .currentVersionNumber(document.getCurrentVersionNumber())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .createdBy(document.getCreatedBy());

        if (document.getFolder() != null) {
            builder.folderId(document.getFolder().getId())
                    .folderName(document.getFolder().getName());
        }

        if (document.getDocumentType() != null) {
            builder.documentTypeId(document.getDocumentType().getId())
                    .documentTypeName(document.getDocumentType().getName());
        }

        if (document.getDocumentMetadata() != null) {
            builder.metadata(toMetadataDto(document.getDocumentMetadata()));
        }

        if (document.getVersions() != null) {
            List<DocumentVersionDto> versionDtos = document.getVersions().stream()
                    .map(this::toVersionDto)
                    .toList();
            builder.versions(versionDtos);
        }

        return builder.build();
    }

    public DocumentVersionDto toVersionDto(DocumentVersion version) {
        return DocumentVersionDto.builder()
                .id(version.getId())
                .versionNumber(version.getVersionNumber())
                .fileName(version.getFileName())
                .fileSize(version.getFileSize())
                .contentType(version.getContentType())
                .filePath(version.getFilePath())
                .uploadedBy(version.getUploadedBy())
                .createdAt(version.getCreatedAt())
                .build();
    }

    public DocumentMetadataDto toMetadataDto(DocumentMetadata metadata) {
        DocumentMetadataDto.DocumentMetadataDtoBuilder builder = DocumentMetadataDto.builder()
                .id(metadata.getId());

        if (metadata.getDomain() != null) {
            builder.domain(IdNameDto.builder()
                    .id(metadata.getDomain().getId())
                    .name(metadata.getDomain().getName())
                    .build());
        }

        if (metadata.getIndustry() != null) {
            builder.industry(IdNameDto.builder()
                    .id(metadata.getIndustry().getId())
                    .name(metadata.getIndustry().getName())
                    .build());
        }

        if (metadata.getBusinessUnit() != null) {
            builder.businessUnit(IdNameDto.builder()
                    .id(metadata.getBusinessUnit().getId())
                    .name(metadata.getBusinessUnit().getName())
                    .build());
        }

        if (metadata.getSbu() != null) {
            builder.sbu(IdNameDto.builder()
                    .id(metadata.getSbu().getId())
                    .name(metadata.getSbu().getName())
                    .build());
        }

        if (metadata.getTechnologies() != null && !metadata.getTechnologies().isEmpty()) {
            List<IdNameDto> techDtos = metadata.getTechnologies().stream()
                    .map(t -> IdNameDto.builder()
                            .id(t.getId())
                            .name(t.getName())
                            .build())
                    .toList();
            builder.technologies(techDtos);
        }

        return builder.build();
    }

    public FolderDto toFolderDto(Folder folder) {
        FolderDto.FolderDtoBuilder builder = FolderDto.builder()
                .id(folder.getId())
                .name(folder.getName())
                .description(folder.getDescription())
                .childCount(folder.getChildren() != null ? folder.getChildren().size() : 0)
                .documentCount(folder.getDocuments() != null ? folder.getDocuments().size() : 0)
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .createdBy(folder.getCreatedBy());

        if (folder.getParent() != null) {
            builder.parentId(folder.getParent().getId())
                    .parentName(folder.getParent().getName());
        }

        return builder.build();
    }
}
