package com.sedin.presales.application.service;

import com.sedin.presales.application.dto.CreateFolderRequest;
import com.sedin.presales.application.dto.FolderDto;
import com.sedin.presales.application.exception.BadRequestException;
import com.sedin.presales.application.exception.ResourceNotFoundException;
import com.sedin.presales.application.mapper.DocumentMapper;
import com.sedin.presales.domain.entity.Document;
import com.sedin.presales.domain.entity.Folder;
import com.sedin.presales.domain.repository.FolderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FolderServiceTest {

    @Mock
    private FolderRepository folderRepository;

    @Mock
    private DocumentMapper documentMapper;

    @InjectMocks
    private FolderService folderService;

    @Test
    @DisplayName("list should return root folders when parentId is null")
    void list_shouldReturnRootFolders_whenParentIdIsNull() {
        Folder folder = buildFolder(UUID.randomUUID(), "Root Folder", null);
        FolderDto folderDto = FolderDto.builder()
                .id(folder.getId())
                .name(folder.getName())
                .build();

        when(folderRepository.findByParentIsNull()).thenReturn(List.of(folder));
        when(documentMapper.toFolderDto(folder)).thenReturn(folderDto);

        List<FolderDto> result = folderService.list(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Root Folder");
        verify(folderRepository).findByParentIsNull();
    }

    @Test
    @DisplayName("list should return child folders when parentId is provided")
    void list_shouldReturnChildFolders_whenParentIdProvided() {
        UUID parentId = UUID.randomUUID();
        Folder child = buildFolder(UUID.randomUUID(), "Child Folder", null);
        FolderDto childDto = FolderDto.builder()
                .id(child.getId())
                .name(child.getName())
                .parentId(parentId)
                .build();

        when(folderRepository.findByParentId(parentId)).thenReturn(List.of(child));
        when(documentMapper.toFolderDto(child)).thenReturn(childDto);

        List<FolderDto> result = folderService.list(parentId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getParentId()).isEqualTo(parentId);
        verify(folderRepository).findByParentId(parentId);
    }

    @Test
    @DisplayName("getById should return FolderDto when folder exists")
    void getById_shouldReturnFolder() {
        UUID id = UUID.randomUUID();
        Folder folder = buildFolder(id, "Test Folder", null);
        FolderDto folderDto = FolderDto.builder()
                .id(id)
                .name("Test Folder")
                .build();

        when(folderRepository.findById(id)).thenReturn(Optional.of(folder));
        when(documentMapper.toFolderDto(folder)).thenReturn(folderDto);

        FolderDto result = folderService.getById(id);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getName()).isEqualTo("Test Folder");
    }

    @Test
    @DisplayName("getById should throw ResourceNotFoundException when folder not found")
    void getById_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(folderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> folderService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("create should create root folder when no parentId")
    void create_shouldCreateRootFolder() {
        CreateFolderRequest request = CreateFolderRequest.builder()
                .name("New Root Folder")
                .description("Root folder description")
                .build();

        Folder savedFolder = buildFolder(UUID.randomUUID(), "New Root Folder", null);
        FolderDto folderDto = FolderDto.builder()
                .id(savedFolder.getId())
                .name("New Root Folder")
                .build();

        when(folderRepository.save(any(Folder.class))).thenReturn(savedFolder);
        when(documentMapper.toFolderDto(savedFolder)).thenReturn(folderDto);

        FolderDto result = folderService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("New Root Folder");
        verify(folderRepository).save(any(Folder.class));
    }

    @Test
    @DisplayName("create should create child folder when parentId is provided")
    void create_shouldCreateChildFolder() {
        UUID parentId = UUID.randomUUID();
        Folder parentFolder = buildFolder(parentId, "Parent Folder", null);

        CreateFolderRequest request = CreateFolderRequest.builder()
                .name("Child Folder")
                .description("Child folder description")
                .parentId(parentId)
                .build();

        Folder savedFolder = buildFolder(UUID.randomUUID(), "Child Folder", parentFolder);
        FolderDto folderDto = FolderDto.builder()
                .id(savedFolder.getId())
                .name("Child Folder")
                .parentId(parentId)
                .build();

        when(folderRepository.findById(parentId)).thenReturn(Optional.of(parentFolder));
        when(folderRepository.save(any(Folder.class))).thenReturn(savedFolder);
        when(documentMapper.toFolderDto(savedFolder)).thenReturn(folderDto);

        FolderDto result = folderService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Child Folder");
        assertThat(result.getParentId()).isEqualTo(parentId);
        verify(folderRepository).findById(parentId);
    }

    @Test
    @DisplayName("update should update folder name and description")
    void update_shouldUpdateFolder() {
        UUID id = UUID.randomUUID();
        Folder existingFolder = buildFolder(id, "Old Name", null);

        CreateFolderRequest request = CreateFolderRequest.builder()
                .name("Updated Name")
                .description("Updated Description")
                .build();

        Folder updatedFolder = buildFolder(id, "Updated Name", null);
        FolderDto folderDto = FolderDto.builder()
                .id(id)
                .name("Updated Name")
                .description("Updated Description")
                .build();

        when(folderRepository.findById(id)).thenReturn(Optional.of(existingFolder));
        when(folderRepository.save(any(Folder.class))).thenReturn(updatedFolder);
        when(documentMapper.toFolderDto(updatedFolder)).thenReturn(folderDto);

        FolderDto result = folderService.update(id, request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Name");
        verify(folderRepository).save(any(Folder.class));
    }

    @Test
    @DisplayName("update should throw BadRequestException when parentId equals folder id")
    void update_shouldThrowWhenSelfParenting() {
        UUID id = UUID.randomUUID();
        Folder existingFolder = buildFolder(id, "Folder", null);

        CreateFolderRequest request = CreateFolderRequest.builder()
                .name("Folder")
                .description("Desc")
                .parentId(id)
                .build();

        when(folderRepository.findById(id)).thenReturn(Optional.of(existingFolder));

        assertThatThrownBy(() -> folderService.update(id, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("own parent");
    }

    @Test
    @DisplayName("delete should delete empty folder")
    void delete_shouldDeleteEmptyFolder() {
        UUID id = UUID.randomUUID();
        Folder folder = buildFolder(id, "Empty Folder", null);
        folder.setChildren(new ArrayList<>());
        folder.setDocuments(new ArrayList<>());

        when(folderRepository.findById(id)).thenReturn(Optional.of(folder));

        folderService.delete(id);

        verify(folderRepository).delete(folder);
    }

    @Test
    @DisplayName("delete should throw BadRequestException when folder has children")
    void delete_shouldThrowWhenFolderHasChildren() {
        UUID id = UUID.randomUUID();
        Folder folder = buildFolder(id, "Parent Folder", null);
        Folder child = buildFolder(UUID.randomUUID(), "Child", folder);
        folder.setChildren(List.of(child));
        folder.setDocuments(new ArrayList<>());

        when(folderRepository.findById(id)).thenReturn(Optional.of(folder));

        assertThatThrownBy(() -> folderService.delete(id))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("sub-folders");
    }

    @Test
    @DisplayName("delete should throw BadRequestException when folder has documents")
    void delete_shouldThrowWhenFolderHasDocuments() {
        UUID id = UUID.randomUUID();
        Folder folder = buildFolder(id, "Folder With Docs", null);
        folder.setChildren(new ArrayList<>());
        Document doc = Document.builder().title("Doc").build();
        doc.setId(UUID.randomUUID());
        folder.setDocuments(List.of(doc));

        when(folderRepository.findById(id)).thenReturn(Optional.of(folder));

        assertThatThrownBy(() -> folderService.delete(id))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("documents");
    }

    private Folder buildFolder(UUID id, String name, Folder parent) {
        Folder folder = Folder.builder()
                .name(name)
                .description(name + " description")
                .parent(parent)
                .children(new ArrayList<>())
                .documents(new ArrayList<>())
                .build();
        folder.setId(id);
        folder.setCreatedAt(Instant.now());
        folder.setUpdatedAt(Instant.now());
        return folder;
    }
}
