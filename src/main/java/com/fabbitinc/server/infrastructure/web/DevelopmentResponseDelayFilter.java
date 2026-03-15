package com.fabbitinc.server.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

//@Component
//@Order(Ordered.HIGHEST_PRECEDENCE)
//public class DevelopmentResponseDelayFilter extends OncePerRequestFilter {
//
//    private static final long RESPONSE_DELAY_MS = 600L;
//
//    @Override
//    protected boolean shouldNotFilter(HttpServletRequest request) {
//        String path = request.getRequestURI();
//        return path.startsWith("/actuator")
//                || path.startsWith("/swagger-ui")
//                || path.startsWith("/v3/api-docs");
//    }
//
//    @Override
//    protected void doFilterInternal(
//            HttpServletRequest request,
//            @NonNull HttpServletResponse response,
//            @NonNull FilterChain filterChain
//    ) throws ServletException, IOException {
//        try {
//            Thread.sleep(RESPONSE_DELAY_MS);
//        } catch (InterruptedException ex) {
//            Thread.currentThread().interrupt();
//            throw new ServletException("응답 지연 처리 중 인터럽트가 발생했습니다", ex);
//        }
//
//        filterChain.doFilter(request, response);
//    }
//}
