package com.sedin.presales.config.audit;

import com.sedin.presales.application.service.AuditLogService;
import com.sedin.presales.config.CurrentUserService;
import com.sedin.presales.config.UserPrincipal;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.PathVariable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLoggingAspectTest {

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Audited audited;

    @InjectMocks
    private AuditLoggingAspect auditLoggingAspect;

    @Test
    @DisplayName("should proceed with join point and log audit entry")
    void around_shouldProceedAndLogAuditEntry() throws Throwable {
        Object expectedResult = "success";
        when(joinPoint.proceed()).thenReturn(expectedResult);
        when(audited.action()).thenReturn("CREATE_DOCUMENT");
        when(audited.resourceType()).thenReturn("DOCUMENT");

        UserPrincipal principal = UserPrincipal.builder()
                .userId(UUID.randomUUID().toString())
                .email("admin@sedin.com")
                .displayName("Admin User")
                .role("ADMIN")
                .build();
        when(currentUserService.getCurrentUser()).thenReturn(principal);

        // No MethodSignature mock needed — extractResourceId will catch and return null
        when(joinPoint.getSignature()).thenReturn(null);

        Object result = auditLoggingAspect.around(joinPoint, audited);

        assertThat(result).isEqualTo(expectedResult);
        verify(joinPoint).proceed();
        verify(auditLogService).log(
                eq("admin@sedin.com"),
                eq("CREATE_DOCUMENT"),
                eq("DOCUMENT"),
                isNull(),
                isNull(),
                isNull()
        );
    }

    @Test
    @DisplayName("should use 'anonymous' when CurrentUserService throws exception")
    void around_shouldUseAnonymousWhenCurrentUserServiceThrows() throws Throwable {
        Object expectedResult = "result";
        when(joinPoint.proceed()).thenReturn(expectedResult);
        when(audited.action()).thenReturn("VIEW_DOCUMENT");
        when(audited.resourceType()).thenReturn("DOCUMENT");
        when(currentUserService.getCurrentUser()).thenThrow(new RuntimeException("No auth context"));

        Object result = auditLoggingAspect.around(joinPoint, audited);

        assertThat(result).isEqualTo(expectedResult);
        verify(auditLogService).log(
                eq("anonymous"),
                eq("VIEW_DOCUMENT"),
                eq("DOCUMENT"),
                isNull(),
                isNull(),
                isNull()
        );
    }

    @Test
    @DisplayName("should extract UUID from @PathVariable parameter")
    void around_shouldExtractUuidFromPathVariable() throws Throwable {
        UUID resourceId = UUID.randomUUID();
        Object expectedResult = "ok";

        when(joinPoint.proceed()).thenReturn(expectedResult);
        when(audited.action()).thenReturn("DELETE_DOCUMENT");
        when(audited.resourceType()).thenReturn("DOCUMENT");

        UserPrincipal principal = UserPrincipal.builder()
                .userId(UUID.randomUUID().toString())
                .email("editor@sedin.com")
                .role("EDITOR")
                .build();
        when(currentUserService.getCurrentUser()).thenReturn(principal);

        // Mock MethodSignature and Method to simulate a @PathVariable UUID parameter
        MethodSignature methodSignature = org.mockito.Mockito.mock(MethodSignature.class);
        Method method = SampleController.class.getMethod("deleteDocument", UUID.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{resourceId});

        Object result = auditLoggingAspect.around(joinPoint, audited);

        assertThat(result).isEqualTo(expectedResult);
        verify(auditLogService).log(
                eq("editor@sedin.com"),
                eq("DELETE_DOCUMENT"),
                eq("DOCUMENT"),
                eq(resourceId),
                isNull(),
                isNull()
        );
    }

    @Test
    @DisplayName("should not break request when audit logging fails")
    void around_shouldNotBreakRequestWhenAuditLoggingFails() throws Throwable {
        Object expectedResult = "response-data";
        when(joinPoint.proceed()).thenReturn(expectedResult);
        when(audited.action()).thenReturn("UPDATE_DOCUMENT");
        when(audited.resourceType()).thenReturn("DOCUMENT");

        UserPrincipal principal = UserPrincipal.builder()
                .userId(UUID.randomUUID().toString())
                .email("user@sedin.com")
                .role("EDITOR")
                .build();
        when(currentUserService.getCurrentUser()).thenReturn(principal);

        doThrow(new RuntimeException("DB connection failed"))
                .when(auditLogService).log(anyString(), anyString(), anyString(), any(), any(), any());

        Object result = auditLoggingAspect.around(joinPoint, audited);

        assertThat(result).isEqualTo(expectedResult);
        verify(joinPoint).proceed();
    }

    /**
     * Helper class with a method that has a @PathVariable UUID parameter,
     * used to provide a real Method object for reflection-based tests.
     */
    static class SampleController {
        public void deleteDocument(@PathVariable UUID id) {
            // stub for test
        }
    }
}
