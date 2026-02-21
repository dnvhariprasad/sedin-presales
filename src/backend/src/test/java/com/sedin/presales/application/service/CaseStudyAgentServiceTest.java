package com.sedin.presales.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sedin.presales.application.dto.CaseStudyAgentDto;
import com.sedin.presales.application.dto.CreateCaseStudyAgentRequest;
import com.sedin.presales.application.dto.PagedResponse;
import com.sedin.presales.application.dto.UpdateCaseStudyAgentRequest;
import com.sedin.presales.application.exception.ResourceNotFoundException;
import com.sedin.presales.domain.entity.CaseStudyAgent;
import com.sedin.presales.domain.repository.CaseStudyAgentRepository;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseStudyAgentServiceTest {

    @Mock
    private CaseStudyAgentRepository caseStudyAgentRepository;

    private CaseStudyAgentService caseStudyAgentService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID testId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        caseStudyAgentService = new CaseStudyAgentService(caseStudyAgentRepository, objectMapper);
    }

    private CaseStudyAgent buildAgent() {
        return CaseStudyAgent.builder()
                .id(testId)
                .name("Test Agent")
                .description("Test Description")
                .templateConfig("{\"version\":\"1.0\"}")
                .isActive(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void create_shouldSaveAndReturnDto() throws JsonProcessingException {
        CreateCaseStudyAgentRequest request = CreateCaseStudyAgentRequest.builder()
                .name("Test Agent")
                .description("Test Description")
                .templateConfig(Map.of("version", "1.0"))
                .build();

        CaseStudyAgent saved = buildAgent();
        saved.setTemplateConfig(objectMapper.writeValueAsString(request.getTemplateConfig()));
        when(caseStudyAgentRepository.save(any(CaseStudyAgent.class))).thenReturn(saved);

        CaseStudyAgentDto result = caseStudyAgentService.create(request);

        assertThat(result.getName()).isEqualTo("Test Agent");
        assertThat(result.getDescription()).isEqualTo("Test Description");
        assertThat(result.getTemplateConfig()).isNotNull();
        verify(caseStudyAgentRepository).save(any(CaseStudyAgent.class));
    }

    @Test
    void create_shouldSetIsActiveFalse() {
        CreateCaseStudyAgentRequest request = CreateCaseStudyAgentRequest.builder()
                .name("Test Agent")
                .description("Test Description")
                .templateConfig(Map.of("version", "1.0"))
                .build();

        CaseStudyAgent saved = buildAgent();
        saved.setIsActive(false);
        when(caseStudyAgentRepository.save(any(CaseStudyAgent.class))).thenReturn(saved);

        CaseStudyAgentDto result = caseStudyAgentService.create(request);

        assertThat(result.isActive()).isFalse();
    }

    @Test
    void update_shouldUpdateNameAndDescription() {
        CaseStudyAgent existing = buildAgent();
        when(caseStudyAgentRepository.findById(testId)).thenReturn(Optional.of(existing));

        CaseStudyAgent updated = buildAgent();
        updated.setName("Updated Name");
        updated.setDescription("Updated Description");
        when(caseStudyAgentRepository.save(any(CaseStudyAgent.class))).thenReturn(updated);

        UpdateCaseStudyAgentRequest request = UpdateCaseStudyAgentRequest.builder()
                .name("Updated Name")
                .description("Updated Description")
                .build();

        CaseStudyAgentDto result = caseStudyAgentService.update(testId, request);

        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getDescription()).isEqualTo("Updated Description");
    }

    @Test
    void update_shouldThrowWhenNotFound() {
        when(caseStudyAgentRepository.findById(testId)).thenReturn(Optional.empty());

        UpdateCaseStudyAgentRequest request = UpdateCaseStudyAgentRequest.builder()
                .name("Updated Name")
                .build();

        assertThatThrownBy(() -> caseStudyAgentService.update(testId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void activate_shouldDeactivateAllOthersAndActivateTarget() {
        CaseStudyAgent target = buildAgent();
        target.setIsActive(false);

        CaseStudyAgent otherActive = CaseStudyAgent.builder()
                .id(UUID.randomUUID())
                .name("Other Agent")
                .isActive(true)
                .templateConfig("{}")
                .build();

        when(caseStudyAgentRepository.findById(testId)).thenReturn(Optional.of(target));
        when(caseStudyAgentRepository.findByIsActiveTrue()).thenReturn(List.of(otherActive));
        when(caseStudyAgentRepository.saveAll(any())).thenReturn(List.of(otherActive));

        CaseStudyAgent activated = buildAgent();
        activated.setIsActive(true);
        when(caseStudyAgentRepository.save(any(CaseStudyAgent.class))).thenReturn(activated);

        CaseStudyAgentDto result = caseStudyAgentService.activate(testId);

        assertThat(result.isActive()).isTrue();
        verify(caseStudyAgentRepository).saveAll(any());
        verify(caseStudyAgentRepository).save(any(CaseStudyAgent.class));
    }

    @Test
    void activate_shouldThrowWhenNotFound() {
        when(caseStudyAgentRepository.findById(testId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> caseStudyAgentService.activate(testId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deactivate_shouldSetFalse() {
        CaseStudyAgent agent = buildAgent();
        agent.setIsActive(true);
        when(caseStudyAgentRepository.findById(testId)).thenReturn(Optional.of(agent));

        CaseStudyAgent deactivated = buildAgent();
        deactivated.setIsActive(false);
        when(caseStudyAgentRepository.save(any(CaseStudyAgent.class))).thenReturn(deactivated);

        CaseStudyAgentDto result = caseStudyAgentService.deactivate(testId);

        assertThat(result.isActive()).isFalse();
    }

    @Test
    void getById_shouldReturnDto() {
        CaseStudyAgent agent = buildAgent();
        when(caseStudyAgentRepository.findById(testId)).thenReturn(Optional.of(agent));

        CaseStudyAgentDto result = caseStudyAgentService.getById(testId);

        assertThat(result.getId()).isEqualTo(testId);
        assertThat(result.getName()).isEqualTo("Test Agent");
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(caseStudyAgentRepository.findById(testId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> caseStudyAgentService.getById(testId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getActiveAgent_shouldReturnActiveAgent() {
        CaseStudyAgent agent = buildAgent();
        agent.setIsActive(true);
        when(caseStudyAgentRepository.findFirstByIsActiveTrue()).thenReturn(Optional.of(agent));

        CaseStudyAgentDto result = caseStudyAgentService.getActiveAgent();

        assertThat(result.isActive()).isTrue();
    }

    @Test
    void getActiveAgent_shouldThrowWhenNoneActive() {
        when(caseStudyAgentRepository.findFirstByIsActiveTrue()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> caseStudyAgentService.getActiveAgent())
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void list_shouldReturnPagedResponse() {
        CaseStudyAgent agent = buildAgent();
        Pageable pageable = PageRequest.of(0, 10);
        Page<CaseStudyAgent> page = new PageImpl<>(List.of(agent), pageable, 1);

        when(caseStudyAgentRepository.findAll(pageable)).thenReturn(page);

        PagedResponse<CaseStudyAgentDto> result = caseStudyAgentService.list(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Test Agent");
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.isLast()).isTrue();
    }
}
