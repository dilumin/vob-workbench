package com.synergen.vobworkbench.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            authenticateAccessToken(header.substring(7));
        }
        filterChain.doFilter(request, response);
    }

    private void authenticateAccessToken(String token) {
        try {
            String username = jwtService.extractUsername(token);
            if (username == null || SecurityContextHolder.getContext().getAuthentication() != null) {
                return;
            }

            var userDetails = userDetailsService.loadUserByUsername(username);
            if (jwtService.isValidAccessToken(token, userDetails)) {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                ));
            }
        } catch (Exception ignored) {
            SecurityContextHolder.clearContext();
        }
    }
}
