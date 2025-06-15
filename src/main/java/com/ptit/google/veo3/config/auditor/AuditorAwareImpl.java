package com.ptit.google.veo3.config.auditor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Objects;
import java.util.Optional;


@Slf4j
public class AuditorAwareImpl implements AuditorAware<String> {

    private static final String NAME = "name";
    private static final String PREFERRED_USERNAME = "preferred_username";
    private static final String SUB = "sub";
    private static final String SYSTEM = "system";
    private static final String ANONYMOUS = "anonymous";

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (Objects.isNull(authentication)) {
            return Optional.of(SYSTEM);
        }

        log.info("principal: {}", authentication.getPrincipal());

        if (!this.isAnonymous() && (Objects.nonNull(authentication.getPrincipal()))) {
            if (authentication.getPrincipal() instanceof Jwt jwt) {
                // Ưu tiên lấy từ claim "name" để đồng nhất với JwtTokenService
                String username = jwt.getClaim(NAME);
                if (username == null) {
                    username = jwt.getClaim(PREFERRED_USERNAME);
                }
                if (username == null) {
                    username = jwt.getClaim(SUB);
                }
                return Optional.of(username != null ? username : SYSTEM);
            }

            return Optional.of(authentication.getName());
        }

        return Optional.of(SYSTEM);
    }

    private boolean isAnonymous() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (Objects.isNull(authentication)) {
            return true;
        }
        return authentication.getName().equals(ANONYMOUS);
    }
}