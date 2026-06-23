package com.unionsg.xaccounting.service;

import com.unionsg.xaccounting.dto.ChartClearToResponseDto;
import com.unionsg.xaccounting.entity.ChartOfAccountClearTo_ENTITY;
import com.unionsg.xaccounting.repository.ChartOfAccountClearToQueriesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChartOfAccountClearToService {

    private final ChartOfAccountClearToQueriesRepository clearToRepo;

    @Transactional(readOnly = true)
    public ChartClearToResponseDto getByClearToCode(Long clearToCode) {
        ChartOfAccountClearTo_ENTITY entity = clearToRepo.findActiveByClearToCode(clearToCode)
                .orElseThrow(() -> new RuntimeException("Chart clear-to not found for code: " + clearToCode));
        return convert(entity);
    }

    @Transactional(readOnly = true)
    public List<ChartClearToResponseDto> getAllActive() {
        return clearToRepo.findAllActive().stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    private ChartClearToResponseDto convert(ChartOfAccountClearTo_ENTITY e) {
        return ChartClearToResponseDto.builder()
                .id(e.getId())
                .clearToCode(e.getClearToCode())
                .chartCode(e.getChartOfAccount() != null ? e.getChartOfAccount().getCoaCode() : null)
                .description(e.getDescription())
                .dateCreated(e.getDateCreated())
                .deleted(e.getDeleted())
                .deletedBy(e.getDeletedB())
                .dateDeleted(e.getDateDeleted())
                .build();
    }
}

