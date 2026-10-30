package com.enterprise.mongodb.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Enterprise AuditorAware provider resolving the current operator/service principal for MongoDB auditing.
 */
@Component("auditorAware")
public class SecurityAuditorAware implements AuditorAware<String> {

    private static final String DEFAULT_SYSTEM_USER = "SYSTEM_PROCESS";
    private static final ThreadLocal<String> CURRENT_USER = new ThreadLocal<>();

    @Override
    @NonNull
    public Optional<String> getCurrentAuditor() {
        // If an explicit context user was set (e.g. from security filter / token), use it;
        // otherwise fallback to the automated background system identifier.
        String user = CURRENT_USER.get();
        return Optional.ofNullable(user != null && !user.isBlank() ? user : DEFAULT_SYSTEM_USER);
    }

    public static void setCurrentUser(String username) {
        CURRENT_USER.set(username);
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
