package com.unionsg.xaccounting.service.bankrec;

import com.unionsg.xaccounting.dto.bankrec.AdjustmentResponse;
import com.unionsg.xaccounting.dto.bankrec.AuditLogResponse;
import com.unionsg.xaccounting.dto.bankrec.AutoMatchRequest;
import com.unionsg.xaccounting.dto.bankrec.AutoMatchResultResponse;
import com.unionsg.xaccounting.dto.bankrec.BookTransactionResponse;
import com.unionsg.xaccounting.dto.bankrec.CompleteReconciliationRequest;
import com.unionsg.xaccounting.dto.bankrec.CreateAdjustmentRequest;
import com.unionsg.xaccounting.dto.bankrec.DashboardAccountResponse;
import com.unionsg.xaccounting.dto.bankrec.ManualMatchRequest;
import com.unionsg.xaccounting.dto.bankrec.MatchActionRequest;
import com.unionsg.xaccounting.dto.bankrec.MatchResponse;
import com.unionsg.xaccounting.dto.bankrec.ReconciliationReportResponse;
import com.unionsg.xaccounting.dto.bankrec.ReconciliationResponse;
import com.unionsg.xaccounting.dto.bankrec.ReconciliationSummaryResponse;
import com.unionsg.xaccounting.dto.bankrec.SaveReconciliationRequest;
import com.unionsg.xaccounting.dto.bankrec.StatementTransactionResponse;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.entity.bankrec.BankReconciliation;
import com.unionsg.xaccounting.entity.bankrec.BankReconciliationAdjustment;
import com.unionsg.xaccounting.entity.bankrec.BankReconciliationMatch;
import com.unionsg.xaccounting.entity.bankrec.BankReconciliationMatchItem;
import com.unionsg.xaccounting.entity.bankrec.BankStatementImport;
import com.unionsg.xaccounting.entity.bankrec.BankStatementTransaction;
import com.unionsg.xaccounting.entity.payment.PaymentEntity;
import com.unionsg.xaccounting.entity.payment.SupplierPaymentEntity;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.enums.DocumentModule;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.enums.bankrec.AdjustmentStatus;
import com.unionsg.xaccounting.enums.bankrec.BookTransactionStatus;
import com.unionsg.xaccounting.enums.bankrec.MatchItemSide;
import com.unionsg.xaccounting.enums.bankrec.MatchStatus;
import com.unionsg.xaccounting.enums.bankrec.MatchType;
import com.unionsg.xaccounting.enums.bankrec.ReconciliationAdjustmentType;
import com.unionsg.xaccounting.enums.bankrec.ReconciliationAuditAction;
import com.unionsg.xaccounting.enums.bankrec.ReconciliationStatus;
import com.unionsg.xaccounting.enums.bankrec.StatementTransactionStatus;
import com.unionsg.xaccounting.enums.settings.BankAccountStatus;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.bankrec.BankLedgerRepository;
import com.unionsg.xaccounting.repository.bankrec.BankReconciliationAdjustmentRepository;
import com.unionsg.xaccounting.repository.bankrec.BankReconciliationAuditLogRepository;
import com.unionsg.xaccounting.repository.bankrec.BankReconciliationMatchItemRepository;
import com.unionsg.xaccounting.repository.bankrec.BankReconciliationMatchRepository;
import com.unionsg.xaccounting.repository.bankrec.BankReconciliationRepository;
import com.unionsg.xaccounting.repository.bankrec.BankStatementImportRepository;
import com.unionsg.xaccounting.repository.bankrec.BankStatementTransactionRepository;
import com.unionsg.xaccounting.repository.payment.PaymentRepository;
import com.unionsg.xaccounting.repository.payment.SupplierPaymentRepository;
import com.unionsg.xaccounting.repository.settings.BankAccountRepository;
import com.unionsg.xaccounting.security.service.PermissionService;
import com.unionsg.xaccounting.service.DocumentNumberService;
import com.unionsg.xaccounting.service.bankrec.engine.AllocationItem;
import com.unionsg.xaccounting.service.bankrec.engine.MatchAllocator;
import com.unionsg.xaccounting.service.bankrec.engine.MatchProposal;
import com.unionsg.xaccounting.service.bankrec.engine.MatchingCandidate;
import com.unionsg.xaccounting.service.bankrec.engine.MatchingCriteria;
import com.unionsg.xaccounting.service.bankrec.engine.MatchingEngine;
import com.unionsg.xaccounting.service.bankrec.engine.ReconciliationCalculator;
import com.unionsg.xaccounting.service.bankrec.engine.ReconciliationFigures;
import com.unionsg.xaccounting.service.settings.AccountingMappingService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The bank reconciliation engine: reconciliation lifecycle, the bank-statement and book sides of
 * the workspace, automatic and manual (including partial) matching, adjustments, completion with
 * tolerance/override, reopening, reports and the audit trail.
 *
 * <p>Book transactions are the posted journal lines on the bank account's GL account. They are
 * only ever linked to, never copied or re-posted; the only journals this module creates are
 * adjustments for bank-only items.</p>
 */
@Service
@RequiredArgsConstructor
public class BankReconciliationService {

    public static final String OVERRIDE_PERMISSION = "override_reconciliation_difference";

    /** Journal statuses that make up the ledger: a reversed journal still happened, its reversal offsets it. */
    static final Set<JournalStatus> LEDGER_STATUSES = EnumSet.of(JournalStatus.POSTED, JournalStatus.REVERSED);
    static final Set<MatchStatus> ACTIVE = EnumSet.of(MatchStatus.PROPOSED, MatchStatus.CONFIRMED);

    private final BankReconciliationRepository repository;
    private final BankAccountRepository bankAccountRepository;
    private final BankStatementTransactionRepository statementRepository;
    private final BankStatementImportRepository importRepository;
    private final BankLedgerRepository ledgerRepository;
    private final BankReconciliationMatchRepository matchRepository;
    private final BankReconciliationMatchItemRepository matchItemRepository;
    private final BankReconciliationAdjustmentRepository adjustmentRepository;
    private final BankReconciliationAuditLogRepository auditLogRepository;
    private final AccountRepository accountRepository;
    private final PaymentRepository paymentRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final DocumentNumberService documentNumberService;
    private final AccountingMappingService accountingMappingService;
    private final PermissionService permissionService;
    private final BankMatchingRuleService ruleService;
    private final BankReconciliationJournalService journalService;
    private final BankRecAuditService auditService;

    // =====================================================================================
    // Lifecycle
    // =====================================================================================

    @Transactional
    public ReconciliationResponse create(SaveReconciliationRequest request) {
        BankAccount account = requireBankAccount(request.getBankAccountId());
        validateHeader(request);
        List<BankReconciliation> open = repository.findByBankAccountIdAndStatusInAndDeletedFalse(account.getId(), openStatuses());
        if (!open.isEmpty()) {
            throw new BusinessException("Reconciliation " + open.get(0).getReconciliationNumber() + " is still open for "
                    + account.getAccountName() + ". Complete or cancel it first");
        }
        BankReconciliation last = lastReconciled(account.getId(), null);
        if (last != null && !request.getPeriodStart().isAfter(last.getPeriodEnd())) {
            throw new BusinessException("The period must start after " + last.getPeriodEnd()
                    + ", the end of the last completed reconciliation (" + last.getReconciliationNumber() + ")");
        }

        BankReconciliation r = new BankReconciliation();
        r.setBankAccount(account);
        r.setCurrency(account.getCurrency());
        applyHeader(r, request);
        if (request.getOpeningBankBalance() == null) {
            r.setOpeningBankBalance(last != null ? last.getClosingBankBalance() : BigDecimal.ZERO);
        }
        r.setStatus(ReconciliationStatus.DRAFT);
        r.setReconciliationNumber(documentNumberService.generateNextNumber(DocumentModule.BANK_RECONCILIATION));
        r.setPreparedById(BankRecAuditService.currentUserId());
        r.setPreparedByName(BankRecAuditService.currentUserName());
        r.setPreparedAt(LocalDateTime.now());
        BankReconciliation saved = repository.save(r);
        recalculate(saved);
        repository.save(saved);

        auditService.record(saved.getId(), account.getId(), ReconciliationAuditAction.CREATED,
                "Started " + saved.getReconciliationNumber() + " for " + account.getAccountName()
                        + " (" + saved.getPeriodStart() + " to " + saved.getPeriodEnd() + ")");
        return BankRecMapper.toResponse(saved);
    }

