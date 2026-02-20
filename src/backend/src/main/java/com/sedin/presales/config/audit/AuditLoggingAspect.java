package com.sedin.presales.config.audit;

import com.sedin.presales.application.service.AuditLogService;
import com.sedin.presales.config.CurrentUserService;
import com.sedin.presales.config.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * AOP aspect that intercepts methods annotated with {@link Audited} and
 * persists an audit log entry capturing who did what, on which resource, and from where.
 */
@Slf4j
@Aspect
@Component
public class AuditLoggingAspect {

    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;

    public AuditLoggingAspect(AuditLogService auditLogService, CurrentUserService currentUserService) {
        this.auditLogService = auditLogService;
        this.currentUserService = currentUserService;
    }

    @Around("@annotation(audited)")
    public Object around(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        String userEmail = resolveUserEmail();
        String action = audited.action();
        String resourceType = audited.resourceType();
        UUID resourceId = extractResourceId(joinPoint);
        String ipAddress = resolveIpAddress();

        try {
            Object result = joinPoint.proceed();

            // Log successful operation
            try {
                auditLogService.log(userEmail, action, resourceType, resourceId, ipAddress, null);
            } catch (Exception ex) {
                log.warn("Failed to write audit log for action={}: {}", action, ex.getMessage());
            }

            return result;
        } catch (Throwable ex) {
            // Log failed operation
            try {
                String details = "FAILED: " + ex.getClass().getSimpleName();
                auditLogService.log(userEmail, action, resourceType, resourceId, ipAddress, details);
            } catch (Exception auditEx) {
                log.warn("Failed to write audit log for failed action={}: {}", action, auditEx.getMessage());
            }
            throw ex;
        }
    }

    /**
     * Resolves the current user's email from the security context.
     * Falls back to "anonymous" when no user is authenticated (e.g., dev mode, public endpoints).
     */
    private String resolveUserEmail() {
        try {
            UserPrincipal principal = currentUserService.getCurrentUser();
            if (principal != null && principal.getEmail() != null) {
                return principal.getEmail();
            }
        } catch (Exception ex) {
            log.debug("Could not resolve current user for audit log: {}", ex.getMessage());
        }
        return "anonymous";
    }

    /**
     * Extracts the first UUID-typed {@link PathVariable} parameter from the method arguments.
     * For create/upload methods that don't have a path UUID, returns null.
     */
    private UUID extractResourceId(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Annotation[][] paramAnnotations = method.getParameterAnnotations();
            Object[] args = joinPoint.getArgs();

            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof UUID) {
                    for (Annotation annotation : paramAnnotations[i]) {
                        if (annotation instanceof PathVariable) {
                            return (UUID) args[i];
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("Could not extract resource ID from method arguments: {}", ex.getMessage());
        }
        return null;
    }

    /**
     * Resolves the client IP address from the current HTTP request.
     */
    private String resolveIpAddress() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String forwarded = request.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isBlank()) {
                    return forwarded.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception ex) {
            log.debug("Could not resolve IP address for audit log: {}", ex.getMessage());
        }
        return null;
    }
}
