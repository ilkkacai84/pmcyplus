package com.rcai.pm.audit;

import com.rcai.pm.user.UserAccount;
import com.rcai.pm.user.UserAccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AuditService {
    private final AuditLogRepository logs;
    private final UserAccountRepository users;

    public AuditService(AuditLogRepository logs, UserAccountRepository users) {
        this.logs = logs;
        this.users = users;
    }

    public List<AuditView> list() {
        return logs.findTop200ByOrderByCreatedAtDesc().stream().map(AuditView::from).toList();
    }

    @Transactional
    public void log(Authentication authentication, String action, String objectType, Long objectId,
                    Map<String, ?> details) {
        UserAccount actor = users.findByUsernameIgnoreCase(authentication.getName()).orElseThrow();
        log(actor, action, objectType, objectId, details);
    }

    @Transactional
    public void log(UserAccount actor, String action, String objectType, Long objectId, Map<String, ?> details) {
        logs.save(new AuditLog(actor, action, objectType, objectId, toJson(details)));
    }

    private String toJson(Map<String, ?> details) {
        return details.entrySet().stream()
            .map(entry -> quote(entry.getKey()) + ":" + value(entry.getValue()))
            .collect(java.util.stream.Collectors.joining(",", "{", "}"));
    }

    private String value(Object value) {
        if (value == null) return "null";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        return quote(value.toString());
    }

    private String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    public record AuditView(Long id, Long actorId, String actorName, String action, String objectType,
                            Long objectId, String detailJson, Instant createdAt) {
        static AuditView from(AuditLog log) {
            UserAccount actor = log.getActor();
            return new AuditView(log.getId(), actor == null ? null : actor.getId(),
                actor == null ? "系统" : actor.getDisplayName(), log.getAction(), log.getObjectType(),
                log.getObjectId(), log.getDetailJson(), log.getCreatedAt());
        }
    }
}
