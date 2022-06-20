package com.amaxilatis.metis.server.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SimpleAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest arg0, HttpServletResponse arg1, Authentication authentication) {
        
        final Set<String> authoritiesSet = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        log.info("authoritiesSet:{}", authoritiesSet);
        if (authoritiesSet.contains("ADMIN")) {
            try {
                redirectStrategy.sendRedirect(arg0, arg1, "/");
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        } else if (authoritiesSet.contains("USER")) {
            try {
                redirectStrategy.sendRedirect(arg0, arg1, "/");
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        } else {
            try {
                redirectStrategy.sendRedirect(arg0, arg1, "/");
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }
    
}
