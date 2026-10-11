package edu.collegehub.security;

import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/css/**", "/js/**", "/error", "/login", "/register", "/resources/**", "/question-papers", "/api/catalog/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/usage/export.csv").hasRole("ADMIN")
                        .requestMatchers("/uploads/**", "/my-uploads/**", "/profile/**").authenticated()
                        .anyRequest().permitAll())
                .formLogin(form -> form.loginPage("/login").successHandler(loginSuccessHandler()).permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/?loggedOut"))
                .requestCache(cache -> cache.requestCache(requestCache))
                .csrf(Customizer.withDefaults())
                .build();
    }

    private AuthenticationSuccessHandler loginSuccessHandler() {
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        return (HttpServletRequest request, HttpServletResponse response, Authentication authentication)
                -> {
            String next = request.getParameter("next");
            if (next != null && next.startsWith("/") && !next.startsWith("//") && !next.contains("\\")) {
                response.sendRedirect(request.getContextPath() + next);
            } else {
                SavedRequest saved = requestCache.getRequest(request, response);
                if (saved != null) {
                    URI target = URI.create(saved.getRedirectUrl());
                    if (target.getHost() == null || target.getHost().equalsIgnoreCase(request.getServerName())) {
                        response.sendRedirect(saved.getRedirectUrl());
                        requestCache.removeRequest(request, response);
                        return;
                    }
                }
                response.sendRedirect(request.getContextPath() + "/");
            }
        };
    }
}
