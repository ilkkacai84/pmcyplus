package com.rcai.pm.auth;

import com.rcai.pm.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {
    private final int maxFailures;
    private final Duration lockDuration;
    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(@Value("${app.security.login.max-failures:5}") int maxFailures,
                               @Value("${app.security.login.lock-minutes:15}") long lockMinutes) {
        this.maxFailures = maxFailures;
        this.lockDuration = Duration.ofMinutes(lockMinutes);
    }

    public void checkAllowed(String username, String address) {
        String key = key(username, address);
        Attempt attempt = attempts.get(key);
        if (attempt == null) return;
        if (attempt.lastFailure().plus(lockDuration).isBefore(Instant.now())) {
            attempts.remove(key);
            return;
        }
        if (attempt.failures() >= maxFailures) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "登录失败次数过多，请稍后再试");
        }
    }

    public void recordFailure(String username, String address) {
        attempts.compute(key(username, address), (key, current) ->
            current == null || current.lastFailure().plus(lockDuration).isBefore(Instant.now())
                ? new Attempt(1, Instant.now()) : new Attempt(current.failures() + 1, Instant.now()));
    }

    public void clear(String username, String address) {
        attempts.remove(key(username, address));
    }

    private String key(String username, String address) {
        return username.trim().toLowerCase(Locale.ROOT) + "@" + address;
    }

    private record Attempt(int failures, Instant lastFailure) {}
}
