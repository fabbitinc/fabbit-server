package com.fabbitinc.server.infrastructure.security;

import com.fabbitinc.server.application.config.AppProperties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityWebConfig {

    private static final String[] PUBLIC_PATHS = {
            "/health",
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/check-email",
            "/api/v1/auth/send-verification",
            "/api/v1/auth/verify-email",
            "/api/v1/auth/invitations/verify",
            "/api/v1/auth/accept-invitation",
            "/api/v1/auth/site",
            "/api/v1/auth/plans",
            "/api/v1/auth/check-slug"
    };

    private static final String[] SWAGGER_PATHS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/openapi.json",
            "/openapi.json/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/redoc"
    };

    private final JwtSecurityContextFilter jwtSecurityContextFilter;
    private final SecurityAuthenticationEntryPoint securityAuthenticationEntryPoint;
    private final SecurityAccessDeniedHandler securityAccessDeniedHandler;
    private final AppProperties appProperties;

    @Value("${springdoc.api-docs.enabled:true}")
    private boolean swaggerEnabled;

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("""
                ROLE_OWNER > ROLE_ADMIN
                ROLE_ADMIN > ROLE_MEMBER
                """);
    }



    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        Pattern allowedOriginPattern = Pattern.compile(
                "^https?://([\\w-]+\\.)?" + Pattern.quote(appProperties.baseDomain()) + "(:\\d+)?$"
        );

        return request -> {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowCredentials(true);
            config.setAllowedMethods(List.of("*"));
            config.setAllowedHeaders(List.of("*"));
            config.setExposedHeaders(List.of("Content-Disposition"));

            String origin = request.getHeader(HttpHeaders.ORIGIN);
            if (origin != null && allowedOriginPattern.matcher(origin).matches()) {
                config.setAllowedOrigins(List.of(origin));
            }

            return config;
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> {
                    List<String> paths = new ArrayList<>(Arrays.asList(PUBLIC_PATHS));
                    if (swaggerEnabled) {
                        paths.addAll(Arrays.asList(SWAGGER_PATHS));
                    }
                    authorize.requestMatchers(paths.toArray(String[]::new)).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/organizations").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated();
                })
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(securityAuthenticationEntryPoint)
                        .accessDeniedHandler(securityAccessDeniedHandler)
                )
                .addFilterBefore(jwtSecurityContextFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
