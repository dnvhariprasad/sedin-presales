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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User buildUser() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        User user = User.builder()
                .email("john@sedin.com")
                .displayName("John Doe")
                .role(Role.EDITOR)
                .status(UserStatus.ACTIVE)
                .avatarUrl("https://example.com/avatar.png")
                .build();
        user.setId(id);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return user;
    }

    @Test
    @DisplayName("create should save and return UserDto")
    void create_shouldSaveAndReturnDto() {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("john@sedin.com")
                .displayName("John Doe")
                .role(Role.EDITOR)
                .build();

        User savedUser = buildUser();

        when(userRepository.findByEmail("john@sedin.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserDto result = userService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("john@sedin.com");
        assertThat(result.getDisplayName()).isEqualTo("John Doe");
        assertThat(result.getRole()).isEqualTo(Role.EDITOR);
        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("create should throw BadRequestException when email already exists")
    void create_shouldThrowWhenEmailExists() {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("john@sedin.com")
                .displayName("John Doe")
                .role(Role.EDITOR)
                .build();

        when(userRepository.findByEmail("john@sedin.com")).thenReturn(Optional.of(buildUser()));

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("update should update only non-null fields")
    void update_shouldUpdateOnlyNonNullFields() {
        User existingUser = buildUser();
        UUID id = existingUser.getId();

        UpdateUserRequest request = UpdateUserRequest.builder()
                .displayName("Jane Doe")
                .build();

        when(userRepository.findById(id)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto result = userService.update(id, request);

        assertThat(result).isNotNull();
        assertThat(result.getDisplayName()).isEqualTo("Jane Doe");
        assertThat(result.getRole()).isEqualTo(Role.EDITOR); // unchanged
        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE); // unchanged
    }

    @Test
    @DisplayName("getById should return UserDto when found")
    void getById_shouldReturnDto() {
        User user = buildUser();
        UUID id = user.getId();

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        UserDto result = userService.getById(id);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getEmail()).isEqualTo("john@sedin.com");
    }

    @Test
    @DisplayName("getById should throw ResourceNotFoundException when not found")
    void getById_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list should return paged response")
    void list_shouldReturnPagedResponse() {
        User user = buildUser();
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user), pageable, 1);

        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        PagedResponse<UserDto> result = userService.list(pageable, null, null, null);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("john@sedin.com");
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getPage()).isZero();
        assertThat(result.isLast()).isTrue();
    }

    @Test
    @DisplayName("deactivate should set status to INACTIVE")
    void deactivate_shouldSetStatusToInactive() {
        User user = buildUser();
        UUID id = user.getId();

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto result = userService.deactivate(id);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(UserStatus.INACTIVE);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("activate should set status to ACTIVE")
    void activate_shouldSetStatusToActive() {
        User user = buildUser();
        user.setStatus(UserStatus.INACTIVE);
        UUID id = user.getId();

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto result = userService.activate(id);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(userRepository).save(any(User.class));
    }
}
