package com.unionsg.xaccounting.service.reports.template.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unionsg.xaccounting.entity.reports.ReportTemplateHistory;
import com.unionsg.xaccounting.enums.ReportTemplateHistoryAction;
import com.unionsg.xaccounting.repository.reports.ReportTemplateHistoryRepository;
import com.unionsg.xaccounting.security.auth.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportTemplateAuditService {

    private final ReportTemplateHistoryRepository historyRepository;
    private final ObjectMapper objectMapper;

    public void record(Long templateId,
                        ReportTemplateHistoryAction action,
                        Map<String, Object> metadata) {

        String performedBy = resolveAuthenticatedUserId();

        historyRepository.save(
                ReportTemplateHistory.builder()
                        .templateId(templateId)
                        .action(action)
                        .performedBy(performedBy)
                        .performedAt(LocalDateTime.now())
                        .metadata(serializeMetadata(metadata))
                        .build()
        );
    }

    public void record(Long templateId,
                        ReportTemplateHistoryAction action) {
        record(templateId, action, null);
    }

    private String resolveAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "UNKNOWN";
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            var userId = userPrincipal.getUsername();
            return userId != null ? userId : "UNKNOWN";
        }

        return "UNKNOWN";
    }

    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            // Fallback to string representation
            return String.valueOf(metadata);
        }
    }
}

