package com.sedin.presales.config.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method for audit logging.
 * The aspect will capture user, action, resource info, and IP address.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Audited {

    /**
     * The audit action name (e.g., "CREATE_DOCUMENT", "DELETE_FOLDER").
     */
    String action();

    /**
     * The resource type being acted upon (e.g., "DOCUMENT", "FOLDER").
     * Defaults to empty string, in which case the aspect may infer it from context.
     */
    String resourceType() default "";
}
