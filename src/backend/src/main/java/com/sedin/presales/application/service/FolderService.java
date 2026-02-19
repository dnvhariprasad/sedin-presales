package com.sedin.presales.application.service;

import com.sedin.presales.application.dto.CreateFolderRequest;
import com.sedin.presales.application.dto.FolderDto;
import com.sedin.presales.application.exception.BadRequestException;
import com.sedin.presales.application.exception.ResourceNotFoundException;
import com.sedin.presales.application.mapper.DocumentMapper;
import com.sedin.presales.domain.entity.Folder;
import com.sedin.presales.domain.repository.FolderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class FolderService {

    private final FolderRepository folderRepository;
    private final DocumentMapper documentMapper;

    public FolderService(FolderRepository folderRepository, DocumentMapper documentMapper) {
        this.folderRepository = folderRepository;
        this.documentMapper = documentMapper;
    }

    public List<FolderDto> list(UUID parentId) {
        log.debug("Listing folders with parentId: {}", parentId);
        List<Folder> folders;
        if (parentId == null) {
            folders = folderRepository.findByParentIsNull();
        } else {
            folders = folderRepository.findByParentId(parentId);
        }
        return folders.stream()
                .map(documentMapper::toFolderDto)
                .toList();
    }

    public FolderDto getById(UUID id) {
        log.debug("Getting folder with id: {}", id);
        Folder folder = folderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", id));
        return documentMapper.toFolderDto(folder);
    }

    @Transactional
    public FolderDto create(CreateFolderRequest request) {
        log.info("Creating folder with name: {}", request.getName());

        Folder folder = Folder.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        if (request.getParentId() != null) {
            Folder parent = folderRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", request.getParentId()));
            folder.setParent(parent);
        }

        Folder saved = folderRepository.save(folder);
        log.info("Created folder with id: {}", saved.getId());
        return documentMapper.toFolderDto(saved);
    }

    @Transactional
    public FolderDto update(UUID id, CreateFolderRequest request) {
        log.info("Updating folder with id: {}", id);
        Folder folder = folderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", id));

        folder.setName(request.getName());
        folder.setDescription(request.getDescription());

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new BadRequestException("A folder cannot be its own parent");
            }
            Folder parent = folderRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", request.getParentId()));
            folder.setParent(parent);
        } else {
            folder.setParent(null);
        }

        Folder saved = folderRepository.save(folder);
        log.info("Updated folder with id: {}", saved.getId());
        return documentMapper.toFolderDto(saved);
    }

    @Transactional
    public void delete(UUID id) {
        log.info("Deleting folder with id: {}", id);
        Folder folder = folderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", id));

        if (folder.getChildren() != null && !folder.getChildren().isEmpty()) {
            throw new BadRequestException("Cannot delete folder that contains sub-folders");
        }

        if (folder.getDocuments() != null && !folder.getDocuments().isEmpty()) {
            throw new BadRequestException("Cannot delete folder that contains documents");
        }

        folderRepository.delete(folder);
        log.info("Deleted folder with id: {}", id);
    }
}
