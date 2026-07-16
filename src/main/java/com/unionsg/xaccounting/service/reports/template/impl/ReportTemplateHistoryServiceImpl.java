package com.unionsg.xaccounting.service.reports.template.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unionsg.xaccounting.dto.reports.ReportTemplateHistoryDto;
import com.unionsg.xaccounting.entity.reports.ReportTemplateHistory;
import com.unionsg.xaccounting.repository.reports.ReportTemplateHistoryRepository;
import com.unionsg.xaccounting.service.reports.template.ReportTemplateHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportTemplateHistoryServiceImpl implements ReportTemplateHistoryService {

    private final ReportTemplateHistoryRepository historyRepository;
    private final ObjectMapper objectMapper;

    @Override
    public List<ReportTemplateHistoryDto> getHistory(Long templateId) {
        List<ReportTemplateHistory> history = historyRepository.findByTemplateIdOrderByPerformedAtAsc(templateId);

        return history.stream()
                .map(h -> new ReportTemplateHistoryDto(
                        h.getId(),
                        h.getTemplateId(),
                        h.getAction(),
                        h.getPerformedBy(),
                        h.getPerformedAt(),
                        normalizeMetadata(h.getMetadata())
                ))
                .toList();
    }

    private String normalizeMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) return null;
        try {
            // Ensure stored JSON is valid and keep stable formatting
            Object obj = objectMapper.readValue(metadata, Object.class);
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException ex) {
            // If metadata is not JSON, keep as-is
            return metadata;
        }
    }
}

