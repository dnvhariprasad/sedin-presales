package com.sedin.presales.application.service;

import com.sedin.presales.application.dto.CreateUserRequest;
import com.sedin.presales.application.dto.PagedResponse;
import com.sedin.presales.application.dto.UpdateUserRequest;
import com.sedin.presales.application.dto.UserDto;
import com.sedin.presales.application.exception.BadRequestException;
import com.sedin.presales.application.exception.ResourceNotFoundException;
import com.sedin.presales.domain.entity.User;
import com.sedin.presales.domain.enums.Role;
import com.sedin.presales.domain.enums.UserStatus;
import com.sedin.presales.domain.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserDto create(CreateUserRequest request) {
        log.debug("Creating user with email: {}", request.getEmail());

        userRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
            throw new BadRequestException("User with email '" + request.getEmail() + "' already exists");
        });

        User user = User.builder()
                .email(request.getEmail())
                .displayName(request.getDisplayName())
                .role(request.getRole())
                .status(UserStatus.ACTIVE)
                .avatarUrl(request.getAvatarUrl())
                .build();

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        User saved = userRepository.save(user);
        log.debug("User created with id: {}", saved.getId());
        return toDto(saved);
    }

    @Transactional
    public UserDto update(UUID id, UpdateUserRequest request) {
        log.debug("Updating user with id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        User saved = userRepository.save(user);
        log.debug("User updated with id: {}", saved.getId());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public UserDto getById(UUID id) {
        log.debug("Getting user by id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return toDto(user);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserDto> list(Pageable pageable, Role role, UserStatus status, String search) {
        log.debug("Listing users with role={}, status={}, search={}", role, status, search);

        Specification<User> spec = Specification.where(null);

        if (role != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("role"), role));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("displayName")), pattern)
            ));
        }

        Page<User> page = userRepository.findAll(spec, pageable);

        return PagedResponse.<UserDto>builder()
                .content(page.getContent().stream().map(this::toDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional
    public UserDto deactivate(UUID id) {
        log.debug("Deactivating user with id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setStatus(UserStatus.INACTIVE);
        User saved = userRepository.save(user);
        return toDto(saved);
    }

    @Transactional
    public UserDto activate(UUID id) {
        log.debug("Activating user with id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setStatus(UserStatus.ACTIVE);
        User saved = userRepository.save(user);
        return toDto(saved);
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .status(user.getStatus())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
