package com.unionsg.xaccounting.service.bankrec;

import com.unionsg.xaccounting.dto.bankrec.ImportRowResponse;
import com.unionsg.xaccounting.dto.bankrec.StatementImportOptions;
import com.unionsg.xaccounting.dto.bankrec.StatementImportResponse;
import com.unionsg.xaccounting.dto.bankrec.StatementTransactionResponse;
import com.unionsg.xaccounting.entity.bankrec.BankReconciliation;
import com.unionsg.xaccounting.entity.bankrec.BankStatementImport;
import com.unionsg.xaccounting.entity.bankrec.BankStatementImportProfile;
import com.unionsg.xaccounting.entity.bankrec.BankStatementTransaction;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.enums.bankrec.ReconciliationAuditAction;
import com.unionsg.xaccounting.enums.bankrec.ReconciliationStatus;
import com.unionsg.xaccounting.enums.bankrec.StatementTransactionStatus;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.bankrec.BankReconciliationAdjustmentRepository;
import com.unionsg.xaccounting.repository.bankrec.BankReconciliationMatchItemRepository;
import com.unionsg.xaccounting.repository.bankrec.BankReconciliationRepository;
import com.unionsg.xaccounting.repository.bankrec.BankStatementImportRepository;
import com.unionsg.xaccounting.repository.bankrec.BankStatementTransactionRepository;
import com.unionsg.xaccounting.repository.settings.BankAccountRepository;
import com.unionsg.xaccounting.service.bankrec.engine.CsvColumnMapping;
import com.unionsg.xaccounting.service.bankrec.engine.ParsedStatementRow;
import com.unionsg.xaccounting.service.bankrec.engine.StatementCsvParser;
import com.unionsg.xaccounting.service.bankrec.engine.StatementDedupe;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Imports bank statement CSV files into {@link BankStatementTransaction}s - a store kept apart
 * from the ledger - skipping lines already imported (see {@link StatementDedupe}).
 */
@Service
@RequiredArgsConstructor
public class BankStatementImportService {

    static final int MAX_ROWS = 20_000;
    private static final int KEY_LOOKUP_CHUNK = 500;

    private final BankAccountRepository bankAccountRepository;
    private final BankReconciliationRepository reconciliationRepository;
    private final BankStatementImportRepository importRepository;
    private final BankStatementTransactionRepository transactionRepository;
    private final BankReconciliationMatchItemRepository matchItemRepository;
    private final BankReconciliationAdjustmentRepository adjustmentRepository;
    private final BankStatementImportProfileService profileService;
    private final BankRecAuditService auditService;

