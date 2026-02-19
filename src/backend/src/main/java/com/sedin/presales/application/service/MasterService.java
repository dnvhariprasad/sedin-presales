package com.sedin.presales.application.service;

import com.sedin.presales.application.dto.CreateMasterRequest;
import com.sedin.presales.application.dto.MasterDto;
import com.sedin.presales.application.dto.PagedResponse;
import com.sedin.presales.application.dto.UpdateMasterRequest;
import com.sedin.presales.application.exception.ResourceNotFoundException;
import com.sedin.presales.domain.entity.BaseEntity;
import com.sedin.presales.domain.entity.BusinessUnit;
import com.sedin.presales.domain.entity.DocumentType;
import com.sedin.presales.domain.entity.Domain;
import com.sedin.presales.domain.entity.Industry;
import com.sedin.presales.domain.entity.Sbu;
import com.sedin.presales.domain.entity.Technology;
import com.sedin.presales.domain.repository.BusinessUnitRepository;
import com.sedin.presales.domain.repository.DocumentTypeRepository;
import com.sedin.presales.domain.repository.DomainRepository;
import com.sedin.presales.domain.repository.IndustryRepository;
import com.sedin.presales.domain.repository.SbuRepository;
import com.sedin.presales.domain.repository.TechnologyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class MasterService {

    private final Map<String, JpaRepository<? extends BaseEntity, UUID>> repositoryMap;

    public MasterService(DomainRepository domainRepository,
                         IndustryRepository industryRepository,
                         TechnologyRepository technologyRepository,
                         DocumentTypeRepository documentTypeRepository,
                         BusinessUnitRepository businessUnitRepository,
                         SbuRepository sbuRepository) {
        this.repositoryMap = Map.of(
                "domains", domainRepository,
                "industries", industryRepository,
                "technologies", technologyRepository,
                "document-types", documentTypeRepository,
                "business-units", businessUnitRepository,
                "sbus", sbuRepository
        );
    }

    public PagedResponse<MasterDto> list(String type, Pageable pageable) {
        log.debug("Listing master entities of type: {}", type);
        JpaRepository<? extends BaseEntity, UUID> repository = getRepository(type);

        Page<? extends BaseEntity> page = repository.findAll(pageable);

        return PagedResponse.<MasterDto>builder()
                .content(page.getContent().stream().map(this::toDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    public MasterDto getById(String type, UUID id) {
        log.debug("Getting master entity of type: {} with id: {}", type, id);
        JpaRepository<? extends BaseEntity, UUID> repository = getRepository(type);

        BaseEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(type, "id", id));

        return toDto(entity);
    }

    @Transactional
    public MasterDto create(String type, CreateMasterRequest request) {
        log.info("Creating master entity of type: {} with name: {}", type, request.getName());
        JpaRepository<? extends BaseEntity, UUID> repository = getRepository(type);

        BaseEntity entity = createEntity(type, request.getName(), request.getDescription());

        @SuppressWarnings("unchecked")
        JpaRepository<BaseEntity, UUID> repo = (JpaRepository<BaseEntity, UUID>) repository;
        BaseEntity saved = repo.save(entity);

        log.info("Created master entity of type: {} with id: {}", type, saved.getId());
        return toDto(saved);
    }

    @Transactional
    public MasterDto update(String type, UUID id, UpdateMasterRequest request) {
        log.info("Updating master entity of type: {} with id: {}", type, id);
        JpaRepository<? extends BaseEntity, UUID> repository = getRepository(type);

        BaseEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(type, "id", id));

        updateEntity(entity, request.getName(), request.getDescription());

        @SuppressWarnings("unchecked")
        JpaRepository<BaseEntity, UUID> repo = (JpaRepository<BaseEntity, UUID>) repository;
        BaseEntity saved = repo.save(entity);

        log.info("Updated master entity of type: {} with id: {}", type, saved.getId());
        return toDto(saved);
    }

    @Transactional
    public void delete(String type, UUID id) {
        log.info("Deleting master entity of type: {} with id: {}", type, id);
        JpaRepository<? extends BaseEntity, UUID> repository = getRepository(type);

        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException(type, "id", id);
        }

        repository.deleteById(id);
        log.info("Deleted master entity of type: {} with id: {}", type, id);
    }

    private JpaRepository<? extends BaseEntity, UUID> getRepository(String type) {
        JpaRepository<? extends BaseEntity, UUID> repository = repositoryMap.get(type);
        if (repository == null) {
            throw new ResourceNotFoundException("Master type", "type", type);
        }
        return repository;
    }

    private BaseEntity createEntity(String type, String name, String description) {
        return switch (type) {
            case "domains" -> Domain.builder().name(name).description(description).isActive(true).build();
            case "industries" -> Industry.builder().name(name).description(description).isActive(true).build();
            case "technologies" -> Technology.builder().name(name).description(description).isActive(true).build();
            case "document-types" -> DocumentType.builder().name(name).description(description).isActive(true).build();
            case "business-units" -> BusinessUnit.builder().name(name).description(description).isActive(true).build();
            case "sbus" -> Sbu.builder().name(name).description(description).isActive(true).build();
            default -> throw new ResourceNotFoundException("Master type", "type", type);
        };
    }

    private void updateEntity(BaseEntity entity, String name, String description) {
        switch (entity) {
            case Domain d -> { d.setName(name); d.setDescription(description); }
            case Industry i -> { i.setName(name); i.setDescription(description); }
            case Technology t -> { t.setName(name); t.setDescription(description); }
            case DocumentType dt -> { dt.setName(name); dt.setDescription(description); }
            case BusinessUnit bu -> { bu.setName(name); bu.setDescription(description); }
            case Sbu s -> { s.setName(name); s.setDescription(description); }
            default -> throw new ResourceNotFoundException("Master type", "type", entity.getClass().getSimpleName());
        }
    }

    private MasterDto toDto(BaseEntity entity) {
        String name = null;
        String description = null;

        switch (entity) {
            case Domain d -> { name = d.getName(); description = d.getDescription(); }
            case Industry i -> { name = i.getName(); description = i.getDescription(); }
            case Technology t -> { name = t.getName(); description = t.getDescription(); }
            case DocumentType dt -> { name = dt.getName(); description = dt.getDescription(); }
            case BusinessUnit bu -> { name = bu.getName(); description = bu.getDescription(); }
            case Sbu s -> { name = s.getName(); description = s.getDescription(); }
            default -> throw new ResourceNotFoundException("Master type", "type", entity.getClass().getSimpleName());
        }

        return MasterDto.builder()
                .id(entity.getId())
                .name(name)
                .description(description)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
