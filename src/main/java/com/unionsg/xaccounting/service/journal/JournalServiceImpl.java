package com.unionsg.xaccounting.service.journal;

import com.unionsg.xaccounting.MapperLayer.JournalMapper;
import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.dto.journal.JournalReversalResponse;
import com.unionsg.xaccounting.dto.journal.ReverseJournalRequest;
import com.unionsg.xaccounting.dto.journal.UpdateJournalRequest;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.enums.DocumentModule;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.exception.BadRequestException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.journal.JournalEntryRepository;
import com.unionsg.xaccounting.service.DocumentNumberService;
import com.unionsg.xaccounting.service.accounting.PeriodLockGuard;
import com.unionsg.xaccounting.service.config.ConfigValueValidator;
import com.unionsg.xaccounting.MapperLayer.CreatedByMapper;
import com.unionsg.xaccounting.dto.branch.JournalApprovalEventResponse;
import com.unionsg.xaccounting.entity.Journals.JournalApprovalEvent;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.entity.branch.Branch;
import com.unionsg.xaccounting.entity.branch.BranchAccountingSettings;
import com.unionsg.xaccounting.entity.branch.CostCenter;
import com.unionsg.xaccounting.enums.JournalApprovalAction;
import com.unionsg.xaccounting.exception.AccessDeniedException;
import com.unionsg.xaccounting.repository.journal.JournalApprovalEventRepository;
import com.unionsg.xaccounting.security.service.PermissionService;
import com.unionsg.xaccounting.security.util.SecurityUtils;
import com.unionsg.xaccounting.service.branch.AccessScope;
import com.unionsg.xaccounting.service.branch.AccessScopeService;
import com.unionsg.xaccounting.service.branch.BranchAccountingSettingsService;
import com.unionsg.xaccounting.service.branch.InterBranchBalancer;
import com.unionsg.xaccounting.service.branch.JournalDimensionResolver;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.criteria.Root;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class JournalServiceImpl implements JournalService {

    private final JournalEntryRepository journalRepository;
    private final AccountRepository accountRepository;
    private final JournalMapper journalMapper;
    private final JournalPostingService postingService;
    private final JournalNumberGenerator numberGenerator;
    private final DocumentNumberService generalSequenceGeneratorService;
    private final PeriodLockGuard periodLockGuard;
    private final ConfigValueValidator configValues;
    private final JournalDimensionResolver dimensionResolver;
    private final InterBranchBalancer interBranchBalancer;
    private final AccessScopeService accessScopeService;
    private final BranchAccountingSettingsService branchSettings;
    private final JournalApprovalEventRepository approvalEventRepository;
    private final PermissionService permissionService;
    private final JournalDocumentService journalDocumentService;

    public JournalServiceImpl(
            JournalEntryRepository journalRepository,
            AccountRepository accountRepository,
            JournalMapper journalMapper,
            JournalPostingService postingService,
            JournalNumberGenerator numberGenerator,
            DocumentNumberService generalSequenceGeneratorService,
            PeriodLockGuard periodLockGuard,
            ConfigValueValidator configValues,
            JournalDimensionResolver dimensionResolver,
            InterBranchBalancer interBranchBalancer,
            AccessScopeService accessScopeService,
            BranchAccountingSettingsService branchSettings,
            JournalApprovalEventRepository approvalEventRepository,
            PermissionService permissionService,
            JournalDocumentService journalDocumentService
    ) {
        this.journalRepository = journalRepository;
        this.accountRepository = accountRepository;
        this.journalMapper = journalMapper;
        this.postingService = postingService;
        this.numberGenerator = numberGenerator;
        this.generalSequenceGeneratorService = generalSequenceGeneratorService;
        this.periodLockGuard = periodLockGuard;
        this.configValues = configValues;
        this.dimensionResolver = dimensionResolver;
        this.interBranchBalancer = interBranchBalancer;
        this.accessScopeService = accessScopeService;
        this.branchSettings = branchSettings;
        this.approvalEventRepository = approvalEventRepository;
        this.permissionService = permissionService;
        this.journalDocumentService = journalDocumentService;
    }

    @Override
    public JournalResponse create(CreateJournalRequest request) {
        return toResponse(journalRepository.save(build(request, false)));
    }

    @Override
    public JournalResponse createManualJournal(CreateJournalRequest request) {
        assertManualType(request.getJournalType());
        request.setCurrencyCode(configValues.require("currencies", request.getCurrencyCode(), null, "Currency"));
        assertNoControlAccountLines(request.getLines());
        return toResponse(journalRepository.save(build(request, true)));
    }

    private JournalEntry build(CreateJournalRequest request, boolean manual) {

        JournalEntry journal = new JournalEntry();

        journal.setJournalNumber(generalSequenceGeneratorService.generateNextNumber(DocumentModule.JOURNAL));
        journal.setJournalDate(request.getJournalDate());
        journal.setReference(request.getReference());
        journal.setDescription(request.getDescription());
        journal.setJournalType(request.getJournalType());
        journal.setCurrencyCode(request.getCurrencyCode());
        journal.setManualEntry(manual);

        applyHeaderDimensions(journal, request.getBranchId(), request.getCostCenterId());

        buildLines(journal, request.getLines());

        interBranchBalancer.balance(journal);

        calculateTotals(journal);

        validateBalanced(journal);

        if (manual) {
            assertLinesInScope(journal, "post to");
            journal.setRequiresApproval(needsApproval(journal));
        }

        return journal;
    }

    @Override
    public JournalResponse update(Long id, UpdateJournalRequest request) {

        JournalEntry journal = getEntity(id);

        if (journal.getStatus() != JournalStatus.DRAFT && journal.getStatus() != JournalStatus.REJECTED) {
            throw new BadRequestException("Only draft or rejected journals can be edited");
        }

        if (Boolean.TRUE.equals(journal.getManualEntry())) {
            assertLinesInScope(journal, "edit");
        }

        assertNoControlAccountLines(request.getLines());

        if (request.getJournalType() != null && request.getJournalType() != journal.getJournalType()) {
            assertManualType(request.getJournalType());
            journal.setJournalType(request.getJournalType());
        }
        if (request.getCurrencyCode() != null) {
            journal.setCurrencyCode(configValues.require(
                    "currencies", request.getCurrencyCode(), journal.getCurrencyCode(), "Currency"));
        }
        journal.setJournalDate(request.getJournalDate());
        journal.setReference(request.getReference());
        journal.setDescription(request.getDescription());

        if (request.getBranchId() != null || request.getCostCenterId() != null) {
            applyHeaderDimensions(journal, request.getBranchId(), request.getCostCenterId());
        }

        journal.getLines().clear();

        buildLines(journal, request.getLines());

        interBranchBalancer.balance(journal);

        calculateTotals(journal);

        if (Boolean.TRUE.equals(journal.getManualEntry())) {
            assertLinesInScope(journal, "post to");
            journal.setRequiresApproval(needsApproval(journal));
        }

        if (journal.getStatus() == JournalStatus.REJECTED) {
            // Editing a rejected journal sends it back to draft so it can be resubmitted.
            journal.setStatus(JournalStatus.DRAFT);
        }

        JournalEntry updated = journalRepository.save(journal);

        return toResponse(updated);
    }

    @Override
    public JournalResponse getById(Long id) {
        JournalEntry entity = getEntity(id);
        assertVisible(entity);
        JournalResponse response = toResponse(entity);
        journalRepository.findFirstByReversalOfJournalId(id).ifPresent(r -> response.setReversal(
                JournalReversalResponse.builder()
                        .journalId(r.getId())
                        .journalNumber(r.getJournalNumber())
                        .reverseDate(r.getJournalDate())
                        .reason(r.getDescription())
                        .reversedBy(r.getCreatedBy() != null ? CreatedByMapper.toDto(r.getCreatedBy()).getFullName() : null)
                        .build()));
        return response;
    }

    @Override
    public JournalResponse getByJournalNumber(String journalNumber) {

        JournalEntry journal = journalRepository
                .findByJournalNumber(journalNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Journal not found")
                );

        assertVisible(journal);

        return toResponse(journal);
    }

@Override
    public Page<JournalResponse> getAll(
            String search,
            JournalStatus status,
            JournalType journalType,
            LocalDate fromDate,
            LocalDate toDate,
            String sourceModule,
            Pageable pageable
    ) {
        return getAll(search, status, journalType, fromDate, toDate, sourceModule, null, null, pageable);
    }

    @Override
    public Page<JournalResponse> getAll(
            String search,
            JournalStatus status,
            JournalType journalType,
            LocalDate fromDate,
            LocalDate toDate,
            String sourceModule,
            List<Long> branchIds,
            List<Long> costCenterIds,
            Pageable pageable
    ) {
        AccessScope scope = accessScopeService.current();
        Specification<JournalEntry> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("deleted"), false));

            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("journalNumber")), like),
                        cb.like(cb.lower(root.get("reference")), like),
                        cb.like(cb.lower(root.get("description")), like)
                ));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (journalType != null) {
                predicates.add(cb.equal(root.get("journalType"), journalType));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("journalDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("journalDate"), toDate));
            }
            if (sourceModule != null && !sourceModule.isBlank()) {
                predicates.add(cb.equal(root.get("sourceModule"), sourceModule));
            }

            if (!scope.global()) {
                // Visible when any line falls in one of the user's cost centers.
                Set<Long> ids = scope.costCenterIds().isEmpty() ? Set.of(-1L) : scope.costCenterIds();
                predicates.add(cb.exists(lineSubquery(query, cb, root, "costCenter", ids)));
            }
            if (branchIds != null && !branchIds.isEmpty()) {
                predicates.add(cb.exists(lineSubquery(query, cb, root, "branch", branchIds)));
            }
            if (costCenterIds != null && !costCenterIds.isEmpty()) {
                predicates.add(cb.exists(lineSubquery(query, cb, root, "costCenter", costCenterIds)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return journalRepository.findAll(spec, pageable)
                .map(this::toResponse);
    }

    @Override
    public void deleteDraft(Long id) {

        JournalEntry journal = getEntity(id);

        if (journal.getStatus() != JournalStatus.DRAFT && journal.getStatus() != JournalStatus.REJECTED) {
            throw new BadRequestException(
                    "Only draft or rejected journals can be deleted"
            );
        }

        if (Boolean.TRUE.equals(journal.getManualEntry())) {
            assertLinesInScope(journal, "delete");
        }

        approvalEventRepository.deleteAll(approvalEventRepository.findByJournalEntryIdOrderByCreatedAtAsc(id));
        journalRepository.delete(journal);
    }

    @Override
    public JournalResponse post(Long id) {

        JournalEntry journal = getEntity(id);

        if (Boolean.TRUE.equals(journal.getManualEntry())) {
            assertLinesInScope(journal, "post");
        }

        postingService.post(journal);

        JournalEntry saved = journalRepository.save(journal);

        if (Boolean.TRUE.equals(journal.getManualEntry())) {
            recordEvent(saved.getId(), JournalApprovalAction.POSTED, null);
        }

        return toResponse(saved);
    }

    @Override
    public JournalResponse reverse(Long id, String reason) {
        return reverse(id, ReverseJournalRequest.builder().reason(reason).build());
    }

    @Override
    public JournalResponse reverse(Long id, ReverseJournalRequest request) {

        JournalEntry original = getEntity(id);

        if (original.getStatus() != JournalStatus.POSTED) {
            throw new BadRequestException(
                    "Only posted journals can be reversed"
            );
        }

        LocalDate reverseDate = request.getReverseDate() != null ? request.getReverseDate() : LocalDate.now();
        if (original.getJournalDate() != null && reverseDate.isBefore(original.getJournalDate())) {
            throw new BadRequestException(
                    "The reversal date can't be before the journal's own date (" + original.getJournalDate() + ")");
        }

        periodLockGuard.assertPostable(reverseDate);

        JournalEntry reversal = new JournalEntry();

        // Same numbering as create(); the legacy JournalNumberGenerator needs a "JOURNAL" row in
        // document_sequences that nothing seeds, so every reversal failed with "Journal sequence
        // not configured".
        reversal.setJournalNumber(generalSequenceGeneratorService.generateNextNumber(DocumentModule.JOURNAL));
        reversal.setCurrencyCode(original.getCurrencyCode());
        reversal.setJournalDate(reverseDate);
        reversal.setPostingDate(reverseDate);
        reversal.setPostedAt(LocalDateTime.now());

        reversal.setStatus(JournalStatus.POSTED);

        reversal.setReference(request.getReference() != null && !request.getReference().isBlank()
                ? request.getReference().trim()
                : "REV-" + original.getJournalNumber());

        reversal.setDescription(request.getReason());

        reversal.setReversalOfJournalId(original.getId());

        reversal.setJournalType(original.getJournalType());

        reversal.setBranch(original.getBranch());

        reversal.setCostCenter(original.getCostCenter());

        reversal.setManualEntry(original.getManualEntry());

        if (Boolean.TRUE.equals(original.getManualEntry())) {
            assertLinesInScope(original, "reverse");
        }

        for (JournalLine line : original.getLines()) {

            JournalLine reversalLine = new JournalLine();

            reversalLine.setAccount(line.getAccount());

            reversalLine.setLineNumber(line.getLineNumber());

            reversalLine.setDescription(
                    "Reversal of " + original.getJournalNumber()
            );

            reversalLine.setDebitAmount(line.getCreditAmount());

            reversalLine.setCreditAmount(line.getDebitAmount());

            reversalLine.setCurrencyCode(line.getCurrencyCode());

            reversalLine.setBranch(line.getBranch());

            reversalLine.setCostCenter(line.getCostCenter());

            reversalLine.setCounterpartyBranch(line.getCounterpartyBranch());

            reversalLine.setInterBranch(Boolean.TRUE.equals(line.getInterBranch()));

            reversal.addLine(reversalLine);
        }

        calculateTotals(reversal);

        journalRepository.save(reversal);

        original.setStatus(JournalStatus.REVERSED);

        original.setReversedAt(LocalDateTime.now());

        journalRepository.save(original);

        if (Boolean.TRUE.equals(original.getManualEntry())) {
            recordEvent(original.getId(), JournalApprovalAction.REVERSED, request.getReason());
        }

        return toResponse(reversal);
    }

    // ======================================================
    // Helpers
    // ======================================================

    /** Only some journal types may be entered by hand; the rest are posted by their own module. */
    private static void assertManualType(JournalType type) {
        if (type != null && !type.isManualEntry()) {
            throw new BadRequestException(
                    "Journals of type " + type + " are posted by their own module and can't be entered by hand");
        }
    }

    private JournalEntry getEntity(Long id) {
        return journalRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Journal not found")
                );
    }

    /**
     * Rejects any line in a manual journal entry (create or edit) that targets a control
     * account - see {@link JournalService#createManualJournal(CreateJournalRequest)}.
     */
    private void assertNoControlAccountLines(List<CreateJournalLineRequest> requests) {
        for (CreateJournalLineRequest request : requests) {
            AccountEntity account = accountRepository.findByAccountId(request.getAccountId().toString())
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
            if (Boolean.TRUE.equals(account.getIsControlAccount())) {
                throw new BadRequestException(
                        "\"" + account.getAccountName() + "\" (" + account.getAccountId() + ") is a control account "
                                + "and cannot be posted to from a manual journal entry - it is only ever updated by "
                                + "the transaction that owns it (an invoice, payment, payroll run, etc.)");
            }
        }
    }

    private void buildLines(
            JournalEntry journal,
            List<CreateJournalLineRequest> requests
    ) {

        int lineNumber = 1;

        Map<String, Branch> homeBranches = dimensionResolver.accountHomeBranches();

        for (CreateJournalLineRequest request : requests) {

            AccountEntity account = accountRepository.findByAccountId(
                    request.getAccountId().toString()
            ).orElseThrow(() ->
                    new ResourceNotFoundException("Account not found")
            );

            validateLine(request);

            JournalLine line = new JournalLine();

            line.setAccount(account);

            line.setLineNumber(lineNumber++);

            line.setDescription(request.getDescription());

            line.setDebitAmount(
                    defaultAmount(request.getDebitAmount())
            );

            line.setCreditAmount(
                    defaultAmount(request.getCreditAmount())
            );

            line.setCurrencyCode(journal.getCurrencyCode());

            line.setDocumentRef(JournalDocumentService.normalize(request.getDocumentRef()));

            CostCenter costCenter = dimensionResolver.lineCostCenter(
                    request.getCostCenterId(), account, journal.getCostCenter(), homeBranches);
            line.setCostCenter(costCenter);
            line.setBranch(costCenter.getBranch());

            journal.addLine(line);
        }
    }

    private void validateLine(CreateJournalLineRequest request) {

        BigDecimal debit = defaultAmount(request.getDebitAmount());

        BigDecimal credit = defaultAmount(request.getCreditAmount());

        if (debit.compareTo(BigDecimal.ZERO) > 0
                && credit.compareTo(BigDecimal.ZERO) > 0) {

            throw new BadRequestException(
                    "Line cannot contain both debit and credit"
            );
        }

        if (debit.compareTo(BigDecimal.ZERO) <= 0
                && credit.compareTo(BigDecimal.ZERO) <= 0) {

            throw new BadRequestException(
                    "Line must contain either debit or credit"
            );
        }
    }

    private void calculateTotals(JournalEntry journal) {

        BigDecimal totalDebit = BigDecimal.ZERO;

        BigDecimal totalCredit = BigDecimal.ZERO;

        for (JournalLine line : journal.getLines()) {

            totalDebit = totalDebit.add(
                    defaultAmount(line.getDebitAmount())
            );

            totalCredit = totalCredit.add(
                    defaultAmount(line.getCreditAmount())
            );
        }

        journal.setTotalDebit(totalDebit);

        journal.setTotalCredit(totalCredit);
    }

    private void validateBalanced(JournalEntry journal) {
        BigDecimal totalDebit = journal.getTotalDebit() == null ? BigDecimal.ZERO : journal.getTotalDebit();
        BigDecimal totalCredit = journal.getTotalCredit() == null ? BigDecimal.ZERO : journal.getTotalCredit();

        // System.out.println("the total debit "+ totalDebit + " total credit "+totalCredit);
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BadRequestException(
                    "Journal is not balanced: total debit must equal total credit"
            );
        }
    }


    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    // ======================================================
    // Approval workflow
    // ======================================================

    @Override
    public JournalResponse submit(Long id, String comment) {
        JournalEntry journal = getEntity(id);
        if (journal.getStatus() != JournalStatus.DRAFT) {
            throw new BadRequestException("Only draft journals can be submitted for approval");
        }
        if (!Boolean.TRUE.equals(journal.getRequiresApproval())) {
            throw new BadRequestException("This journal doesn't need approval. Post it directly.");
        }
        assertLinesInScope(journal, "submit");
        postingService.validateLines(journal);
        validateBalanced(journal);
        interBranchBalancer.assertBalancedPerBranch(journal);
        periodLockGuard.assertPostable(journal.getJournalDate());

        journal.setStatus(JournalStatus.SUBMITTED);
        journal.setSubmittedBy(SecurityUtils.getCurrentUser());
        journal.setSubmittedAt(LocalDateTime.now());
        journal.setRejectedBy(null);
        journal.setRejectedAt(null);
        journal.setRejectionReason(null);
        JournalEntry saved = journalRepository.save(journal);
        recordEvent(id, JournalApprovalAction.SUBMITTED, comment);
        return toResponse(saved);
    }

    @Override
    public JournalResponse approve(Long id, String comment) {
        JournalEntry journal = getEntity(id);
        if (journal.getStatus() != JournalStatus.SUBMITTED) {
            throw new BadRequestException("Only submitted journals can be approved");
        }
        assertSegregation(journal, "approve");
        assertLinesInScope(journal, "approve");
        periodLockGuard.assertPostable(journal.getJournalDate());

        journal.setStatus(JournalStatus.APPROVED);
        journal.setApprovedBy(SecurityUtils.getCurrentUser());
        journal.setApprovedAt(LocalDateTime.now());
        JournalEntry saved = journalRepository.save(journal);
        recordEvent(id, JournalApprovalAction.APPROVED, comment);
        return toResponse(saved);
    }

    @Override
    public JournalResponse reject(Long id, String reason) {
        JournalEntry journal = getEntity(id);
        if (journal.getStatus() != JournalStatus.SUBMITTED) {
            throw new BadRequestException("Only submitted journals can be rejected");
        }
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Give a reason so the preparer knows what to fix");
        }
        if (reason.trim().length() > 500) {
            throw new BadRequestException("The rejection reason can be at most 500 characters");
        }
        assertSegregation(journal, "reject");
        assertLinesInScope(journal, "reject");

        journal.setStatus(JournalStatus.REJECTED);
        journal.setRejectedBy(SecurityUtils.getCurrentUser());
        journal.setRejectedAt(LocalDateTime.now());
        journal.setRejectionReason(reason.trim());
        JournalEntry saved = journalRepository.save(journal);
        recordEvent(id, JournalApprovalAction.REJECTED, reason.trim());
        return toResponse(saved);
    }

    @Override
    public List<JournalApprovalEventResponse> approvalHistory(Long id) {
        assertVisible(getEntity(id));
        return approvalEventRepository.findByJournalEntryIdOrderByCreatedAtAsc(id).stream()
                .map(e -> JournalApprovalEventResponse.builder()
                        .id(e.getId())
                        .action(e.getAction())
                        .comment(e.getComment())
                        .userName(e.getCreatedBy() != null ? CreatedByMapper.toDto(e.getCreatedBy()).getFullName() : null)
                        .at(e.getCreatedAt())
                        .build())
                .toList();
    }

    private void recordEvent(Long journalId, JournalApprovalAction action, String comment) {
        JournalApprovalEvent event = new JournalApprovalEvent();
        event.setJournalEntryId(journalId);
        event.setAction(action);
        String text = comment == null || comment.isBlank() ? null : comment.trim();
        event.setComment(text == null ? null : text.substring(0, Math.min(text.length(), 500)));
        approvalEventRepository.save(event);
    }

    /** Nobody approves or rejects a journal they prepared or submitted. */
    private void assertSegregation(JournalEntry journal, String verb) {
        User me = SecurityUtils.getCurrentUser();
        if (me == null) {
            return;
        }
        boolean preparedIt = journal.getCreatedBy() != null && me.getId().equals(journal.getCreatedBy().getId());
        boolean submittedIt = journal.getSubmittedBy() != null && me.getId().equals(journal.getSubmittedBy().getId());
        if (preparedIt || submittedIt) {
            throw new BadRequestException("You can't " + verb + " a journal you prepared or submitted. "
                    + "Another approver has to review it.");
        }
    }

    private boolean needsApproval(JournalEntry journal) {
        BranchAccountingSettings settings = branchSettings.settings();
        if (!Boolean.TRUE.equals(settings.getJournalApprovalRequired())) {
            return false;
        }
        BigDecimal threshold = settings.getApprovalThreshold();
        return threshold == null || defaultAmount(journal.getTotalDebit()).compareTo(threshold) >= 0;
    }

    private void applyHeaderDimensions(JournalEntry journal, Long branchId, Long costCenterId) {
        CostCenter header = dimensionResolver.headerCostCenter(branchId, costCenterId);
        journal.setCostCenter(header);
        journal.setBranch(header.getBranch());
    }

    private static Subquery<Long> lineSubquery(jakarta.persistence.criteria.CriteriaQuery<?> query,
                                               jakarta.persistence.criteria.CriteriaBuilder cb,
                                               Root<JournalEntry> root, String dimension,
                                               java.util.Collection<Long> ids) {
        Subquery<Long> sub = query.subquery(Long.class);
        Root<JournalLine> line = sub.from(JournalLine.class);
        sub.select(line.get("id")).where(
                cb.equal(line.get("journalEntry"), root),
                line.get(dimension).get("id").in(ids));
        return sub;
    }

    /** Every line the user entered (not the system's inter-branch lines) must be in their scope. */
    private void assertLinesInScope(JournalEntry journal, String verb) {
        AccessScope scope = accessScopeService.current();
        if (scope.global()) {
            return;
        }
        List<String> outside = journal.getLines().stream()
                .filter(l -> !Boolean.TRUE.equals(l.getInterBranch()))
                .map(JournalLine::getCostCenter)
                .filter(Objects::nonNull)
                .filter(cc -> !scope.coversCostCenter(cc.getId()))
                .map(cc -> cc.getCode() + " (" + cc.getName() + ")")
                .distinct()
                .toList();
        if (!outside.isEmpty()) {
            throw AccessDeniedException.outOfScope("You can't " + verb + " this journal: it has lines in "
                    + String.join(", ", outside) + ", which are outside your branch / cost center access.");
        }
    }

    private void assertVisible(JournalEntry journal) {
        AccessScope scope = accessScopeService.current();
        if (scope.global()) {
            return;
        }
        boolean visible = journal.getLines().stream()
                .map(JournalLine::getCostCenter)
                .filter(Objects::nonNull)
                .anyMatch(cc -> scope.coversCostCenter(cc.getId()));
        if (!visible) {
            throw new ResourceNotFoundException("Journal not found");
        }
    }

    private JournalResponse toResponse(JournalEntry journal) {
        JournalResponse response = journalMapper.toResponse(journal);
        if (response.getLines() != null && response.getLines().stream().anyMatch(l -> l.getDocumentRef() != null)) {
            Map<String, com.unionsg.xaccounting.entity.FileEntity> docs = journalDocumentService.documentsOf(journal);
            response.getLines().forEach(l -> {
                com.unionsg.xaccounting.entity.FileEntity f = l.getDocumentRef() == null ? null : docs.get(l.getDocumentRef());
                if (f != null) {
                    l.setDocumentFileId(f.getId());
                    l.setDocumentName(f.getOriginalName());
                }
            });
        }
        Map<Long, BigDecimal> net = journal.getLines().stream()
                .filter(l -> l.getBranch() != null)
                .collect(Collectors.groupingBy(l -> l.getBranch().getId(), Collectors.reducing(BigDecimal.ZERO,
                        l -> defaultAmount(l.getDebitAmount()).subtract(defaultAmount(l.getCreditAmount())), BigDecimal::add)));
        response.setBalancedPerBranch(net.values().stream().allMatch(v -> v.signum() == 0));

        User me = SecurityUtils.getCurrentUser();
        boolean mine = me != null && ((journal.getCreatedBy() != null && me.getId().equals(journal.getCreatedBy().getId()))
                || (journal.getSubmittedBy() != null && me.getId().equals(journal.getSubmittedBy().getId())));
        boolean approval = Boolean.TRUE.equals(journal.getRequiresApproval());
        JournalStatus status = journal.getStatus();
        response.setCanSubmit(approval && status == JournalStatus.DRAFT);
        response.setCanApprove(status == JournalStatus.SUBMITTED && !mine
                && (me == null || permissionService.currentUserHasPermission("approve_journals")));
        response.setCanPost(status == JournalStatus.APPROVED || (status == JournalStatus.DRAFT && !approval));
        return response;
    }


    private void JournalMapper(){}
}