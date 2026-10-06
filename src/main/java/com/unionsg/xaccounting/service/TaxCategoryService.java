package com.unionsg.xaccounting.service;

import com.unionsg.xaccounting.dto.TaxCategoryDTO;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.TaxCategory;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.TaxCategoryRepository;
import com.unionsg.xaccounting.repository.product.ProductRepository;
import com.unionsg.xaccounting.service.config.ConfigValueValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaxCategoryService {

    private final TaxCategoryRepository taxCategoryRepository;
    private final ProductRepository productRepository;
    private final AccountRepository accountRepository;
    private final ConfigValueValidator configValues;

    @Transactional
    public TaxCategoryDTO create(TaxCategoryDTO dto) {
        TaxCategory entity = TaxCategory.builder().build();
        apply(entity, dto);
        entity.setActive(dto.getActive() == null || dto.getActive());
        return toDto(save(entity));
    }

    @Transactional(readOnly = true)
    public List<TaxCategoryDTO> getAll(boolean activeOnly) {
        return taxCategoryRepository.findByDeletedFalse().stream()
                .filter(t -> !activeOnly || t.isActiveRate())
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaxCategoryDTO getById(Long id) {
        return toDto(getEntity(id));
    }

    @Transactional
    public TaxCategoryDTO update(Long id, TaxCategoryDTO dto) {
        TaxCategory entity = getEntity(id);
        apply(entity, dto);
        if (dto.getActive() != null) entity.setActive(dto.getActive());
        return toDto(save(entity));
    }

    @Transactional
    public TaxCategoryDTO setActive(Long id, boolean active) {
        TaxCategory entity = getEntity(id);
        entity.setActive(active);
        if (!active) entity.setDefaultForNewItems(false);
        return toDto(taxCategoryRepository.save(entity));
    }

    /** Soft-deletes a tax rate no product uses; used rates can only be deactivated. */
    @Transactional
    public void delete(Long id) {
        TaxCategory entity = getEntity(id);
        if (productRepository.existsByTaxCategoryIdAndDeletedFalse(id)) {
            throw new BusinessException("\"" + entity.getName() + "\" is used by products. Deactivate it instead.");
        }
        entity.setDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        entity.setActive(false);
        entity.setDefaultForNewItems(false);
        taxCategoryRepository.save(entity);
    }

    /**
     * Loads a tax rate for a product. A product may keep the rate it already has even after the
     * rate is deactivated, but cannot newly pick an inactive one.
     */
    @Transactional(readOnly = true)
    public TaxCategory getUsable(Long id, Long currentId) {
        if (id == null) return null;
        TaxCategory entity = getEntity(id);
        if (!entity.isActiveRate() && !id.equals(currentId)) {
            throw new BusinessException("Tax rate \"" + entity.getName() + "\" is inactive");
        }
        return entity;
    }

    private TaxCategory save(TaxCategory entity) {
        TaxCategory saved = taxCategoryRepository.save(entity);
        if (Boolean.TRUE.equals(saved.getDefaultForNewItems())) {
            taxCategoryRepository.findByDeletedFalseAndDefaultForNewItemsTrue().stream()
                    .filter(t -> !t.getId().equals(saved.getId()))
                    .forEach(t -> {
                        t.setDefaultForNewItems(false);
                        taxCategoryRepository.save(t);
                    });
        }
        return saved;
    }

    private void apply(TaxCategory entity, TaxCategoryDTO dto) {
        Long id = entity.getId();

        String name = required(dto.getName(), "Tax name", 100);
        boolean nameTaken = id == null
                ? taxCategoryRepository.existsByNameIgnoreCaseAndDeletedFalse(name)
                : taxCategoryRepository.existsByNameIgnoreCaseAndDeletedFalseAndIdNot(name, id);
        if (nameTaken) throw new BusinessException("A tax rate named \"" + name + "\" already exists");

        String code = required(dto.getCode(), "Tax code", 30);
        boolean codeTaken = id == null
                ? taxCategoryRepository.existsByCodeIgnoreCaseAndDeletedFalse(code)
                : taxCategoryRepository.existsByCodeIgnoreCaseAndDeletedFalseAndIdNot(code, id);
        if (codeTaken) throw new BusinessException("A tax rate with code \"" + code + "\" already exists");

        if (dto.getType() == null) throw new BusinessException("Tax type is required");
        BigDecimal rate = dto.getRate();
        if (rate == null) throw new BusinessException("Tax rate is required");
        if (rate.signum() < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException("Tax rate must be between 0 and 100");
        }
        if (dto.getEffectiveFrom() == null) throw new BusinessException("Effective from date is required");
        if (dto.getEffectiveTo() != null && dto.getEffectiveTo().isBefore(dto.getEffectiveFrom())) {
            throw new BusinessException("Effective to date cannot be before the effective from date");
        }

        entity.setName(name);
        entity.setCode(code);
        entity.setType(dto.getType());
        entity.setRate(rate);
        entity.setDescription(optional(dto.getDescription(), "Description", 500));
        entity.setAuthority(configValues.require("tax-authorities", dto.getAuthority(), entity.getAuthority(), "Tax authority"));
        entity.setRegistrationNumber(optional(dto.getRegistrationNumber(), "Registration number", 50));
        entity.setFilingFrequency(configValues.validate("filing-frequencies", dto.getFilingFrequency(), entity.getFilingFrequency(), "Filing frequency"));
        entity.setReportingCode(optional(dto.getReportingCode(), "Reporting code", 50));
        entity.setAppliesTo(configValues.require("tax-applies-to", dto.getAppliesTo(), entity.getAppliesTo(), "Applies to"));
        entity.setTransactionType(configValues.validate("tax-transaction-types", dto.getTransactionType(), entity.getTransactionType(), "Transaction type"));
        entity.setTaxGroup(configValues.validate("tax-groups", dto.getTaxGroup(), entity.getTaxGroup(), "Tax group"));
        entity.setLinkedAccount(resolveAccount(dto.getLinkedAccountId()));
        entity.setEffectiveFrom(dto.getEffectiveFrom());
        entity.setEffectiveTo(dto.getEffectiveTo());
        entity.setCompound(Boolean.TRUE.equals(dto.getCompound()));
        entity.setRecoverable(Boolean.TRUE.equals(dto.getRecoverable()));
        entity.setDefaultForNewItems(Boolean.TRUE.equals(dto.getDefaultForNewItems()));
    }

    private AccountEntity resolveAccount(Long accountId) {
        if (accountId == null) return null;
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException("Linked account not found"));
    }

    private TaxCategory getEntity(Long id) {
        return taxCategoryRepository.findById(id)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Tax rate not found with ID: " + id));
    }

    private static String required(String value, String label, int max) {
        if (value == null || value.isBlank()) throw new BusinessException(label + " is required");
        return optional(value, label, max);
    }

    private static String optional(String value, String label, int max) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim();
        if (v.length() > max) throw new BusinessException(label + " can be at most " + max + " characters");
        return v;
    }

    private TaxCategoryDTO toDto(TaxCategory entity) {
        AccountEntity account = entity.getLinkedAccount();
        return TaxCategoryDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .type(entity.getType())
                .rate(entity.getRate())
                .description(entity.getDescription())
                .authority(entity.getAuthority())
                .registrationNumber(entity.getRegistrationNumber())
                .filingFrequency(entity.getFilingFrequency())
                .reportingCode(entity.getReportingCode())
                .appliesTo(entity.getAppliesTo())
                .transactionType(entity.getTransactionType())
                .linkedAccountId(account == null ? null : account.getId())
                .linkedAccountCode(account == null ? null : account.getAccountId())
                .linkedAccountName(account == null ? null : account.getAccountName())
                .taxGroup(entity.getTaxGroup())
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(entity.getEffectiveTo())
                .active(entity.isActiveRate())
                .compound(Boolean.TRUE.equals(entity.getCompound()))
                .recoverable(Boolean.TRUE.equals(entity.getRecoverable()))
                .defaultForNewItems(Boolean.TRUE.equals(entity.getDefaultForNewItems()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
