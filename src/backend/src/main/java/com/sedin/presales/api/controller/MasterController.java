package com.sedin.presales.api.controller;

import com.sedin.presales.application.dto.ApiResponse;
import com.sedin.presales.application.dto.CreateMasterRequest;
import com.sedin.presales.application.dto.MasterDto;
import com.sedin.presales.application.dto.PagedResponse;
import com.sedin.presales.application.dto.UpdateMasterRequest;
import com.sedin.presales.application.service.MasterService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/masters/{type}")
public class MasterController {

    private final MasterService masterService;

    public MasterController(MasterService masterService) {
        this.masterService = masterService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<MasterDto>>> list(
            @PathVariable String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("REST request to list master entities of type: {}", type);
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<MasterDto> response = masterService.list(type, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MasterDto>> getById(
            @PathVariable String type,
            @PathVariable UUID id) {
        log.debug("REST request to get master entity of type: {} with id: {}", type, id);
        MasterDto dto = masterService.getById(type, id);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MasterDto>> create(
            @PathVariable String type,
            @Valid @RequestBody CreateMasterRequest request) {
        log.debug("REST request to create master entity of type: {}", type);
        MasterDto dto = masterService.create(type, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dto, "Created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MasterDto>> update(
            @PathVariable String type,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMasterRequest request) {
        log.debug("REST request to update master entity of type: {} with id: {}", type, id);
        MasterDto dto = masterService.update(type, id, request);
        return ResponseEntity.ok(ApiResponse.success(dto, "Updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String type,
            @PathVariable UUID id) {
        log.debug("REST request to delete master entity of type: {} with id: {}", type, id);
        masterService.delete(type, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted successfully"));
    }
}