    @Transactional
    public ReconciliationResponse update(Long id, SaveReconciliationRequest request) {
        BankReconciliation r = find(id);
        requireOpen(r);
        validateHeader(request);
        boolean hasWork = matchRepository.existsByReconciliationIdAndStatusIn(id, EnumSet.allOf(MatchStatus.class))
                || adjustmentRepository.existsByReconciliationId(id);
        if (request.getBankAccountId() != null && !request.getBankAccountId().equals(r.getBankAccount().getId())) {
            if (hasWork) {
                throw new BusinessException("The bank account cannot change once transactions have been matched or adjusted");
            }
            BankAccount account = requireBankAccount(request.getBankAccountId());
            if (!repository.findByBankAccountIdAndStatusInAndDeletedFalse(account.getId(), openStatuses()).isEmpty()) {
                throw new BusinessException(account.getAccountName() + " already has an open reconciliation");
            }
            r.setBankAccount(account);
            r.setCurrency(account.getCurrency());
        }
        if (hasWork && request.getPeriodEnd().isBefore(r.getPeriodEnd())) {
            throw new BusinessException("The period end cannot move earlier once transactions have been matched; unmatch them first");
        }
        BankReconciliation last = lastReconciled(r.getBankAccount().getId(), r.getId());
        if (last != null && !request.getPeriodStart().isAfter(last.getPeriodEnd())) {
            throw new BusinessException("The period must start after " + last.getPeriodEnd()
                    + ", the end of the last completed reconciliation");
        }
        applyHeader(r, request);
        if (request.getOpeningBankBalance() != null) {
            r.setOpeningBankBalance(request.getOpeningBankBalance());
        }
        recalculate(r);
        BankReconciliation saved = repository.save(r);
        auditService.record(id, r.getBankAccount().getId(), ReconciliationAuditAction.UPDATED,
                "Updated statement details: closing balance " + r.getClosingBankBalance().toPlainString()
                        + ", period " + r.getPeriodStart() + " to " + r.getPeriodEnd()
                        + ", tolerance " + r.getTolerance().toPlainString());
        return BankRecMapper.toResponse(saved);
    }

    /** Only a draft with no matches or adjustments can be deleted; anything further is cancelled instead. */
    @Transactional
    public void delete(Long id) {
        BankReconciliation r = find(id);
        if (r.getStatus() != ReconciliationStatus.DRAFT) {
            throw new BusinessException("Only a draft reconciliation can be deleted. Cancel it instead");
        }
        if (matchRepository.existsByReconciliationIdAndStatusIn(id, EnumSet.allOf(MatchStatus.class))
                || adjustmentRepository.existsByReconciliationId(id)) {
            throw new BusinessException("This reconciliation has matching history or adjustments. Cancel it instead");
        }
        for (BankStatementImport batch : importRepository.findByBankAccountIdAndDeletedFalseOrderByIdDesc(r.getBankAccount().getId())) {
            if (batch.getReconciliation() != null && batch.getReconciliation().getId().equals(id)) {
                batch.setReconciliation(null);
                importRepository.save(batch);
            }
        }
        r.softDelete(String.valueOf(BankRecAuditService.currentUserId()));
        repository.save(r);
        auditService.record(id, r.getBankAccount().getId(), ReconciliationAuditAction.DELETED,
                "Deleted draft " + r.getReconciliationNumber());
    }

