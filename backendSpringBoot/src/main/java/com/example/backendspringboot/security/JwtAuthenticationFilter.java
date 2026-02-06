package com.example.backendspringboot.security;

import com.example.backendspringboot.model.Administrator;
import com.example.backendspringboot.model.Driver;
import com.example.backendspringboot.model.Passenger;
import com.example.backendspringboot.model.User;
import com.example.backendspringboot.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // preskoči filter za otvorene endpoint-e
        if (path.startsWith("/auth/") || path.equals("/api/drivers/complete-registration")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // ukloni "Bearer "
            if (jwtUtil.isTokenValid(token)) {
                Long userId = jwtUtil.extractUserId(token);
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {

                    // Determine role based on class
                    String tokenRole = jwtUtil.extractRole(token);
                    String role = mapRoleFromToken(tokenRole);
                    List<SimpleGrantedAuthority> authorities = List.of(
                            new SimpleGrantedAuthority("ROLE_" + role)
                    );

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    System.out.println("User not found in DB");
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    // Helper
    private String mapRoleFromToken(String roleFromToken) {
        switch(roleFromToken) {
            case "Administrator": return "ADMIN";
            case "Driver": return "DRIVER";
            case "Passenger": return "USER";
            default: return "";
        }
    }
}