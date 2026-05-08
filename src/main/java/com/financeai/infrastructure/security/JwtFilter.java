package com.financeai.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        System.out.println("Authorization Header: " + request.getHeader("Authorization"));
        var authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        var token = authHeader.substring(7);

        System.out.println("--- DEBUG JWT FILTER ---");
        System.out.println("Token extraído: " + token);
        boolean isValid = jwtService.isValid(token);
        System.out.println("O token é válido segundo o JwtService? " + isValid);

        if (isValid) {
            String email = jwtService.extractEmail(token);
            System.out.println("Email extraído do token: " + email);
        }

        if (jwtService.isValid(token) &&
            SecurityContextHolder.getContext().getAuthentication() == null) {

            var email = jwtService.extractEmail(token);
            var userDetails = userDetailsService.loadUserByUsername(email);

            var auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
            );
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        chain.doFilter(request, response);
    }
}
