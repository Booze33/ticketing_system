package com.booze.ticketing_system.config;

import com.booze.ticketing_system.entity.AdminPrincipal;
import com.booze.ticketing_system.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class ApiRateLimitFilter extends OncePerRequestFilter {
    private static final long LOG_INTERVAL_MILLIS = 30_000;

    private final RateLimitService rateLimitService;
    private final boolean failOpen;
    private final AtomicLong lastFailureLogMillis = new AtomicLong(0);

    public ApiRateLimitFilter(
            RateLimitService rateLimitService,
            @Value("${app.rate-limit.fail-open:false}") boolean failOpen) {
        this.rateLimitService = rateLimitService;
        this.failOpen = failOpen;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(request.getContextPath() + "/api/")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String identity = identity(request);
        RateLimitService.RateLimitResult result;
        try {
            result = rateLimitService.consume(identity);
        } catch (RuntimeException exception) {
            // Redis down, refused, timed out, wrong password, factory stopped, bad script reply...
            // Whatever the cause, the limiter could not give an answer.
            logLimiterFailure(exception);
            if (failOpen) {
                filterChain.doFilter(request, response);
            } else {
                writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Rate limiter unavailable");
            }
            return;
        }

        response.setHeader("X-RateLimit-Remaining", Long.toString(result.remaining()));
        response.setHeader("X-RateLimit-Reset", Long.toString(result.resetEpochSeconds()));
        if (!result.allowed()) {
            writeError(response, 429, "Rate limit exceeded");
            return;
        }
        filterChain.doFilter(request, response);
    }

    /** Logs at most once per 30s so a Redis outage does not bury the real error under thousands of stack traces. */
    private void logLimiterFailure(RuntimeException exception) {
        long now = System.currentTimeMillis();
        long last = lastFailureLogMillis.get();
        if (now - last >= LOG_INTERVAL_MILLIS && lastFailureLogMillis.compareAndSet(last, now)) {
            log.error("Rate limiter (Redis) unavailable; {} API requests. "
                            + "Check that Redis is running and REDIS_HOST / REDIS_PORT are correct.",
                    failOpen ? "ALLOWING" : "REJECTING", exception);
        }
    }

    private static String identity(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AdminPrincipal user) {
            return "user:" + user.getId();
        }
        return "ip:" + request.getRemoteAddr();
    }

    private static void writeError(HttpServletResponse response, int status, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":" + status + ",\"title\":\""
                + (status == 429 ? "Too Many Requests" : "Service Unavailable")
                + "\",\"detail\":\"" + detail + "\"}");
    }
}
