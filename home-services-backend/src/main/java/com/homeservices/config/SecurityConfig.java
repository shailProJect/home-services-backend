package com.homeservices.config;

import com.homeservices.filter.JwtAuthenticationFilter;
import com.homeservices.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * SecurityConfig — phone/Firebase auth removed.
 * Phone OTP endpoints removed from permitAll list.
 * Cashfree webhook kept public.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter  jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth

                // ── Fully public ──────────────────────────────────────────────
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/categories/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/user/providers/*/reviews").permitAll()
                // Cashfree calls this server-to-server — no JWT
                .requestMatchers("/provider/subscription/webhook").permitAll()

                // ── Admin ─────────────────────────────────────────────────────
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/uploads/provider-docs/**").hasRole("ADMIN")

                // ── Provider ──────────────────────────────────────────────────
                .requestMatchers("/provider/**").hasRole("PROVIDER")

                // ── Shared (USER or PROVIDER) ─────────────────────────────────
                .requestMatchers(HttpMethod.GET,  "/user/profile").hasAnyRole("USER", "PROVIDER")
                .requestMatchers(HttpMethod.PUT,  "/user/profile").hasAnyRole("USER", "PROVIDER")
                .requestMatchers(HttpMethod.PUT,  "/user/profile/photo").hasAnyRole("USER", "PROVIDER")
                .requestMatchers(HttpMethod.PUT,  "/user/phone").hasAnyRole("USER", "PROVIDER")
                .requestMatchers(HttpMethod.GET,  "/user/reviews/me").hasAnyRole("USER", "PROVIDER")
                .requestMatchers("/user/chat/**").hasAnyRole("USER", "PROVIDER")

                // ── User-only ─────────────────────────────────────────────────
                .requestMatchers("/user/**").hasRole("USER")

                .anyRequest().authenticated()
            )
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService);
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}
