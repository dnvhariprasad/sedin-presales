package com.sedin.presales.api.controller;

import com.sedin.presales.application.dto.ApiResponse;
import com.sedin.presales.application.dto.CreateUserRequest;
import com.sedin.presales.application.dto.PagedResponse;
import com.sedin.presales.application.dto.UpdateUserRequest;
import com.sedin.presales.application.dto.UserDto;
import com.sedin.presales.application.service.UserService;
import com.sedin.presales.config.audit.Audited;
import com.sedin.presales.domain.enums.Role;
import com.sedin.presales.domain.enums.UserStatus;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<UserDto>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String search) {
        log.debug("REST request to list users with role={}, status={}, search={}", role, status, search);
        int safePage = Math.max(page, 0);
        int cappedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, cappedSize);
        PagedResponse<UserDto> response = userService.list(pageable, role, status, search);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getById(@PathVariable UUID id) {
        log.debug("REST request to get user by id: {}", id);
        UserDto dto = userService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @Audited(action = "CREATE_USER", resourceType = "USER")
    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> create(@Valid @RequestBody CreateUserRequest request) {
        log.debug("REST request to create user with email: {}", request.getEmail());
        UserDto dto = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dto, "User created successfully"));
    }

    @Audited(action = "UPDATE_USER", resourceType = "USER")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        log.debug("REST request to update user with id: {}", id);
        UserDto dto = userService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(dto, "User updated successfully"));
    }

    @Audited(action = "DEACTIVATE_USER", resourceType = "USER")
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<UserDto>> deactivate(@PathVariable UUID id) {
        log.debug("REST request to deactivate user with id: {}", id);
        UserDto dto = userService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success(dto, "User deactivated successfully"));
    }

    @Audited(action = "ACTIVATE_USER", resourceType = "USER")
    @PutMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<UserDto>> activate(@PathVariable UUID id) {
        log.debug("REST request to activate user with id: {}", id);
        UserDto dto = userService.activate(id);
        return ResponseEntity.ok(ApiResponse.success(dto, "User activated successfully"));
    }
}