    @Transactional(readOnly = true)
    public ReconciliationResponse get(Long id) {
        return BankRecMapper.toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public Page<ReconciliationResponse> list(Long bankAccountId, ReconciliationStatus status, String search,
                                             LocalDate from, LocalDate to, Pageable pageable) {
        Specification<BankReconciliation> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.isFalse(root.get("deleted")));
            if (bankAccountId != null) p.add(cb.equal(root.get("bankAccount").get("id"), bankAccountId));
            if (status != null) p.add(cb.equal(root.get("status"), status));
            if (from != null) p.add(cb.greaterThanOrEqualTo(root.get("periodEnd"), from));
            if (to != null) p.add(cb.lessThanOrEqualTo(root.get("periodEnd"), to));
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                p.add(cb.or(
                        cb.like(cb.lower(root.get("reconciliationNumber")), like),
                        cb.like(cb.lower(root.get("bankAccount").get("accountName")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("notes"), "")), like)));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        return repository.findAll(spec, pageable).map(BankRecMapper::toResponse);
    }

    @Transactional
    public ReconciliationResponse review(Long id, String notes) {
        BankReconciliation r = find(id);
        requireOpen(r);
        String me = BankRecAuditService.currentUserId();
        if (me != null && me.equals(r.getPreparedById())) {
            throw new BusinessException("A reconciliation must be reviewed by someone other than the person who prepared it");
        }
        recalculate(r);
        r.setReviewedById(me);
        r.setReviewedByName(BankRecAuditService.currentUserName());
        r.setReviewedAt(LocalDateTime.now());
        r.setReviewNotes(trim(notes));
        BankReconciliation saved = repository.save(r);
        auditService.record(id, r.getBankAccount().getId(), ReconciliationAuditAction.REVIEWED,
                "Reviewed with a difference of " + r.getDifference().toPlainString()
                        + (trim(notes) != null ? ". Notes: " + notes.trim() : ""));
        return BankRecMapper.toResponse(saved);
    }

    /**
     * Completes the reconciliation when the statement balance equals book balance +/-
     * outstanding items within tolerance. A difference outside tolerance needs the override
     * permission and a reason. Proposed matches must be confirmed or rejected first.
     */
    @Transactional
    public ReconciliationResponse complete(Long id, CompleteReconciliationRequest request) {
        BankReconciliation r = find(id);
        requireOpen(r);
        long proposed = matchRepository.countByReconciliationIdAndStatus(id, MatchStatus.PROPOSED);
        if (proposed > 0) {
            throw new BusinessException(proposed + " suggested match" + (proposed == 1 ? " is" : "es are")
                    + " waiting. Confirm or reject them before completing");
        }
        ReconciliationFigures figures = recalculate(r);
        String overrideReason = request != null ? trim(request.getOverrideReason()) : null;
        if (!figures.withinTolerance()) {
            if (!permissionService.currentUserHasPermission(OVERRIDE_PERMISSION)) {
                throw new BusinessException("The reconciliation is out by " + figures.difference().toPlainString()
                        + " (tolerance " + figures.tolerance().toPlainString() + "). Resolve the difference or ask someone "
                        + "with the override permission to complete it");
            }
            if (overrideReason == null) {
                throw new BusinessException("Give a reason for completing with a difference of " + figures.difference().toPlainString());
            }
            r.setDifferenceOverridden(true);
            r.setOverrideReason(overrideReason);
        } else {
            r.setDifferenceOverridden(false);
            r.setOverrideReason(null);
        }

        for (BankReconciliationMatchItem item : matchItemRepository.findActiveByReconciliation(id, ACTIVE)) {
            BankStatementTransaction t = item.getStatementTransaction();
            if (t != null && t.getRemainingAmount().signum() <= 0) {
                t.setClearedInReconciliation(r);
                statementRepository.save(t);
            }
        }
        r.setStatus(ReconciliationStatus.RECONCILED);
        r.setCompletedById(BankRecAuditService.currentUserId());
        r.setCompletedByName(BankRecAuditService.currentUserName());
        r.setCompletedAt(LocalDateTime.now());
        BankReconciliation saved = repository.save(r);
        auditService.record(id, r.getBankAccount().getId(), ReconciliationAuditAction.COMPLETED,
                "Completed with a difference of " + figures.difference().toPlainString()
                        + (r.getDifferenceOverridden() ? " (override: " + overrideReason + ")" : ""));
        return BankRecMapper.toResponse(saved);
    }

    /** Only the latest completed reconciliation of an account can be reopened, and only when none is open. */
    @Transactional
    public ReconciliationResponse reopen(Long id, String reason) {
        BankReconciliation r = find(id);
        if (r.getStatus() != ReconciliationStatus.RECONCILED) {
            throw new BusinessException("Only a completed reconciliation can be reopened");
        }
        String why = requireReason(reason, "reopening");
        BankReconciliation latest = lastReconciled(r.getBankAccount().getId(), null);
        if (latest != null && !latest.getId().equals(r.getId())) {
            throw new BusinessException("Only the latest completed reconciliation (" + latest.getReconciliationNumber()
                    + ") can be reopened");
        }
        if (!repository.findByBankAccountIdAndStatusInAndDeletedFalse(r.getBankAccount().getId(), openStatuses()).isEmpty()) {
            throw new BusinessException("Another reconciliation is open for this account. Finish or cancel it first");
        }
        for (BankReconciliationMatchItem item : matchItemRepository.findActiveByReconciliation(id, ACTIVE)) {
            BankStatementTransaction t = item.getStatementTransaction();
            if (t != null && t.getClearedInReconciliation() != null && t.getClearedInReconciliation().getId().equals(id)) {
                t.setClearedInReconciliation(null);
                statementRepository.save(t);
            }
        }
        r.setStatus(ReconciliationStatus.REOPENED);
        r.setReopenedById(BankRecAuditService.currentUserId());
        r.setReopenedByName(BankRecAuditService.currentUserName());
        r.setReopenedAt(LocalDateTime.now());
        r.setReopenReason(why);
        r.setReopenCount(r.getReopenCount() == null ? 1 : r.getReopenCount() + 1);
        r.setReviewedById(null);
        r.setReviewedByName(null);
        r.setReviewedAt(null);
        BankReconciliation saved = repository.save(r);
        auditService.record(id, r.getBankAccount().getId(), ReconciliationAuditAction.REOPENED, "Reopened: " + why);
        return BankRecMapper.toResponse(saved);
    }

    /** Releases every match. Posted adjustments must be reversed first, since cancelling posts nothing. */
    @Transactional
    public ReconciliationResponse cancel(Long id, String reason) {
        BankReconciliation r = find(id);
        if (r.getStatus() != ReconciliationStatus.DRAFT && r.getStatus() != ReconciliationStatus.IN_PROGRESS) {
            throw new BusinessException(r.getStatus() == ReconciliationStatus.REOPENED
                    ? "A reopened reconciliation cannot be cancelled; complete it again instead"
                    : "Only a draft or in-progress reconciliation can be cancelled");
        }
        String why = requireReason(reason, "cancelling");
        if (!adjustmentRepository.findByReconciliationIdAndStatus(id, AdjustmentStatus.POSTED).isEmpty()) {
            throw new BusinessException("Reverse this reconciliation's posted adjustments before cancelling it");
        }
        List<BankReconciliationMatch> active = matchRepository.findByReconciliationIdAndStatusIn(id, ACTIVE);
        for (BankReconciliationMatch m : active) {
            release(m, MatchStatus.UNMATCHED, "Reconciliation cancelled");
        }
        r.setStatus(ReconciliationStatus.CANCELLED);
        r.setCancelledByName(BankRecAuditService.currentUserName());
        r.setCancelledAt(LocalDateTime.now());
        r.setCancelReason(why);
        recalculate(r);
        BankReconciliation saved = repository.save(r);
        auditService.record(id, r.getBankAccount().getId(), ReconciliationAuditAction.CANCELLED,
                "Cancelled (" + active.size() + " matches released): " + why);
        return BankRecMapper.toResponse(saved);
    }

    // =====================================================================================
    // Workspace: the two sides
    // =====================================================================================

    /**
     * Statement lines for the workspace: those still open as at the period end plus those this
     * reconciliation matched. {@code status} is UNMATCHED, PARTIALLY_MATCHED or MATCHED.
     */
    @Transactional(readOnly = true)
    public List<StatementTransactionResponse> statementTransactions(Long id, String status, String search,
                                                                    LocalDate from, LocalDate to,
                                                                    BigDecimal minAmount, BigDecimal maxAmount) {
        BankReconciliation r = find(id);
        return filterStatement(statementSide(r), status, search, from, to, minAmount, maxAmount);
    }

    @Transactional(readOnly = true)
    public List<BookTransactionResponse> bookTransactions(Long id, String status, String search,
                                                          LocalDate from, LocalDate to,
                                                          BigDecimal minAmount, BigDecimal maxAmount) {
        BankReconciliation r = find(id);
        return filterBook(bookSide(r), status, search, from, to, minAmount, maxAmount);
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> matches(Long id, MatchStatus status) {
        find(id);
        return matchRepository.findByReconciliationIdOrderByIdDesc(id).stream()
                .filter(m -> status == null || m.getStatus() == status)
                .map(BankRecMapper::toResponse).toList();
    }

    // =====================================================================================
    // Matching
    // =====================================================================================

    @Transactional
    public AutoMatchResultResponse autoMatch(Long id, AutoMatchRequest request) {
        BankReconciliation r = find(id);
        requireOpen(r);
        List<MatchingCriteria> rules = ruleService.criteriaFor(r.getBankAccount().getId(),
                request != null ? request.getRuleId() : null);

        // Only lines nobody has touched yet: automatic matching is strictly one-to-one and full.
        List<BankStatementTransaction> statement = openStatement(r).entrySet().stream()
                .filter(e -> e.getValue().signum() == 0)
                .map(Map.Entry::getKey)
                .toList();
        Map<JournalLine, BigDecimal> openBook = openBook(r);
        List<JournalLine> book = openBook.entrySet().stream()
                .filter(e -> e.getValue().signum() == 0)
                .map(Map.Entry::getKey)
                .toList();
        Map<Long, String[]> counterparties = counterparties(book);

        List<MatchingCandidate> statementCandidates = statement.stream()
                .map(t -> new MatchingCandidate(t.getId(), t.getTransactionDate(), t.getAmount(),
                        join(t.getReference(), t.getExternalTransactionId()), t.getDescription(), null, null, null))
                .toList();
        List<MatchingCandidate> bookCandidates = book.stream()
                .map(l -> {
                    String[] cp = counterparties.getOrDefault(l.getId(), new String[2]);
                    JournalEntry je = l.getJournalEntry();
                    return new MatchingCandidate(l.getId(), je.getJournalDate(), signed(l),
                            je.getReference(), join(je.getDescription(), l.getDescription()), je.getJournalNumber(), cp[0], cp[1]);
                })
                .toList();

        List<MatchProposal> proposals = MatchingEngine.run(statementCandidates, bookCandidates, rules);
        Map<Long, BankStatementTransaction> statementById = statement.stream()
                .collect(Collectors.toMap(BankStatementTransaction::getId, Function.identity()));
        Map<Long, JournalLine> bookById = book.stream().collect(Collectors.toMap(JournalLine::getId, Function.identity()));

        List<MatchResponse> created = new ArrayList<>();
        int confirmed = 0;
        for (MatchProposal p : proposals) {
            BankReconciliationMatch m = newMatch(r, MatchType.AUTOMATIC,
                    p.autoConfirm() ? MatchStatus.CONFIRMED : MatchStatus.PROPOSED, p.amount());
            m.setConfidence(p.confidence());
            m.setRuleName(p.ruleName());
            m.setMatchReasons(truncate(String.join("; ", p.reasons()), 500));
            if (p.autoConfirm()) {
                m.setConfirmedByName("Automatic matching");
                m.setConfirmedAt(LocalDateTime.now());
                confirmed++;
            }
            addStatementItem(m, statementById.get(p.statementId()), p.amount());
            addBookItem(m, bookById.get(p.bookId()), p.amount());
            created.add(BankRecMapper.toResponse(matchRepository.save(m)));
        }

        r.setLastAutoMatchAt(LocalDateTime.now());
        markInProgress(r);
        recalculate(r);
        repository.save(r);
        auditService.record(id, r.getBankAccount().getId(), ReconciliationAuditAction.AUTO_MATCHED,
                "Automatic matching: " + confirmed + " matched, " + (proposals.size() - confirmed)
                        + " suggested for review, from " + statement.size() + " statement and " + book.size() + " book lines");

        return AutoMatchResultResponse.builder()
                .statementCandidates(statement.size())
                .bookCandidates(book.size())
                .confirmed(confirmed)
                .proposed(proposals.size() - confirmed)
                .rulesApplied(rules.isEmpty() ? List.of(MatchingCriteria.standard().name())
                        : rules.stream().map(MatchingCriteria::name).toList())
                .matches(created)
                .build();
    }

    /** One-to-one, one-to-many or many-to-one; partial when the two sides' totals differ. */
    @Transactional
    public MatchResponse manualMatch(Long id, ManualMatchRequest request) {
        BankReconciliation r = find(id);
        requireOpen(r);
        BankReconciliationMatch match = createManualMatch(r, request);
        markInProgress(r);
        recalculate(r);
        repository.save(r);
        return BankRecMapper.toResponse(match);
    }

    @Transactional
    public List<MatchResponse> bulkMatch(Long id, List<ManualMatchRequest> requests) {
        BankReconciliation r = find(id);
        requireOpen(r);
        if (requests == null || requests.isEmpty()) {
            throw new BusinessException("Nothing to match");
        }
        List<MatchResponse> result = new ArrayList<>();
        for (ManualMatchRequest request : requests) {
            result.add(BankRecMapper.toResponse(createManualMatch(r, request)));
        }
        markInProgress(r);
        recalculate(r);
        repository.save(r);
        return result;
    }

    @Transactional
    public List<MatchResponse> confirmMatches(Long id, MatchActionRequest request) {
        BankReconciliation r = find(id);
        requireOpen(r);
        List<MatchResponse> result = new ArrayList<>();
        for (BankReconciliationMatch m : loadMatches(r, request)) {
            if (m.getStatus() != MatchStatus.PROPOSED) {
                throw new BusinessException("Only suggested matches can be confirmed");
            }
            m.setStatus(MatchStatus.CONFIRMED);
            m.setConfirmedByName(BankRecAuditService.currentUserName());
            m.setConfirmedAt(LocalDateTime.now());
            result.add(BankRecMapper.toResponse(matchRepository.save(m)));
            auditService.record(id, r.getBankAccount().getId(), ReconciliationAuditAction.MATCH_CONFIRMED,
                    "Confirmed suggested match #" + m.getId() + " for " + m.getMatchedAmount().toPlainString()
                            + " (confidence " + m.getConfidence() + ")");
        }
        recalculate(r);
        repository.save(r);
        return result;
    }

    @Transactional
    public List<MatchResponse> rejectMatches(Long id, MatchActionRequest request) {
        BankReconciliation r = find(id);
        requireOpen(r);
        List<MatchResponse> result = new ArrayList<>();
        for (BankReconciliationMatch m : loadMatches(r, request)) {
            if (m.getStatus() != MatchStatus.PROPOSED) {
                throw new BusinessException("Only suggested matches can be rejected; unmatch confirmed ones instead");
            }
            release(m, MatchStatus.REJECTED, trim(request.getReason()));
            result.add(BankRecMapper.toResponse(m));
            auditService.record(id, r.getBankAccount().getId(), ReconciliationAuditAction.MATCH_REJECTED,
                    "Rejected suggested match #" + m.getId() + " for " + m.getMatchedAmount().toPlainString());
        }
        recalculate(r);
        repository.save(r);
        return result;
    }

    @Transactional
    public List<MatchResponse> unmatch(Long id, MatchActionRequest request) {
        BankReconciliation r = find(id);
        requireOpen(r);
        List<MatchResponse> result = new ArrayList<>();
        for (BankReconciliationMatch m : loadMatches(r, request)) {
            if (!m.getStatus().isActive()) {
                throw new BusinessException("Match #" + m.getId() + " is already undone");
            }
            if (m.getMatchType() == MatchType.ADJUSTMENT) {
                throw new BusinessException("Match #" + m.getId() + " was made by an adjustment. Reverse the adjustment instead");
            }
            release(m, MatchStatus.UNMATCHED, trim(request.getReason()));
            result.add(BankRecMapper.toResponse(m));
            auditService.record(id, r.getBankAccount().getId(), ReconciliationAuditAction.UNMATCHED,
                    "Unmatched #" + m.getId() + " (" + m.getMatchedAmount().toPlainString() + ")"
                            + (trim(request.getReason()) != null ? ": " + request.getReason().trim() : ""));
        }
        recalculate(r);
        repository.save(r);
        return result;
    }

    // =====================================================================================
    // Adjustments
    // =====================================================================================

    @Transactional
    public AdjustmentResponse createAdjustment(Long id, CreateAdjustmentRequest request) {
        BankReconciliation r = find(id);
        requireOpen(r);
        ReconciliationAdjustmentType type = request.getAdjustmentType();
        if (type == null) {
            throw new BusinessException("Choose the adjustment type");
        }

        BankStatementTransaction statementLine = null;
        BigDecimal amount = request.getAmount();
        LocalDate date = request.getTransactionDate();
        if (request.getStatementTransactionId() != null) {
            statementLine = requireStatementLine(r, request.getStatementTransactionId());
            BigDecimal remaining = statementRemaining(r, statementLine);
            if (remaining.signum() <= 0) {
                throw new BusinessException("That statement line is already fully matched");
            }
            if ((statementLine.getAmount().signum() > 0) != type.isMoneyIn()) {
                throw new BusinessException(type.getLabel() + (type.isMoneyIn() ? " is money into" : " is money out of")
                        + " the bank, but the statement line is a " + (statementLine.getAmount().signum() > 0 ? "deposit" : "withdrawal"));
            }
            if (amount == null) {
                amount = remaining;
            } else if (amount.compareTo(remaining) > 0) {
                throw new BusinessException("The adjustment is larger than the statement line's unmatched amount ("
                        + remaining.toPlainString() + ")");
            }
            if (date == null) {
                date = statementLine.getTransactionDate();
            }
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("The adjustment amount must be positive");
        }
        if (date == null) {
            date = r.getPeriodEnd();
        }
        if (date.isAfter(r.getPeriodEnd())) {
            throw new BusinessException("The adjustment date must fall on or before the period end (" + r.getPeriodEnd() + ")");
        }

        AccountEntity offset = resolveOffsetAccount(type, request.getOffsetAccountId());
        if (offset.getAccountId().equals(r.getBankAccount().getGlAccountCode())) {
            throw new BusinessException("The offset account cannot be the bank account itself");
        }

        BankReconciliationAdjustment a = new BankReconciliationAdjustment();
        a.setReconciliation(r);
        a.setAdjustmentType(type);
        a.setTransactionDate(date);
        a.setAmount(amount.setScale(2, java.math.RoundingMode.HALF_UP));
        a.setDescription(truncate(trim(request.getDescription()) != null ? request.getDescription().trim()
                : statementLine != null ? statementLine.getDescription() : null, 500));
        a.setReference(truncate(trim(request.getReference()) != null ? request.getReference().trim()
                : statementLine != null ? statementLine.getReference() : null, 150));
        a.setOffsetAccount(offset);
        a.setStatementTransaction(statementLine);
        a.setStatus(AdjustmentStatus.POSTED);
        a.setPostedByName(BankRecAuditService.currentUserName());
        a.setPostedAt(LocalDateTime.now());
        a = adjustmentRepository.save(a);

        JournalEntry journal = journalService.postAdjustment(r, a);
        a.setJournal(journal);

        if (statementLine != null) {
            JournalLine bankLine = BankReconciliationJournalService.bankLine(journal, r.getBankAccount().getGlAccountCode());
            BankReconciliationMatch m = newMatch(r, MatchType.ADJUSTMENT, MatchStatus.CONFIRMED, a.getAmount());
            m.setConfirmedByName(a.getPostedByName());
            m.setConfirmedAt(LocalDateTime.now());
            m.setNotes(type.getLabel() + " adjustment " + journal.getJournalNumber());
            addStatementItem(m, statementLine, a.getAmount());
            addBookItem(m, bankLine, a.getAmount());
            a.setMatch(matchRepository.save(m));
        }
        a = adjustmentRepository.save(a);

        markInProgress(r);
        recalculate(r);
        repository.save(r);
        auditService.record(id, r.getBankAccount().getId(), ReconciliationAuditAction.ADJUSTMENT_POSTED,
                type.getLabel() + " of " + a.getAmount().toPlainString() + " posted as journal " + journal.getJournalNumber()
                        + " against " + offset.getAccountId() + " " + offset.getAccountName());
        return BankRecMapper.toResponse(a);
    }

    /**
     * Reverses an adjustment's journal on its own date, frees the statement line it covered and
     * pairs the original and reversing bank lines so neither shows as outstanding.
     */
    @Transactional
    public AdjustmentResponse reverseAdjustment(Long id, Long adjustmentId, String reason) {
        BankReconciliation r = find(id);
        requireOpen(r);
        String why = requireReason(reason, "reversing an adjustment");
        BankReconciliationAdjustment a = adjustmentRepository.findById(adjustmentId)
                .filter(x -> x.getReconciliation().getId().equals(id))
                .orElseThrow(() -> new ResourceNotFoundException("Adjustment not found with ID: " + adjustmentId));
        if (a.getStatus() != AdjustmentStatus.POSTED) {
            throw new BusinessException("This adjustment is already reversed");
        }
        if (a.getMatch() != null && a.getMatch().getStatus().isActive()) {
            release(a.getMatch(), MatchStatus.UNMATCHED, "Adjustment reversed: " + why);
        }
        JournalEntry reversal = journalService.reverseAdjustment(r, a, why);

        String bankCode = r.getBankAccount().getGlAccountCode();
        BankReconciliationMatch contra = newMatch(r, MatchType.ADJUSTMENT, MatchStatus.CONFIRMED, a.getAmount());
        contra.setConfirmedByName(BankRecAuditService.currentUserName());
        contra.setConfirmedAt(LocalDateTime.now());
        contra.setNotes("Adjustment " + a.getJournal().getJournalNumber() + " and its reversal " + reversal.getJournalNumber());
        addBookItem(contra, BankReconciliationJournalService.bankLine(a.getJournal(), bankCode), a.getAmount());
        addBookItem(contra, BankReconciliationJournalService.bankLine(reversal, bankCode), a.getAmount());
        matchRepository.save(contra);

        a.setStatus(AdjustmentStatus.REVERSED);
        a.setReversalJournalId(reversal.getId());
        a.setReversedByName(BankRecAuditService.currentUserName());
        a.setReversedAt(LocalDateTime.now());
        a.setReversalReason(truncate(why, 500));
        a = adjustmentRepository.save(a);

        recalculate(r);
        repository.save(r);
        auditService.record(id, r.getBankAccount().getId(), ReconciliationAuditAction.ADJUSTMENT_REVERSED,
                "Reversed " + a.getAdjustmentType().getLabel() + " of " + a.getAmount().toPlainString()
                        + " (journal " + reversal.getJournalNumber() + "): " + why);
        return BankRecMapper.toResponse(a);
    }

    @Transactional(readOnly = true)
    public List<AdjustmentResponse> adjustments(Long id) {
        find(id);
        return adjustmentRepository.findByReconciliationIdOrderByIdDesc(id).stream().map(BankRecMapper::toResponse).toList();
    }

    // =====================================================================================
    // Summary, reports, dashboard, audit
    // =====================================================================================

    /** Recalculates an open reconciliation (and saves it) or reads a closed one's frozen figures. */
    @Transactional
    public ReconciliationSummaryResponse summary(Long id) {
        BankReconciliation r = find(id);
        ReconciliationFigures figures;
        if (r.getStatus().isOpen()) {
            figures = recalculate(r);
            repository.save(r);
        } else {
            figures = storedFigures(r);
        }
        List<StatementTransactionResponse> statement = statementSide(r);
        List<BookTransactionResponse> book = bookSide(r);
        long proposed = matchRepository.countByReconciliationIdAndStatus(id, MatchStatus.PROPOSED);
        long confirmedCount = matchRepository.countByReconciliationIdAndStatus(id, MatchStatus.CONFIRMED);

        List<String> blockers = new ArrayList<>();
        boolean canOverride = permissionService.currentUserHasPermission(OVERRIDE_PERMISSION);
        if (!r.getStatus().isOpen()) {
            blockers.add("The reconciliation is " + r.getStatus().name().toLowerCase(Locale.ROOT).replace('_', ' '));
        } else {
            if (proposed > 0) {
                blockers.add(proposed + " suggested match" + (proposed == 1 ? "" : "es") + " to confirm or reject");
            }
            if (!figures.withinTolerance()) {
                blockers.add("Difference of " + figures.difference().toPlainString() + " is outside the tolerance of "
                        + figures.tolerance().toPlainString() + (canOverride ? " (you can override with a reason)" : ""));
            }
        }

        return ReconciliationSummaryResponse.builder()
                .reconciliation(BankRecMapper.toResponse(r))
                .figures(figures)
                .unmatchedStatementCount((int) statement.stream().filter(t -> t.getRemainingAmount().signum() > 0).count())
                .unmatchedBookCount((int) book.stream().filter(b -> b.getRemainingAmount().signum() > 0).count())
                .proposedMatchCount((int) proposed)
                .confirmedMatchCount((int) confirmedCount)
                .adjustmentCount(adjustmentRepository.findByReconciliationIdOrderByIdDesc(id).size())
                .completionBlockers(blockers)
                .canOverrideDifference(canOverride)
                .build();
    }

    @Transactional
    public ReconciliationReportResponse report(Long id) {
        ReconciliationSummaryResponse summary = summary(id);
        BankReconciliation r = find(id);
        List<BookTransactionResponse> openBook = bookSide(r).stream()
                .filter(b -> b.getRemainingAmount().signum() > 0).toList();
        return ReconciliationReportResponse.builder()
                .summary(summary)
                .outstandingDeposits(openBook.stream().filter(b -> b.getAmount().signum() > 0).toList())
                .outstandingWithdrawals(openBook.stream().filter(b -> b.getAmount().signum() < 0).toList())
                .unmatchedStatementTransactions(statementSide(r).stream().filter(t -> t.getRemainingAmount().signum() > 0).toList())
                .unmatchedBookTransactions(openBook)
                .adjustments(adjustments(id))
                .matches(matches(id, null))
                .history(history(r.getBankAccount().getId()))
                .build();
    }

    @Transactional(readOnly = true)
    public List<ReconciliationResponse> history(Long bankAccountId) {
        return repository.findHistory(bankAccountId).stream().map(BankRecMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> audit(Long id) {
        find(id);
        return auditLogRepository.findByReconciliationIdOrderByOccurredAtDescIdDesc(id).stream()
                .map(BankRecMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> accountAudit(Long bankAccountId) {
        return auditLogRepository.findByBankAccountIdOrderByOccurredAtDescIdDesc(bankAccountId).stream()
                .map(BankRecMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<DashboardAccountResponse> dashboard() {
        LocalDate today = LocalDate.now();
        List<DashboardAccountResponse> result = new ArrayList<>();
        for (BankAccount account : bankAccountRepository.findAll()) {
            if (Boolean.TRUE.equals(account.getDeleted()) || Boolean.FALSE.equals(account.getEnableReconciliation())) {
                continue;
            }
            BankReconciliation last = lastReconciled(account.getId(), null);
            BankReconciliation open = repository.findByBankAccountIdAndStatusInAndDeletedFalse(account.getId(), openStatuses())
                    .stream().findFirst().orElse(null);
            result.add(DashboardAccountResponse.builder()
                    .bankAccountId(account.getId())
                    .bankAccountName(account.getAccountName())
                    .bankName(account.getBankName())
                    .accountNumber(account.getAccountNumber())
                    .glAccountCode(account.getGlAccountCode())
                    .currency(account.getCurrency())
                    .currentBookBalance(ledgerRepository.balanceAsOf(account.getGlAccountCode(), LEDGER_STATUSES, today))
                    .lastReconciledPeriodEnd(last != null ? last.getPeriodEnd() : null)
                    .lastReconciledClosingBalance(last != null ? last.getClosingBankBalance() : null)
                    .openReconciliationId(open != null ? open.getId() : null)
                    .openReconciliationNumber(open != null ? open.getReconciliationNumber() : null)
                    .openReconciliationStatus(open != null ? open.getStatus() : null)
                    .openReconciliationDifference(open != null ? open.getDifference() : null)
                    .unmatchedStatementCount(statementRepository.countOpen(account.getId()))
                    .daysSinceLastReconciliation(last != null ? ChronoUnit.DAYS.between(last.getPeriodEnd(), today) : null)
                    .build());
        }
        result.sort(Comparator.comparing(DashboardAccountResponse::getBankAccountName, Comparator.nullsLast(String::compareToIgnoreCase)));
        return result;
    }

    // =====================================================================================
    // Calculation
    // =====================================================================================

    /** Refreshes the reconciliation's figures from the ledger, the statement and the matches. */
    ReconciliationFigures recalculate(BankReconciliation r) {
        String code = r.getBankAccount().getGlAccountCode();
        BigDecimal bookBalance = ledgerRepository.balanceAsOf(code, LEDGER_STATUSES, r.getPeriodEnd());

        BigDecimal deposits = BigDecimal.ZERO;
        BigDecimal withdrawals = BigDecimal.ZERO;
        for (Map.Entry<JournalLine, BigDecimal> e : openBook(r).entrySet()) {
            BigDecimal remaining = lineAmount(e.getKey()).subtract(e.getValue());
            if (e.getKey().getDebitAmount().signum() > 0) {
                deposits = deposits.add(remaining);
            } else {
                withdrawals = withdrawals.add(remaining);
            }
        }

        BigDecimal unmatchedStatement = BigDecimal.ZERO;
        for (Map.Entry<BankStatementTransaction, BigDecimal> e : openStatement(r).entrySet()) {
            BigDecimal remaining = e.getKey().getAbsoluteAmount().subtract(e.getValue());
            unmatchedStatement = unmatchedStatement.add(e.getKey().getAmount().signum() < 0 ? remaining.negate() : remaining);
        }

        BigDecimal charges = BigDecimal.ZERO;
        BigDecimal interest = BigDecimal.ZERO;
        BigDecimal net = BigDecimal.ZERO;
        for (BankReconciliationAdjustment a : adjustmentRepository.findByReconciliationIdAndStatus(r.getId(), AdjustmentStatus.POSTED)) {
            if (a.getAdjustmentType() == ReconciliationAdjustmentType.BANK_CHARGE) charges = charges.add(a.getAmount());
            if (a.getAdjustmentType() == ReconciliationAdjustmentType.BANK_INTEREST) interest = interest.add(a.getAmount());
            net = net.add(a.getSignedAmount());
        }

        BigDecimal movement = statementRepository.sumMovement(r.getBankAccount().getId(), r.getPeriodStart(), r.getPeriodEnd());

        ReconciliationFigures f = ReconciliationCalculator.calculate(r.getOpeningBankBalance(), r.getClosingBankBalance(),
                bookBalance, deposits, withdrawals, charges, interest, net, movement, unmatchedStatement, r.getTolerance());
        r.setBookBalance(f.bookBalance());
        r.setOutstandingDeposits(f.outstandingDeposits());
        r.setOutstandingWithdrawals(f.outstandingWithdrawals());
        r.setBankCharges(f.bankCharges());
        r.setBankInterest(f.bankInterest());
        r.setAdjustmentsTotal(f.adjustmentsTotal());
        r.setDifference(f.difference());
        return f;
    }

    /** Figures of a completed/cancelled reconciliation, from the values frozen on it. */
    private ReconciliationFigures storedFigures(BankReconciliation r) {
        BigDecimal movement = statementRepository.sumMovement(r.getBankAccount().getId(), r.getPeriodStart(), r.getPeriodEnd());
        BigDecimal unmatched = statementSide(r).stream()
                .map(t -> t.getAmount().signum() < 0 ? t.getRemainingAmount().negate() : t.getRemainingAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return ReconciliationCalculator.calculate(r.getOpeningBankBalance(), r.getClosingBankBalance(), r.getBookBalance(),
                r.getOutstandingDeposits(), r.getOutstandingWithdrawals(), r.getBankCharges(), r.getBankInterest(),
                r.getAdjustmentsTotal(), movement, unmatched, r.getTolerance());
    }

    // =====================================================================================
    // Internals
    // =====================================================================================

    /** Open statement lines as at the period end, with the amount already matched as at then. */
    private Map<BankStatementTransaction, BigDecimal> openStatement(BankReconciliation r) {
        List<BankStatementTransaction> lines = statementRepository.findOpenAsOf(r.getBankAccount().getId(), r.getPeriodEnd(), ACTIVE);
        Map<Long, BigDecimal> matched = statementMatchedAsOf(r, lines.stream().map(BankStatementTransaction::getId).toList());
        Map<BankStatementTransaction, BigDecimal> result = new LinkedHashMap<>();
        lines.forEach(t -> result.put(t, matched.getOrDefault(t.getId(), BigDecimal.ZERO)));
        return result;
    }

    private Map<JournalLine, BigDecimal> openBook(BankReconciliation r) {
        List<JournalLine> lines = ledgerRepository.findOpenLines(r.getBankAccount().getGlAccountCode(), LEDGER_STATUSES,
                r.getPeriodEnd(), ACTIVE);
        Map<Long, BigDecimal> matched = bookMatchedAsOf(r, lines.stream().map(JournalLine::getId).toList());
        Map<JournalLine, BigDecimal> result = new LinkedHashMap<>();
        lines.forEach(l -> result.put(l, matched.getOrDefault(l.getId(), BigDecimal.ZERO)));
        return result;
    }

    private Map<Long, BigDecimal> statementMatchedAsOf(BankReconciliation r, Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return toMap(matchItemRepository.sumMatchedByStatementAsOf(ids, ACTIVE, r.getPeriodEnd()));
    }

    private Map<Long, BigDecimal> bookMatchedAsOf(BankReconciliation r, Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return toMap(matchItemRepository.sumMatchedByJournalLineAsOf(ids, ACTIVE, r.getPeriodEnd()));
    }

    /** Workspace statement side: open lines plus everything this reconciliation matched. */
    private List<StatementTransactionResponse> statementSide(BankReconciliation r) {
        Map<Long, BankStatementTransaction> lines = new LinkedHashMap<>();
        openStatement(r).keySet().forEach(t -> lines.put(t.getId(), t));
        for (BankReconciliationMatchItem item : matchItemRepository.findActiveByReconciliation(r.getId(), ACTIVE)) {
            if (item.getStatementTransaction() != null) {
                lines.putIfAbsent(item.getStatementTransaction().getId(), item.getStatementTransaction());
            }
        }
        Map<Long, BigDecimal> matched = statementMatchedAsOf(r, lines.keySet());
        return lines.values().stream()
                .sorted(Comparator.comparing(BankStatementTransaction::getTransactionDate).thenComparing(BankStatementTransaction::getId))
                .map(t -> BankRecMapper.toResponse(t, matched.getOrDefault(t.getId(), BigDecimal.ZERO)))
                .toList();
    }

    private List<BookTransactionResponse> bookSide(BankReconciliation r) {
        Map<Long, JournalLine> lines = new LinkedHashMap<>();
        openBook(r).keySet().forEach(l -> lines.put(l.getId(), l));
        Set<Long> extra = new HashSet<>();
        for (BankReconciliationMatchItem item : matchItemRepository.findActiveByReconciliation(r.getId(), ACTIVE)) {
            if (item.getJournalLine() != null && !lines.containsKey(item.getJournalLine().getId())) {
                extra.add(item.getJournalLine().getId());
            }
        }
        if (!extra.isEmpty()) {
            ledgerRepository.findWithJournal(extra).forEach(l -> lines.put(l.getId(), l));
        }
        Map<Long, BigDecimal> matched = bookMatchedAsOf(r, lines.keySet());
        Map<Long, String[]> counterparties = counterparties(lines.values());
        return lines.values().stream()
                .sorted(Comparator.comparing((JournalLine l) -> l.getJournalEntry().getJournalDate()).thenComparing(JournalLine::getId))
                .map(l -> toBookResponse(l, matched.getOrDefault(l.getId(), BigDecimal.ZERO), counterparties.get(l.getId())))
                .toList();
    }

    private BankReconciliationMatch createManualMatch(BankReconciliation r, ManualMatchRequest request) {
        if (request == null) {
            throw new BusinessException("Nothing to match");
        }
        List<Long> statementIds = distinct(request.getStatementTransactionIds());
        List<Long> bookIds = distinct(request.getJournalLineIds());
        List<BankStatementTransaction> statement = statementIds.stream().map(sid -> requireStatementLine(r, sid)).toList();
        Map<Long, JournalLine> bookById = bookIds.isEmpty() ? Map.of()
                : ledgerRepository.findWithJournal(bookIds).stream().collect(Collectors.toMap(JournalLine::getId, Function.identity()));
        List<JournalLine> book = new ArrayList<>();
        for (Long bid : bookIds) {
            JournalLine line = bookById.get(bid);
            if (line == null) {
                throw new ResourceNotFoundException("Book transaction not found with ID: " + bid);
            }
            requireBankLine(r, line);
            book.add(line);
        }

        Map<Long, BigDecimal> statementMatched = statementMatchedAsOf(r, statementIds);
        Map<Long, BigDecimal> bookMatched = bookMatchedAsOf(r, bookIds);
        Map<Long, BigDecimal> requestedStatement = request.getStatementAllocations() != null ? request.getStatementAllocations() : Map.of();
        Map<Long, BigDecimal> requestedBook = request.getBookAllocations() != null ? request.getBookAllocations() : Map.of();

        List<AllocationItem> statementItems = statement.stream().map(t -> {
            BigDecimal remaining = t.getAbsoluteAmount().subtract(statementMatched.getOrDefault(t.getId(), BigDecimal.ZERO));
            return new AllocationItem(t.getId(), t.getAmount().signum() < 0 ? remaining.negate() : remaining,
                    requestedStatement.get(t.getId()));
        }).toList();
        List<AllocationItem> bookItems = book.stream().map(l -> {
            BigDecimal remaining = lineAmount(l).subtract(bookMatched.getOrDefault(l.getId(), BigDecimal.ZERO));
            return new AllocationItem(l.getId(), l.getDebitAmount().signum() > 0 ? remaining : remaining.negate(),
                    requestedBook.get(l.getId()));
        }).toList();

        MatchAllocator.Allocation allocation = MatchAllocator.allocate(statementItems, bookItems);

        BankReconciliationMatch m = newMatch(r, MatchType.MANUAL, MatchStatus.CONFIRMED, allocation.amount());
        m.setConfirmedByName(m.getMatchedByName());
        m.setConfirmedAt(m.getMatchedAt());
        m.setNotes(trim(request.getNotes()));
        Map<Long, BankStatementTransaction> statementById = statement.stream()
                .collect(Collectors.toMap(BankStatementTransaction::getId, Function.identity()));
        allocation.statement().forEach((sid, amount) -> addStatementItem(m, statementById.get(sid), amount));
        allocation.book().forEach((bid, amount) -> addBookItem(m, bookById.get(bid), amount));
        BankReconciliationMatch saved = matchRepository.save(m);

        BigDecimal statementTotal = statementItems.stream().map(i -> i.signedRemaining().abs()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal bookTotal = bookItems.stream().map(i -> i.signedRemaining().abs()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal left = statementTotal.subtract(bookTotal);
        auditService.record(r.getId(), r.getBankAccount().getId(), ReconciliationAuditAction.MATCHED,
                "Matched " + statement.size() + " statement line(s) to " + book.size() + " book line(s) for "
                        + allocation.amount().toPlainString()
                        + (left.signum() != 0 ? " (partial: " + left.abs().toPlainString() + " left on the "
                        + (left.signum() > 0 ? "statement" : "book") + " side)" : ""));
        return saved;
    }

    private BankReconciliationMatch newMatch(BankReconciliation r, MatchType type, MatchStatus status, BigDecimal amount) {
        BankReconciliationMatch m = new BankReconciliationMatch();
        m.setReconciliation(r);
        m.setMatchType(type);
        m.setStatus(status);
        m.setMatchedAmount(amount);
        m.setMatchedByName(type == MatchType.AUTOMATIC ? "Automatic matching" : BankRecAuditService.currentUserName());
        m.setMatchedAt(LocalDateTime.now());
        return m;
    }

    private void addStatementItem(BankReconciliationMatch m, BankStatementTransaction t, BigDecimal amount) {
        BankReconciliationMatchItem item = new BankReconciliationMatchItem();
        item.setSide(MatchItemSide.STATEMENT);
        item.setStatementTransaction(t);
        item.setAmount(amount);
        m.addItem(item);
        t.setMatchedAmount(t.getMatchedAmount().add(amount));
        t.setStatus(BankRecMapper.statementStatus(t.getAbsoluteAmount(), t.getMatchedAmount()));
        statementRepository.save(t);
    }

    private void addBookItem(BankReconciliationMatch m, JournalLine line, BigDecimal amount) {
        BankReconciliationMatchItem item = new BankReconciliationMatchItem();
        item.setSide(MatchItemSide.BOOK);
        item.setJournalLine(line);
        item.setAmount(amount);
        m.addItem(item);
    }

    /** Ends a match (unmatch/reject) and gives its amounts back to the statement lines. */
    private void release(BankReconciliationMatch m, MatchStatus newStatus, String reason) {
        for (BankReconciliationMatchItem item : m.getItems()) {
            BankStatementTransaction t = item.getStatementTransaction();
            if (t != null) {
                t.setMatchedAmount(t.getMatchedAmount().subtract(item.getAmount()).max(BigDecimal.ZERO));
                t.setStatus(BankRecMapper.statementStatus(t.getAbsoluteAmount(), t.getMatchedAmount()));
                t.setClearedInReconciliation(null);
                statementRepository.save(t);
            }
        }
        m.setStatus(newStatus);
        m.setUnmatchedByName(BankRecAuditService.currentUserName());
        m.setUnmatchedAt(LocalDateTime.now());
        m.setUnmatchReason(truncate(reason, 500));
        matchRepository.save(m);
    }

    private List<BankReconciliationMatch> loadMatches(BankReconciliation r, MatchActionRequest request) {
        List<Long> ids = request != null ? distinct(request.getMatchIds()) : List.of();
        if (ids.isEmpty()) {
            throw new BusinessException("Select at least one match");
        }
        List<BankReconciliationMatch> matches = matchRepository.findAllById(ids);
        if (matches.size() != ids.size() || matches.stream().anyMatch(m -> !m.getReconciliation().getId().equals(r.getId()))) {
            throw new BusinessException("One or more matches do not belong to this reconciliation");
        }
        return matches;
    }

    private BankStatementTransaction requireStatementLine(BankReconciliation r, Long statementId) {
        BankStatementTransaction t = statementRepository.findById(statementId)
                .orElseThrow(() -> new ResourceNotFoundException("Statement transaction not found with ID: " + statementId));
        if (!t.getBankAccount().getId().equals(r.getBankAccount().getId())) {
            throw new BusinessException("Statement transaction " + statementId + " belongs to another bank account");
        }
        if (t.getTransactionDate().isAfter(r.getPeriodEnd())) {
            throw new BusinessException("Statement transaction on " + t.getTransactionDate() + " is after the period end");
        }
        return t;
    }

    private void requireBankLine(BankReconciliation r, JournalLine line) {
        JournalEntry je = line.getJournalEntry();
        if (!r.getBankAccount().getGlAccountCode().equals(line.getAccount().getAccountId())) {
            throw new BusinessException("Journal " + je.getJournalNumber() + " line is not on this bank's GL account");
        }
        if (!LEDGER_STATUSES.contains(je.getStatus())) {
            throw new BusinessException("Journal " + je.getJournalNumber() + " is not posted");
        }
        if (je.getJournalDate().isAfter(r.getPeriodEnd())) {
            throw new BusinessException("Journal " + je.getJournalNumber() + " is dated after the period end");
        }
    }

    private BigDecimal statementRemaining(BankReconciliation r, BankStatementTransaction t) {
        return t.getAbsoluteAmount().subtract(statementMatchedAsOf(r, List.of(t.getId())).getOrDefault(t.getId(), BigDecimal.ZERO));
    }

    private AccountEntity resolveOffsetAccount(ReconciliationAdjustmentType type, Long offsetAccountId) {
        if (offsetAccountId != null) {
            return accountRepository.findById(offsetAccountId)
                    .orElseThrow(() -> new BusinessException("Account not found with ID: " + offsetAccountId));
        }
        String code = accountingMappingService.resolve(type.getDefaultOffsetAccount());
        return accountRepository.findByAccountId(code).orElseThrow(() -> new BusinessException(
                "Required accounting configuration missing: \"" + type.getDefaultOffsetAccount().getDescription()
                        + "\" is mapped to account code \"" + code + "\", which does not exist in the Chart of Accounts. "
                        + "Choose an offset account or configure it under Settings > Accounting Mappings."));
    }

    /** [counterparty name, counterparty reference] for book lines that came from customer/supplier payments. */
    private Map<Long, String[]> counterparties(Collection<JournalLine> lines) {
        Map<Long, List<Long>> paymentLines = new HashMap<>();
        Map<Long, List<Long>> supplierPaymentLines = new HashMap<>();
        for (JournalLine l : lines) {
            JournalEntry je = l.getJournalEntry();
            if (je.getSourceEntityId() == null) continue;
            if ("PAYMENT".equals(je.getSourceModule())) {
                paymentLines.computeIfAbsent(je.getSourceEntityId(), k -> new ArrayList<>()).add(l.getId());
            } else if ("SUPPLIER_PAYMENT".equals(je.getSourceModule())) {
                supplierPaymentLines.computeIfAbsent(je.getSourceEntityId(), k -> new ArrayList<>()).add(l.getId());
            }
        }
        Map<Long, String[]> result = new HashMap<>();
        if (!paymentLines.isEmpty()) {
            for (PaymentEntity p : paymentRepository.findAllById(paymentLines.keySet())) {
                String[] cp = {p.getCustomer() != null ? p.getCustomer().getDisplayName() : null, p.getReferenceNumber()};
                paymentLines.get(p.getId()).forEach(id -> result.put(id, cp));
            }
        }
        if (!supplierPaymentLines.isEmpty()) {
            for (SupplierPaymentEntity p : supplierPaymentRepository.findAllById(supplierPaymentLines.keySet())) {
                String[] cp = {p.getSupplier() != null ? p.getSupplier().getDisplayName() : null, p.getReferenceNumber()};
                supplierPaymentLines.get(p.getId()).forEach(id -> result.put(id, cp));
            }
        }
        return result;
    }

    private static BookTransactionResponse toBookResponse(JournalLine l, BigDecimal matched, String[] counterparty) {
        JournalEntry je = l.getJournalEntry();
        BigDecimal remaining = lineAmount(l).subtract(matched);
        BookTransactionStatus status = matched.signum() <= 0 ? BookTransactionStatus.UNMATCHED
                : remaining.signum() <= 0 ? BookTransactionStatus.MATCHED : BookTransactionStatus.PARTIALLY_MATCHED;
        return BookTransactionResponse.builder()
                .id(l.getId())
                .journalId(je.getId())
                .journalNumber(je.getJournalNumber())
                .journalDate(je.getJournalDate())
                .reference(je.getReference())
                .description(l.getDescription() != null && !l.getDescription().isBlank() ? l.getDescription() : je.getDescription())
                .sourceModule(je.getSourceModule())
                .sourceEntityId(je.getSourceEntityId())
                .counterparty(counterparty != null ? counterparty[0] : null)
                .debitAmount(l.getDebitAmount())
                .creditAmount(l.getCreditAmount())
                .amount(signed(l))
                .matchedAmount(matched)
                .remainingAmount(remaining)
                .status(status)
                .build();
    }

    private static List<StatementTransactionResponse> filterStatement(List<StatementTransactionResponse> rows, String status,
                                                                      String search, LocalDate from, LocalDate to,
                                                                      BigDecimal min, BigDecimal max) {
        String needle = lower(search);
        return rows.stream()
                .filter(t -> matchesStatus(status, t.getStatus().name()))
                .filter(t -> from == null || !t.getTransactionDate().isBefore(from))
                .filter(t -> to == null || !t.getTransactionDate().isAfter(to))
                .filter(t -> min == null || t.getAmount().abs().compareTo(min) >= 0)
                .filter(t -> max == null || t.getAmount().abs().compareTo(max) <= 0)
                .filter(t -> needle == null || containsAny(needle, t.getDescription(), t.getReference(),
                        t.getExternalTransactionId(), t.getAmount().toPlainString()))
                .toList();
    }

    private static List<BookTransactionResponse> filterBook(List<BookTransactionResponse> rows, String status,
                                                            String search, LocalDate from, LocalDate to,
                                                            BigDecimal min, BigDecimal max) {
        String needle = lower(search);
        return rows.stream()
                .filter(b -> matchesStatus(status, b.getStatus().name()))
                .filter(b -> from == null || !b.getJournalDate().isBefore(from))
                .filter(b -> to == null || !b.getJournalDate().isAfter(to))
                .filter(b -> min == null || b.getAmount().abs().compareTo(min) >= 0)
                .filter(b -> max == null || b.getAmount().abs().compareTo(max) <= 0)
                .filter(b -> needle == null || containsAny(needle, b.getDescription(), b.getReference(), b.getJournalNumber(),
                        b.getCounterparty(), b.getAmount().toPlainString()))
                .toList();
    }

    /** "OPEN" is shorthand for unmatched or partially matched. */
    private static boolean matchesStatus(String filter, String status) {
        if (filter == null || filter.isBlank()) return true;
        if (filter.equalsIgnoreCase("OPEN")) {
            return status.equals(StatementTransactionStatus.UNMATCHED.name()) || status.equals(StatementTransactionStatus.PARTIALLY_MATCHED.name());
        }
        return filter.equalsIgnoreCase(status);
    }

    private static boolean containsAny(String needle, String... values) {
        for (String v : values) {
            if (v != null && v.toLowerCase(Locale.ROOT).contains(needle)) return true;
        }
        return false;
    }

    private BankReconciliation find(Long id) {
        return repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bank reconciliation not found with ID: " + id));
    }

    private BankReconciliation lastReconciled(Long bankAccountId, Long excludeId) {
        return repository.findByAccountAndStatusLatestFirst(bankAccountId, ReconciliationStatus.RECONCILED).stream()
                .filter(r -> excludeId == null || !r.getId().equals(excludeId))
                .findFirst().orElse(null);
    }

    private BankAccount requireBankAccount(Long id) {
        if (id == null) {
            throw new BusinessException("Choose the bank account to reconcile");
        }
        BankAccount account = bankAccountRepository.findById(id)
                .filter(a -> !Boolean.TRUE.equals(a.getDeleted()))
                .orElseThrow(() -> new BusinessException("Bank account not found with ID: " + id));
        if (Boolean.FALSE.equals(account.getEnableReconciliation())) {
            throw new BusinessException("Reconciliation is turned off for " + account.getAccountName()
                    + ". Turn it on under Settings > Bank Accounts");
        }
        if (account.getStatus() != null && account.getStatus() != BankAccountStatus.ACTIVE) {
            throw new BusinessException(account.getAccountName() + " is not active");
        }
        return account;
    }

    private static void validateHeader(SaveReconciliationRequest request) {
        if (request.getStatementDate() == null || request.getPeriodStart() == null || request.getPeriodEnd() == null) {
            throw new BusinessException("Statement date, period start and period end are required");
        }
        if (request.getPeriodEnd().isBefore(request.getPeriodStart())) {
            throw new BusinessException("The period end cannot be before the period start");
        }
        if (request.getStatementDate().isBefore(request.getPeriodStart())) {
            throw new BusinessException("The statement date cannot be before the period start");
        }
        if (request.getClosingBankBalance() == null) {
            throw new BusinessException("Enter the closing balance from the bank statement");
        }
        if (request.getTolerance() != null && request.getTolerance().signum() < 0) {
            throw new BusinessException("Tolerance cannot be negative");
        }
    }

    private static void applyHeader(BankReconciliation r, SaveReconciliationRequest request) {
        r.setStatementDate(request.getStatementDate());
        r.setPeriodStart(request.getPeriodStart());
        r.setPeriodEnd(request.getPeriodEnd());
        if (request.getOpeningBankBalance() != null) {
            r.setOpeningBankBalance(request.getOpeningBankBalance());
        }
        r.setClosingBankBalance(request.getClosingBankBalance());
        r.setTolerance(request.getTolerance() != null ? request.getTolerance() : BigDecimal.ZERO);
        r.setNotes(trim(request.getNotes()));
    }

    private static void requireOpen(BankReconciliation r) {
        if (!r.getStatus().isOpen()) {
            throw new BusinessException("Reconciliation " + r.getReconciliationNumber() + " is "
                    + r.getStatus().name().toLowerCase(Locale.ROOT) + (r.getStatus() == ReconciliationStatus.RECONCILED
                    ? ". Reopen it to make changes" : ""));
        }
    }

    private void markInProgress(BankReconciliation r) {
        if (r.getStatus() == ReconciliationStatus.DRAFT) {
            r.setStatus(ReconciliationStatus.IN_PROGRESS);
        }
    }

    private static Set<ReconciliationStatus> openStatuses() {
        return EnumSet.of(ReconciliationStatus.DRAFT, ReconciliationStatus.IN_PROGRESS, ReconciliationStatus.REOPENED);
    }

    private static String requireReason(String reason, String action) {
        String why = trim(reason);
        if (why == null) {
            throw new BusinessException("A reason is required for " + action);
        }
        return why;
    }

    static BigDecimal lineAmount(JournalLine l) {
        return l.getDebitAmount().add(l.getCreditAmount());
    }

    static BigDecimal signed(JournalLine l) {
        return l.getDebitAmount().subtract(l.getCreditAmount());
    }

    private static Map<Long, BigDecimal> toMap(List<Object[]> rows) {
        Map<Long, BigDecimal> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], (BigDecimal) row[1]);
        }
        return map;
    }

    private static List<Long> distinct(List<Long> ids) {
        return ids == null ? List.of() : ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    private static String join(String a, String b) {
        if (a == null) return b;
        if (b == null) return a;
        return a + " " + b;
    }

    private static String lower(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String trim(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
