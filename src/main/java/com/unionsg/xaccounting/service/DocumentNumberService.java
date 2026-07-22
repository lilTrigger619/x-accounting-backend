package com.unionsg.xaccounting.service;

import com.unionsg.xaccounting.dto.config.DocumentNumberConfigDto;
import com.unionsg.xaccounting.dto.config.UpsertDocumentNumberConfigDto;
import com.unionsg.xaccounting.entity.DocumentNumberConfig;
import com.unionsg.xaccounting.entity.DocumentSequence;
import com.unionsg.xaccounting.enums.DocumentModule;
import com.unionsg.xaccounting.repository.DocumentNumberConfigRepository;
import com.unionsg.xaccounting.repository.DocumentSequenceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DocumentNumberService {

    private final DocumentNumberConfigRepository configRepository;
    private final DocumentSequenceRepository sequenceRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Generates the next unique document number for the given module.
     * Uses pessimistic write locking to ensure uniqueness and prevent duplicates.
     */
    @Transactional
    public String generateNextNumber(DocumentModule module) {
        String moduleName = module.name();

        // Find or create the config with pessimistic write lock
        DocumentNumberConfig config = findOrCreateConfig(moduleName);

        // Find or create the sequence
        DocumentSequence sequence = findOrCreateSequence(moduleName);

        // Handle yearly/monthly reset
        handleResetIfNeeded(config);

        // Increment and save the sequence
        Long nextValue = config.getLastNumber() + 1;
        config.setLastNumber(nextValue);
        config.setUpdatedAt(LocalDateTime.now());
        configRepository.save(config);

        sequence.setCurrentValue(nextValue);
        sequenceRepository.save(sequence);

        // Build the formatted number
        return buildFormattedNumber(config, nextValue);
    }

    /**
     * Retrieves the current numbering configuration for a module.
     */
    public DocumentNumberConfigDto getConfig(DocumentModule module) {
        String moduleName = module.name();
        DocumentNumberConfig config = configRepository
                .findByModuleAndCompanyIdAndBranchId(moduleName, 0L, 0L)
                .orElseGet(() -> createDefaultConfig(moduleName));

        return mapToDto(config);
    }

    /**
     * Creates or updates the numbering configuration for a module.
     */
    @Transactional
    public DocumentNumberConfigDto updateConfig(DocumentModule module, UpsertDocumentNumberConfigDto request) {
        String moduleName = module.name();

        DocumentNumberConfig config = configRepository
                .findByModuleAndCompanyIdAndBranchId(moduleName, 0L, 0L)
                .orElseGet(() -> {
                    DocumentNumberConfig newConfig = new DocumentNumberConfig();
                    newConfig.setModule(moduleName);
                    newConfig.setCompanyId(0L);
                    newConfig.setBranchId(0L);
                    newConfig.setLastNumber(0L);
                    newConfig.setCreatedAt(LocalDateTime.now());
                    return newConfig;
                });

        config.setPrefix(request.getPrefix());
        config.setPadding(request.getPadding() != null ? request.getPadding() : 5);
        config.setIncludeYear(request.getIncludeYear() != null ? request.getIncludeYear() : false);
        config.setIncludeMonth(request.getIncludeMonth() != null ? request.getIncludeMonth() : false);
        config.setResetYearly(request.getResetYearly() != null ? request.getResetYearly() : false);
        config.setResetMonthly(request.getResetMonthly() != null ? request.getResetMonthly() : false);
        config.setSeparator(request.getSeparator() != null ? request.getSeparator() : "-");
        config.setUpdatedAt(LocalDateTime.now());

        DocumentNumberConfig saved = configRepository.save(config);
        return mapToDto(saved);
    }

    /**
     * Finds existing config or creates a default one for the module.
     * Uses the global scope (companyId=0, branchId=0).
     */
    private DocumentNumberConfig findOrCreateConfig(String moduleName) {
        return configRepository
                .findByModuleAndCompanyIdAndBranchId(moduleName, 0L, 0L)
                .orElseGet(() -> createDefaultConfig(moduleName));
    }

    /**
     * Creates a default numbering configuration for a module.
     */
    private DocumentNumberConfig createDefaultConfig(String moduleName) {
        DocumentNumberConfig config = new DocumentNumberConfig();
        config.setModule(moduleName);
        config.setCompanyId(0L);
        config.setBranchId(0L);
        config.setPrefix(getDefaultPrefix(moduleName));
        config.setLastNumber(0L);
        config.setPadding(5);
        config.setIncludeYear(false);
        config.setIncludeMonth(false);
        config.setResetYearly(false);
        config.setResetMonthly(false);
        config.setSeparator("-");
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        return configRepository.save(config);
    }

    /**
     * Finds existing sequence or creates a new one for the module.
     */
    private DocumentSequence findOrCreateSequence(String moduleName) {
        String code = "DOC_SEQ_" + moduleName;
        Optional<DocumentSequence> existing = sequenceRepository.findByCode(code);
        if (existing.isPresent()) {
            return existing.get();
        }

        DocumentSequence sequence = new DocumentSequence();
        sequence.setCode(code);
        sequence.setPrefix(getDefaultPrefix(moduleName));
        sequence.setCurrentValue(0L);
        sequence.setPadding(5);
        return sequenceRepository.save(sequence);
    }

    /**
     * Handles yearly or monthly reset of the sequence number.
     */
    private void handleResetIfNeeded(DocumentNumberConfig config) {
        LocalDateTime now = LocalDateTime.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();

        boolean shouldReset = false;

        if (Boolean.TRUE.equals(config.getResetYearly())) {
            Integer lastResetYear = config.getLastResetYear();
            if (lastResetYear == null || lastResetYear != currentYear) {
                shouldReset = true;
            }
        }

        if (Boolean.TRUE.equals(config.getResetMonthly())) {
            Integer lastResetMonth = config.getLastResetMonth();
            if (lastResetMonth == null || lastResetMonth != currentMonth) {
                shouldReset = true;
            }
        }

        if (shouldReset) {
            config.setLastNumber(0L);
            config.setLastResetYear(currentYear);
            config.setLastResetMonth(currentMonth);
        }
    }

    /**
     * Builds the formatted document number string.
     * Pattern: {prefix}{separator}{year?}{month?}{separator}{padded_number}
     */
    private String buildFormattedNumber(DocumentNumberConfig config, Long number) {
        StringBuilder sb = new StringBuilder();

        // Prefix
        if (config.getPrefix() != null && !config.getPrefix().isEmpty()) {
            sb.append(config.getPrefix());
        }

        // Separator after prefix
        String sep = config.getSeparator() != null ? config.getSeparator() : "-";

        // Year
        if (Boolean.TRUE.equals(config.getIncludeYear())) {
            sb.append(sep);
            sb.append(String.format("%04d", YearMonth.now().getYear()));
        }

        // Month
        if (Boolean.TRUE.equals(config.getIncludeMonth())) {
            sb.append(sep);
            sb.append(String.format("%02d", YearMonth.now().getMonthValue()));
        }

        // Separator before number
        sb.append(sep);

        // Padded number
        int padding = config.getPadding() != null ? config.getPadding() : 5;
        sb.append(String.format("%0" + padding + "d", number));

        return sb.toString();
    }

    /**
     * Returns a default prefix for a given module name.
     */
    private String getDefaultPrefix(String moduleName) {
        return switch (moduleName) {
            case "INVOICE" -> "INV";
            case "JOURNAL" -> "JRN";
            case "ACCOUNT" -> "ACC";
            default -> moduleName.substring(0, Math.min(moduleName.length(), 3));
        };
    }

    /**
     * Maps entity to DTO.
     */
    private DocumentNumberConfigDto mapToDto(DocumentNumberConfig config) {
        return DocumentNumberConfigDto.builder()
                .id(config.getId())
                .module(config.getModule())
                .prefix(config.getPrefix())
                .lastNumber(config.getLastNumber())
                .padding(config.getPadding())
                .includeYear(config.getIncludeYear())
                .includeMonth(config.getIncludeMonth())
                .resetYearly(config.getResetYearly())
                .resetMonthly(config.getResetMonthly())
                .separator(config.getSeparator())
                .lastResetYear(config.getLastResetYear())
                .lastResetMonth(config.getLastResetMonth())
                .build();
    }
}

