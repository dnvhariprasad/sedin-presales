package com.sedin.presales.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sedin.presales.application.dto.CaseStudyAgentDto;
import com.sedin.presales.application.dto.CreateCaseStudyAgentRequest;
import com.sedin.presales.application.dto.PagedResponse;
import com.sedin.presales.application.dto.UpdateCaseStudyAgentRequest;
import com.sedin.presales.application.exception.BadRequestException;
import com.sedin.presales.application.exception.ResourceNotFoundException;
import com.sedin.presales.domain.entity.CaseStudyAgent;
import com.sedin.presales.domain.repository.CaseStudyAgentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class CaseStudyAgentService {

    private final CaseStudyAgentRepository caseStudyAgentRepository;
    private final ObjectMapper objectMapper;

    public CaseStudyAgentService(CaseStudyAgentRepository caseStudyAgentRepository, ObjectMapper objectMapper) {
        this.caseStudyAgentRepository = caseStudyAgentRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CaseStudyAgentDto create(CreateCaseStudyAgentRequest request) {
        log.info("Creating case study agent with name: {}", request.getName());

        String templateConfigJson = serializeTemplateConfig(request.getTemplateConfig());

        CaseStudyAgent agent = CaseStudyAgent.builder()
                .name(request.getName())
                .description(request.getDescription())
                .templateConfig(templateConfigJson)
                .isActive(false)
                .build();

        CaseStudyAgent saved = caseStudyAgentRepository.save(agent);
        log.info("Created case study agent with id: {}", saved.getId());
        return toDto(saved);
    }

    @Transactional
    public CaseStudyAgentDto update(UUID id, UpdateCaseStudyAgentRequest request) {
        log.info("Updating case study agent with id: {}", id);

        CaseStudyAgent agent = caseStudyAgentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CaseStudyAgent", "id", id));

        if (request.getName() != null) {
            agent.setName(request.getName());
        }
        if (request.getDescription() != null) {
            agent.setDescription(request.getDescription());
        }
        if (request.getTemplateConfig() != null) {
            agent.setTemplateConfig(serializeTemplateConfig(request.getTemplateConfig()));
        }

        CaseStudyAgent saved = caseStudyAgentRepository.save(agent);
        log.info("Updated case study agent with id: {}", saved.getId());
        return toDto(saved);
    }

    @Transactional
    public void delete(UUID id) {
        log.info("Deleting case study agent with id: {}", id);

        if (!caseStudyAgentRepository.existsById(id)) {
            throw new ResourceNotFoundException("CaseStudyAgent", "id", id);
        }

        caseStudyAgentRepository.deleteById(id);
        log.info("Deleted case study agent with id: {}", id);
    }

    public CaseStudyAgentDto getById(UUID id) {
        log.debug("Getting case study agent with id: {}", id);

        CaseStudyAgent agent = caseStudyAgentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CaseStudyAgent", "id", id));

        return toDto(agent);
    }

    public PagedResponse<CaseStudyAgentDto> list(Pageable pageable) {
        log.debug("Listing case study agents");

        Page<CaseStudyAgent> page = caseStudyAgentRepository.findAll(pageable);

        return PagedResponse.<CaseStudyAgentDto>builder()
                .content(page.getContent().stream().map(this::toDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional
    public CaseStudyAgentDto activate(UUID id) {
        log.info("Activating case study agent with id: {}", id);

        CaseStudyAgent target = caseStudyAgentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CaseStudyAgent", "id", id));

        List<CaseStudyAgent> activeAgents = caseStudyAgentRepository.findByIsActiveTrue();
        for (CaseStudyAgent agent : activeAgents) {
            agent.setIsActive(false);
        }
        caseStudyAgentRepository.saveAll(activeAgents);

        target.setIsActive(true);
        CaseStudyAgent saved = caseStudyAgentRepository.save(target);
        log.info("Activated case study agent with id: {}", saved.getId());
        return toDto(saved);
    }

    @Transactional
    public CaseStudyAgentDto deactivate(UUID id) {
        log.info("Deactivating case study agent with id: {}", id);

        CaseStudyAgent agent = caseStudyAgentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CaseStudyAgent", "id", id));

        agent.setIsActive(false);
        CaseStudyAgent saved = caseStudyAgentRepository.save(agent);
        log.info("Deactivated case study agent with id: {}", saved.getId());
        return toDto(saved);
    }

    public CaseStudyAgentDto getActiveAgent() {
        log.debug("Getting active case study agent");

        CaseStudyAgent agent = caseStudyAgentRepository.findFirstByIsActiveTrue()
                .orElseThrow(() -> new ResourceNotFoundException("CaseStudyAgent", "status", "active"));

        return toDto(agent);
    }

    private String serializeTemplateConfig(Object templateConfig) {
        try {
            return objectMapper.writeValueAsString(templateConfig);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Invalid template config: " + e.getMessage());
        }
    }

    private CaseStudyAgentDto toDto(CaseStudyAgent agent) {
        Object templateConfigObj = null;
        if (agent.getTemplateConfig() != null) {
            try {
                templateConfigObj = objectMapper.readValue(agent.getTemplateConfig(), Object.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize template config for agent {}: {}", agent.getId(), e.getMessage());
                templateConfigObj = agent.getTemplateConfig();
            }
        }

        return CaseStudyAgentDto.builder()
                .id(agent.getId())
                .name(agent.getName())
                .description(agent.getDescription())
                .templateConfig(templateConfigObj)
                .isActive(agent.getIsActive())
                .createdAt(agent.getCreatedAt())
                .updatedAt(agent.getUpdatedAt())
                .build();
    }
}
