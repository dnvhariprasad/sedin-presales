package com.sedin.presales.application.service;

import com.sedin.presales.application.dto.CreateMasterRequest;
import com.sedin.presales.application.dto.MasterDto;
import com.sedin.presales.application.dto.PagedResponse;
import com.sedin.presales.application.dto.UpdateMasterRequest;
import com.sedin.presales.application.exception.ResourceNotFoundException;
import com.sedin.presales.domain.entity.Domain;
import com.sedin.presales.domain.repository.BusinessUnitRepository;
import com.sedin.presales.domain.repository.DocumentTypeRepository;
import com.sedin.presales.domain.repository.DomainRepository;
import com.sedin.presales.domain.repository.IndustryRepository;
import com.sedin.presales.domain.repository.SbuRepository;
import com.sedin.presales.domain.repository.TechnologyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MasterServiceTest {

    @Mock
    private DomainRepository domainRepository;

    @Mock
    private IndustryRepository industryRepository;

    @Mock
    private TechnologyRepository technologyRepository;

    @Mock
    private DocumentTypeRepository documentTypeRepository;

    @Mock
    private BusinessUnitRepository businessUnitRepository;

    @Mock
    private SbuRepository sbuRepository;

    private MasterService masterService;

    @BeforeEach
    void setUp() {
        masterService = new MasterService(
                domainRepository,
                industryRepository,
                technologyRepository,
                documentTypeRepository,
                businessUnitRepository,
                sbuRepository
        );
    }

    @Test
    @DisplayName("list should return paged response for valid type")
    void list_shouldReturnPagedResponse() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        Domain domain = Domain.builder()
                .name("Finance")
                .description("Finance domain")
                .isActive(true)
                .build();
        domain.setId(id);
        domain.setCreatedAt(now);
        domain.setUpdatedAt(now);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Domain> page = new PageImpl<>(List.of(domain), pageable, 1);

        when(domainRepository.findAll(pageable)).thenReturn(page);

        PagedResponse<MasterDto> response = masterService.list("domains", pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getName()).isEqualTo("Finance");
        assertThat(response.getContent().get(0).getDescription()).isEqualTo("Finance domain");
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getPage()).isZero();
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.isLast()).isTrue();
    }

    @Test
    @DisplayName("getById should return MasterDto when entity exists")
    void getById_shouldReturnMasterDto() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        Domain domain = Domain.builder()
                .name("Finance")
                .description("Finance domain")
                .isActive(true)
                .build();
        domain.setId(id);
        domain.setCreatedAt(now);
        domain.setUpdatedAt(now);

        when(domainRepository.findById(id)).thenReturn(Optional.of(domain));

        MasterDto result = masterService.getById("domains", id);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getName()).isEqualTo("Finance");
        assertThat(result.getDescription()).isEqualTo("Finance domain");
        assertThat(result.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("getById should throw ResourceNotFoundException when entity not found")
    void getById_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(domainRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> masterService.getById("domains", id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("create should save entity and return MasterDto")
    void create_shouldSaveAndReturnDto() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        CreateMasterRequest request = CreateMasterRequest.builder()
                .name("Healthcare")
                .description("Healthcare domain")
                .build();

        Domain savedDomain = Domain.builder()
                .name("Healthcare")
                .description("Healthcare domain")
                .isActive(true)
                .build();
        savedDomain.setId(id);
        savedDomain.setCreatedAt(now);
        savedDomain.setUpdatedAt(now);

        when(domainRepository.save(any())).thenReturn(savedDomain);

        MasterDto result = masterService.create("domains", request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getName()).isEqualTo("Healthcare");
        assertThat(result.getDescription()).isEqualTo("Healthcare domain");
        verify(domainRepository).save(any());
    }

    @Test
    @DisplayName("update should update fields and return MasterDto")
    void update_shouldUpdateAndReturnDto() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        Domain existingDomain = Domain.builder()
                .name("Old Name")
                .description("Old Desc")
                .isActive(true)
                .build();
        existingDomain.setId(id);
        existingDomain.setCreatedAt(now);
        existingDomain.setUpdatedAt(now);

        UpdateMasterRequest request = UpdateMasterRequest.builder()
                .name("New Name")
                .description("New Desc")
                .build();

        when(domainRepository.findById(id)).thenReturn(Optional.of(existingDomain));
        when(domainRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MasterDto result = masterService.update("domains", id, request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getDescription()).isEqualTo("New Desc");
        verify(domainRepository).findById(id);
        verify(domainRepository).save(any());
    }

    @Test
    @DisplayName("delete should call deleteById when entity exists")
    void delete_shouldDeleteWhenExists() {
        UUID id = UUID.randomUUID();
        when(domainRepository.existsById(id)).thenReturn(true);

        masterService.delete("domains", id);

        verify(domainRepository).deleteById(id);
    }

    @Test
    @DisplayName("delete should throw ResourceNotFoundException when entity not found")
    void delete_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(domainRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> masterService.delete("domains", id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("list should throw ResourceNotFoundException for invalid type")
    void list_shouldThrowForInvalidType() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> masterService.list("invalid-type", pageable))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