    @Transactional
    public StatementImportResponse importStatement(String content, String fileName, StatementImportOptions options) {
        if (options == null || options.getBankAccountId() == null) {
            throw new BusinessException("Choose the bank account this statement belongs to");
        }
        BankAccount bankAccount = bankAccountRepository.findById(options.getBankAccountId())
                .filter(a -> !Boolean.TRUE.equals(a.getDeleted()))
                .orElseThrow(() -> new BusinessException("Bank account not found with ID: " + options.getBankAccountId()));
        if (Boolean.FALSE.equals(bankAccount.getEnableReconciliation())) {
            throw new BusinessException("Reconciliation is turned off for " + bankAccount.getAccountName());
        }

        BankReconciliation reconciliation = null;
        if (options.getReconciliationId() != null) {
            reconciliation = reconciliationRepository.findByIdAndDeletedFalse(options.getReconciliationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reconciliation not found with ID: " + options.getReconciliationId()));
            if (!reconciliation.getBankAccount().getId().equals(bankAccount.getId())) {
                throw new BusinessException("The reconciliation belongs to a different bank account");
            }
            if (!reconciliation.getStatus().isOpen()) {
                throw new BusinessException("Statements can only be imported into an open reconciliation");
            }
        }

        BankStatementImportProfile profile = null;
        if (options.getProfileId() != null) {
            profile = profileService.find(options.getProfileId());
            if (Boolean.FALSE.equals(profile.getActive())) {
                throw new BusinessException("Import mapping \"" + profile.getName() + "\" is inactive");
            }
        }
        CsvColumnMapping mapping = resolveMapping(profile, options);

        List<ParsedStatementRow> rows = StatementCsvParser.parse(content, mapping);
        if (rows.size() > MAX_ROWS) {
            throw new BusinessException("A statement file can hold at most " + MAX_ROWS + " rows; split the file");
        }
        List<String> keys = StatementDedupe.keys(rows);
        Set<String> existing = existingKeys(bankAccount.getId(), keys);

        boolean dryRun = Boolean.TRUE.equals(options.getDryRun());
        List<ImportRowResponse> results = new ArrayList<>();
        List<BankStatementTransaction> toSave = new ArrayList<>();
        Set<String> seenInFile = new HashSet<>();
        int duplicates = 0;
        int errors = 0;

        for (int i = 0; i < rows.size(); i++) {
            ParsedStatementRow row = rows.get(i);
            String key = keys.get(i);
            String outcome;
            String message = null;
            if (!row.isValid()) {
                outcome = "ERROR";
                message = row.error();
                errors++;
            } else if (existing.contains(key)) {
                outcome = "DUPLICATE";
                message = "Already imported";
                duplicates++;
            } else if (!seenInFile.add(key)) {
                outcome = "DUPLICATE";
                message = "Repeated in this file";
                duplicates++;
            } else {
                outcome = "NEW";
                toSave.add(toTransaction(row, key, bankAccount));
            }
            results.add(toRow(row, outcome, message));
        }

        StatementImportResponse.StatementImportResponseBuilder response = StatementImportResponse.builder()
                .bankAccountId(bankAccount.getId())
                .bankAccountName(bankAccount.getAccountName())
                .reconciliationId(reconciliation != null ? reconciliation.getId() : null)
                .reconciliationNumber(reconciliation != null ? reconciliation.getReconciliationNumber() : null)
                .profileId(profile != null ? profile.getId() : null)
                .profileName(profile != null ? profile.getName() : null)
                .fileName(fileName)
                .totalRows(rows.size())
                .importedRows(toSave.size())
                .duplicateRows(duplicates)
                .errorRows(errors)
                .firstTransactionDate(toSave.stream().map(BankStatementTransaction::getTransactionDate).min(Comparator.naturalOrder()).orElse(null))
                .lastTransactionDate(toSave.stream().map(BankStatementTransaction::getTransactionDate).max(Comparator.naturalOrder()).orElse(null))
                .dryRun(dryRun)
                .rows(results);
        if (dryRun) {
            return response.build();
        }

        BankStatementImport batch = new BankStatementImport();
        batch.setBankAccount(bankAccount);
        batch.setReconciliation(reconciliation);
        batch.setProfile(profile);
        batch.setFileName(fileName);
        batch.setTotalRows(rows.size());
        batch.setImportedRows(toSave.size());
        batch.setDuplicateRows(duplicates);
        batch.setErrorRows(errors);
        batch.setFirstTransactionDate(response.build().getFirstTransactionDate());
        batch.setLastTransactionDate(response.build().getLastTransactionDate());
        batch.setImportedByName(BankRecAuditService.currentUserName());
        BankStatementImport saved = importRepository.save(batch);
        toSave.forEach(t -> t.setStatementImport(saved));
        transactionRepository.saveAll(toSave);

        if (reconciliation != null && reconciliation.getStatus() == ReconciliationStatus.DRAFT) {
            reconciliation.setStatus(ReconciliationStatus.IN_PROGRESS);
            reconciliationRepository.save(reconciliation);
        }
        auditService.record(reconciliation != null ? reconciliation.getId() : null, bankAccount.getId(),
                ReconciliationAuditAction.STATEMENT_IMPORTED,
                String.format(Locale.ROOT, "Imported %s: %d new, %d duplicate, %d with errors (%d rows)",
                        fileName != null ? fileName : "statement", toSave.size(), duplicates, errors, rows.size()));

        return response.id(saved.getId()).importedByName(saved.getImportedByName()).importedAt(saved.getCreatedAt()).build();
    }

    @Transactional(readOnly = true)
    public List<StatementImportResponse> listImports(Long bankAccountId) {
        List<BankStatementImport> imports = bankAccountId != null
                ? importRepository.findByBankAccountIdAndDeletedFalseOrderByIdDesc(bankAccountId)
                : importRepository.findByDeletedFalseOrderByIdDesc();
        return imports.stream().map(BankRecMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<StatementTransactionResponse> importTransactions(Long importId) {
        findImport(importId);
        return transactionRepository.findByStatementImportId(importId).stream()
                .sorted(Comparator.comparing(BankStatementTransaction::getTransactionDate).thenComparing(BankStatementTransaction::getId))
                .map(t -> BankRecMapper.toResponse(t, null)).toList();
    }

    /**
     * Statement lines for a bank account, newest first. {@code status} filters on the overall
     * match status; {@code search} looks in description, reference and transaction ID.
     */
    @Transactional(readOnly = true)
    public List<StatementTransactionResponse> listTransactions(Long bankAccountId, LocalDate from, LocalDate to,
                                                               StatementTransactionStatus status, String search) {
        if (bankAccountId == null) {
            throw new BusinessException("Choose a bank account");
        }
        LocalDate start = from != null ? from : LocalDate.of(1900, 1, 1);
        LocalDate end = to != null ? to : LocalDate.of(9999, 12, 31);
        String needle = search == null || search.isBlank() ? null : search.trim().toLowerCase(Locale.ROOT);
        return transactionRepository.findInRange(bankAccountId, start, end).stream()
                .filter(t -> status == null || t.getStatus() == status)
                .filter(t -> needle == null || contains(t.getDescription(), needle) || contains(t.getReference(), needle)
                        || contains(t.getExternalTransactionId(), needle))
                .map(t -> BankRecMapper.toResponse(t, null))
                .toList();
    }

    /**
     * Removes an import and its lines, e.g. after picking the wrong account or mapping. Refused
     * once any of its lines has been matched or adjusted, since that history must stay.
     */
    @Transactional
    public void deleteImport(Long importId) {
        BankStatementImport batch = findImport(importId);
        if (!matchItemRepository.findAllForImport(importId).isEmpty()
                || adjustmentRepository.existsByStatementTransactionStatementImportId(importId)) {
            throw new BusinessException("This statement has lines that were matched or adjusted. Unmatch them first; "
                    + "lines with match history cannot be deleted");
        }
        List<BankStatementTransaction> lines = transactionRepository.findByStatementImportId(importId);
        transactionRepository.deleteAll(lines);
        batch.softDelete(String.valueOf(BankRecAuditService.currentUserId()));
        importRepository.save(batch);
        auditService.record(batch.getReconciliation() != null ? batch.getReconciliation().getId() : null,
                batch.getBankAccount().getId(), ReconciliationAuditAction.STATEMENT_IMPORT_DELETED,
                "Deleted statement import " + (batch.getFileName() != null ? batch.getFileName() : "#" + batch.getId())
                        + " and its " + lines.size() + " lines");
    }

    static CsvColumnMapping resolveMapping(BankStatementImportProfile profile, StatementImportOptions o) {
        CsvColumnMapping.CsvColumnMappingBuilder b = profile != null
                ? BankStatementImportProfileService.toMapping(profile).toBuilder()
                : CsvColumnMapping.builder().delimiter(",").hasHeaderRow(true).skipRows(0);
        if (o.getDelimiter() != null && !o.getDelimiter().isEmpty()) b.delimiter(o.getDelimiter());
        if (o.getHasHeaderRow() != null) b.hasHeaderRow(o.getHasHeaderRow());
        if (o.getSkipRows() != null) b.skipRows(o.getSkipRows());
        if (notBlank(o.getDateFormat())) b.dateFormat(o.getDateFormat().trim());
        if (notBlank(o.getTransactionDateColumn())) b.transactionDateColumn(o.getTransactionDateColumn());
        if (notBlank(o.getValueDateColumn())) b.valueDateColumn(o.getValueDateColumn());
        if (notBlank(o.getDescriptionColumn())) b.descriptionColumn(o.getDescriptionColumn());
        if (notBlank(o.getReferenceColumn())) b.referenceColumn(o.getReferenceColumn());
        if (notBlank(o.getDebitColumn())) b.debitColumn(o.getDebitColumn());
        if (notBlank(o.getCreditColumn())) b.creditColumn(o.getCreditColumn());
        if (notBlank(o.getAmountColumn())) b.amountColumn(o.getAmountColumn());
        if (notBlank(o.getBalanceColumn())) b.balanceColumn(o.getBalanceColumn());
        if (notBlank(o.getExternalIdColumn())) b.externalIdColumn(o.getExternalIdColumn());
        if (o.getAmountSignConvention() != null) b.amountSignConvention(o.getAmountSignConvention());
        return b.build();
    }

    private Set<String> existingKeys(Long bankAccountId, List<String> keys) {
        List<String> lookup = keys.stream().filter(Objects::nonNull).distinct().toList();
        Set<String> existing = new HashSet<>();
        for (int i = 0; i < lookup.size(); i += KEY_LOOKUP_CHUNK) {
            existing.addAll(transactionRepository.findExistingDedupeKeys(bankAccountId,
                    lookup.subList(i, Math.min(lookup.size(), i + KEY_LOOKUP_CHUNK))));
        }
        return existing;
    }

    private static BankStatementTransaction toTransaction(ParsedStatementRow row, String key, BankAccount bankAccount) {
        BankStatementTransaction t = new BankStatementTransaction();
        t.setBankAccount(bankAccount);
        t.setSourceRowNumber(row.rowNumber());
        t.setTransactionDate(row.transactionDate());
        t.setValueDate(row.valueDate());
        t.setDescription(truncate(row.description(), 500));
        t.setReference(truncate(row.reference(), 150));
        t.setDebitAmount(row.debit());
        t.setCreditAmount(row.credit());
        t.setAmount(row.amount());
        t.setRunningBalance(row.balance());
        t.setExternalTransactionId(truncate(row.externalId(), 150));
        t.setDedupeKey(key);
        t.setMatchedAmount(BigDecimal.ZERO);
        t.setStatus(StatementTransactionStatus.UNMATCHED);
        return t;
    }

    private static ImportRowResponse toRow(ParsedStatementRow row, String outcome, String message) {
        return ImportRowResponse.builder()
                .rowNumber(row.rowNumber())
                .outcome(outcome)
                .message(message)
                .transactionDate(row.transactionDate())
                .valueDate(row.valueDate())
                .description(row.description())
                .reference(row.reference())
                .debit(row.debit())
                .credit(row.credit())
                .amount(row.amount())
                .balance(row.balance())
                .externalTransactionId(row.externalId())
                .build();
    }

    private BankStatementImport findImport(Long id) {
        return importRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Statement import not found with ID: " + id));
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
