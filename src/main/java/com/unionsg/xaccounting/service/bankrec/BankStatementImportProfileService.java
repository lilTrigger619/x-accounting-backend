package com.unionsg.xaccounting.service.bankrec;

import com.unionsg.xaccounting.dto.bankrec.ImportProfileRequest;
import com.unionsg.xaccounting.dto.bankrec.ImportProfileResponse;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.entity.bankrec.BankStatementImportProfile;
import com.unionsg.xaccounting.enums.bankrec.AmountSignConvention;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.bankrec.BankStatementImportProfileRepository;
import com.unionsg.xaccounting.repository.settings.BankAccountRepository;
import com.unionsg.xaccounting.security.util.SecurityUtils;
import com.unionsg.xaccounting.service.bankrec.engine.CsvColumnMapping;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Setup for saved CSV column mappings (create, enquiry, update, delete). */
@Service
@RequiredArgsConstructor
public class BankStatementImportProfileService {

    private final BankStatementImportProfileRepository repository;
    private final BankAccountRepository bankAccountRepository;

    @Transactional(readOnly = true)
    public List<ImportProfileResponse> list() {
        return repository.findByDeletedFalseOrderByNameAsc().stream().map(BankRecMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ImportProfileResponse get(Long id) {
        return BankRecMapper.toResponse(find(id));
    }

    @Transactional
    public ImportProfileResponse create(ImportProfileRequest request) {
        String name = requireName(request);
        if (repository.existsByNameIgnoreCaseAndDeletedFalse(name)) {
            throw new BusinessException("An import mapping named \"" + name + "\" already exists");
        }
        BankStatementImportProfile profile = new BankStatementImportProfile();
        apply(profile, request, name);
        return BankRecMapper.toResponse(repository.save(profile));
    }

    @Transactional
    public ImportProfileResponse update(Long id, ImportProfileRequest request) {
        BankStatementImportProfile profile = find(id);
        String name = requireName(request);
        if (repository.existsByNameIgnoreCaseAndDeletedFalseAndIdNot(name, id)) {
            throw new BusinessException("An import mapping named \"" + name + "\" already exists");
        }
        apply(profile, request, name);
        return BankRecMapper.toResponse(repository.save(profile));
    }

    /** Soft delete: imports already made keep a link to the mapping they used. */
    @Transactional
    public void delete(Long id) {
        BankStatementImportProfile profile = find(id);
        User user = SecurityUtils.getCurrentUser();
        profile.softDelete(user != null ? String.valueOf(user.getId()) : null);
        repository.save(profile);
    }

    public BankStatementImportProfile find(Long id) {
        return repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Import mapping not found with ID: " + id));
    }

    public static CsvColumnMapping toMapping(BankStatementImportProfile p) {
        return CsvColumnMapping.builder()
                .delimiter(p.getDelimiter())
                .hasHeaderRow(!Boolean.FALSE.equals(p.getHasHeaderRow()))
                .skipRows(p.getSkipRows() != null ? p.getSkipRows() : 0)
                .dateFormat(p.getDateFormat())
                .transactionDateColumn(p.getTransactionDateColumn())
                .valueDateColumn(p.getValueDateColumn())
                .descriptionColumn(p.getDescriptionColumn())
                .referenceColumn(p.getReferenceColumn())
                .debitColumn(p.getDebitColumn())
                .creditColumn(p.getCreditColumn())
                .amountColumn(p.getAmountColumn())
                .balanceColumn(p.getBalanceColumn())
                .externalIdColumn(p.getExternalIdColumn())
                .amountSignConvention(p.getAmountSignConvention())
                .build();
    }

    private void apply(BankStatementImportProfile p, ImportProfileRequest r, String name) {
        if (trim(r.getTransactionDateColumn()) == null) {
            throw new BusinessException("Map the transaction date column");
        }
        if (trim(r.getAmountColumn()) == null && trim(r.getDebitColumn()) == null && trim(r.getCreditColumn()) == null) {
            throw new BusinessException("Map either an amount column or debit/credit columns");
        }
        if (r.getSkipRows() != null && r.getSkipRows() < 0) {
            throw new BusinessException("Rows to skip cannot be negative");
        }
        if (trim(r.getDateFormat()) != null) {
            try {
                java.time.format.DateTimeFormatter.ofPattern(r.getDateFormat().trim());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("\"" + r.getDateFormat() + "\" is not a valid date format");
            }
        }
        p.setName(name);
        p.setDescription(trim(r.getDescription()));
        p.setBankAccount(r.getBankAccountId() == null ? null : bankAccountRepository.findById(r.getBankAccountId())
                .filter(a -> !Boolean.TRUE.equals(a.getDeleted()))
                .orElseThrow(() -> new BusinessException("Bank account not found with ID: " + r.getBankAccountId())));
        p.setDelimiter(r.getDelimiter() == null || r.getDelimiter().isEmpty() ? "," : r.getDelimiter());
        p.setHasHeaderRow(r.getHasHeaderRow() == null || r.getHasHeaderRow());
        p.setSkipRows(r.getSkipRows() != null ? r.getSkipRows() : 0);
        p.setDateFormat(trim(r.getDateFormat()));
        p.setTransactionDateColumn(trim(r.getTransactionDateColumn()));
        p.setValueDateColumn(trim(r.getValueDateColumn()));
        p.setDescriptionColumn(trim(r.getDescriptionColumn()));
        p.setReferenceColumn(trim(r.getReferenceColumn()));
        p.setDebitColumn(trim(r.getDebitColumn()));
        p.setCreditColumn(trim(r.getCreditColumn()));
        p.setAmountColumn(trim(r.getAmountColumn()));
        p.setBalanceColumn(trim(r.getBalanceColumn()));
        p.setExternalIdColumn(trim(r.getExternalIdColumn()));
        p.setAmountSignConvention(r.getAmountSignConvention() != null ? r.getAmountSignConvention() : AmountSignConvention.POSITIVE_IS_CREDIT);
        p.setActive(r.getActive() == null || r.getActive());
    }

    private static String requireName(ImportProfileRequest request) {
        String name = trim(request.getName());
        if (name == null) {
            throw new BusinessException("Mapping name is required");
        }
        return name;
    }

    private static String trim(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }
}
