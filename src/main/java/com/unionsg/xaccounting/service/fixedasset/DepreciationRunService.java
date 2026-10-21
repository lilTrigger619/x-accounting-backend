package com.unionsg.xaccounting.service.fixedasset;

import com.unionsg.xaccounting.MapperLayer.FixedAssetMapper;
import com.unionsg.xaccounting.dto.fixedasset.CreateDepreciationRunRequest;
import com.unionsg.xaccounting.dto.fixedasset.DepreciationRunActionRequest;
import com.unionsg.xaccounting.dto.fixedasset.DepreciationRunListItemResponse;
import com.unionsg.xaccounting.dto.fixedasset.DepreciationRunResponse;
import com.unionsg.xaccounting.dto.fixedasset.JournalPreviewLine;
import com.unionsg.xaccounting.dto.fixedasset.PeriodDepreciationStatusResponse;
import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.entity.accounting.AccountingPeriod;
import com.unionsg.xaccounting.entity.fixedasset.AssetTransaction;
import com.unionsg.xaccounting.entity.fixedasset.DepreciationRun;
import com.unionsg.xaccounting.entity.fixedasset.DepreciationRunLine;
import com.unionsg.xaccounting.entity.fixedasset.FixedAsset;
import com.unionsg.xaccounting.enums.DocumentModule;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.enums.fixedasset.AssetTransactionType;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationLineStatus;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationMethod;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationRunStatus;
import com.unionsg.xaccounting.enums.fixedasset.FixedAssetAuditAction;
import com.unionsg.xaccounting.enums.fixedasset.FixedAssetStatus;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.accounting.AccountingPeriodRepository;
import com.unionsg.xaccounting.repository.fixedasset.AssetTransactionRepository;
import com.unionsg.xaccounting.repository.fixedasset.DepreciationRunRepository;
import com.unionsg.xaccounting.repository.fixedasset.FixedAssetLedgerRepository;
import com.unionsg.xaccounting.repository.fixedasset.FixedAssetRepository;
import com.unionsg.xaccounting.security.util.SecurityUtils;
import com.unionsg.xaccounting.service.DocumentNumberService;
import com.unionsg.xaccounting.service.accounting.PeriodLockGuard;
import com.unionsg.xaccounting.service.fixedasset.FixedAssetAccountRules.Role;
import com.unionsg.xaccounting.service.fixedasset.depreciation.DepreciationEngine;
import com.unionsg.xaccounting.service.fixedasset.depreciation.DepreciationResult;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Period depreciation: one run per accounting period, moving Calculated → Submitted → Approved →
 * Posted. Every step before posting can recalculate; posting recalculates once more and refuses if
 * anything changed, then posts one journal (a Dr expense / Cr accumulated depreciation pair per
 * asset) through the journal service and writes each asset's subledger. A posted run can be
 * reversed as a whole while none of its assets has a later posting.
 */
@Service
@RequiredArgsConstructor
public class DepreciationRunService {

    private static final Set<DepreciationRunStatus> RECALCULABLE = EnumSet.of(
            DepreciationRunStatus.CALCULATED, DepreciationRunStatus.SUBMITTED, DepreciationRunStatus.APPROVED);
    private static final List<JournalStatus> LEDGER = List.of(JournalStatus.POSTED, JournalStatus.REVERSED);
    static final List<String> MODULE_SOURCES = List.of(FixedAssetJournalService.SOURCE_ASSET, FixedAssetJournalService.SOURCE_RUN);

    private final DepreciationRunRepository repository;
    private final FixedAssetRepository assetRepository;
    private final AssetTransactionRepository transactionRepository;
    private final AccountingPeriodRepository periodRepository;
    private final FixedAssetLedgerRepository ledgerRepository;
    private final DocumentNumberService documentNumberService;
    private final PeriodLockGuard periodLockGuard;
    private final FixedAssetAccountRules rules;
    private final FixedAssetJournalService journalService;
    private final FixedAssetAuditService auditService;
    private final FixedAssetService assetService;
    private final DepreciationEngine engine;

    public record RunSearchCriteria(DepreciationRunStatus status, LocalDate from, LocalDate to) {
    }

    // ------------------------------------------------------------------
    // Calculation
    // ------------------------------------------------------------------

    @Transactional
    public DepreciationRunResponse calculate(CreateDepreciationRunRequest request) {
        if (request.getAccountingPeriodId() == null) {
            throw new BusinessException("Pick the accounting period to depreciate");
        }
        AccountingPeriod period = periodRepository.findById(request.getAccountingPeriodId())
                .orElseThrow(() -> new ResourceNotFoundException("Accounting period not found"));
        repository.findByActivePeriodId(period.getId()).ifPresent(existing -> {
            throw new BusinessException(period.getName() + " already has depreciation run " + existing.getRunNumber()
                    + " (" + existing.getStatus().getLabel().toLowerCase() + "). "
                    + (existing.getStatus() == DepreciationRunStatus.POSTED
                    ? "Reverse it before running the period again."
                    : "Recalculate or cancel that run instead."));
        });
        periodLockGuard.assertPostable(period.getEndDate());

        DepreciationRun run = new DepreciationRun();
        run.setRunNumber(documentNumberService.generateNextNumber(DocumentModule.DEPRECIATION_RUN));
        run.setAccountingPeriod(period);
        run.setActivePeriodId(period.getId());
        run.setPeriodStart(period.getStartDate());
        run.setPeriodEnd(period.getEndDate());
        run.setPostingDate(period.getEndDate());
        run.setNotes(blankToNull(request.getNotes()));
        run.setStatus(DepreciationRunStatus.CALCULATED);
        buildLines(run);
        DepreciationRun saved = repository.save(run);
        auditService.recordRun(saved.getId(), FixedAssetAuditAction.RUN_CALCULATED, null, saved.getStatus(),
                summary(saved));
        return toResponse(saved);
    }

    /** Throws away the run's lines and works them out again from the assets as they are now. */
    @Transactional
    public DepreciationRunResponse recalculate(Long id) {
        DepreciationRun run = getEntity(id);
        if (!RECALCULABLE.contains(run.getStatus())) {
            throw new BusinessException("Run " + run.getRunNumber() + " is " + run.getStatus().getLabel().toLowerCase()
                    + " and can't be recalculated");
        }
        periodLockGuard.assertPostable(run.getPostingDate());
        DepreciationRunStatus before = run.getStatus();
        run.getLines().clear();
        repository.saveAndFlush(run);
        buildLines(run);
        run.setStatus(DepreciationRunStatus.CALCULATED);
        run.setSubmittedAt(null);
        run.setSubmittedBy(null);
        run.setApprovedAt(null);
        run.setApprovedBy(null);
        run.setStatusReason(null);
        DepreciationRun saved = repository.save(run);
        auditService.recordRun(id, FixedAssetAuditAction.RUN_RECALCULATED, before, saved.getStatus(), summary(saved));
        return toResponse(saved);
    }

    private void buildLines(DepreciationRun run) {
        BigDecimal total = BigDecimal.ZERO;
        int charged = 0, skipped = 0, errors = 0;
        for (FixedAsset asset : assetRepository.findDepreciable(run.getPeriodEnd())) {
            DepreciationRunLine line = lineFor(run, asset);
            run.getLines().add(line);
            switch (line.getStatus()) {
                case CALCULATED -> {
                    charged++;
                    total = total.add(line.getDepreciationAmount());
                }
                case ERROR -> errors++;
                default -> skipped++;
            }
        }
        run.setTotalDepreciation(total);
        run.setAssetCount(charged);
        run.setSkippedCount(skipped);
        run.setErrorCount(errors);
        run.setCalculatedAt(LocalDateTime.now());
        run.setCalculatedBy(SecurityUtils.getCurrentUser());
    }

    /** The charge for one asset from the day after its last posted depreciation to the period end. */
    DepreciationRunLine lineFor(DepreciationRun run, FixedAsset asset) {
        DepreciationRunLine line = new DepreciationRunLine();
        line.setRun(run);
        line.setAsset(asset);
        line.setMethod(asset.getDepreciationMethod());
        line.setConvention(asset.getDepreciationConvention());
        line.setOpeningCost(asset.getCostBalance());
        line.setOpeningAccumulatedDepreciation(asset.getAccumulatedDepreciation());
        line.setOpeningAccumulatedImpairment(asset.getAccumulatedImpairment());
        line.setOpeningCarryingAmount(asset.getCarryingAmount());
        line.setClosingAccumulatedDepreciation(asset.getAccumulatedDepreciation());
        line.setClosingCarryingAmount(asset.getCarryingAmount());
        line.setResidualValue(asset.getResidualValue());
        line.setUsefulLifeMonths(asset.getUsefulLifeMonths());
        line.setAnnualRate(asset.getAnnualRate());
        line.setAssetVersion(asset.getVersion());

        LocalDate from = windowStart(asset);
        line.setFromDate(from);
        line.setToDate(run.getPeriodEnd());

        if (asset.getDepreciationMethod() == DepreciationMethod.NOT_DEPRECIATED) {
            return skip(line, "Not depreciated (" + asset.getDepreciationMethod().getLabel() + ")");
        }
        if (from.isAfter(run.getPeriodEnd())) {
            line.setToDate(asset.getDepreciatedThrough());
            return skip(line, "Already depreciated through " + asset.getDepreciatedThrough());
        }
        for (AssetTransaction t : transactionRepository.findLiveNewestFirst(asset.getId())) {
            if (t.getTransactionDate().isAfter(run.getPeriodEnd()) && t.getType() != AssetTransactionType.ACQUISITION) {
                return error(line, "Has a " + t.getType().getLabel().toLowerCase() + " dated " + t.getTransactionDate()
                        + ", after this period. Depreciate the earlier period before posting later movements.");
            }
            break;
        }
        try {
            line.setExpenseAccount(rules.resolve(asset, Role.DEPRECIATION_EXPENSE));
            line.setAccumulatedDepreciationAccount(rules.validate(asset.getAccumulatedDepreciationAccount(), Role.ACCUMULATED_DEPRECIATION));
        } catch (BusinessException e) {
            return error(line, e.getMessage());
        }
        DepreciationResult r;
        try {
            r = engine.calculate(assetService.inputFor(asset, from, run.getPeriodEnd(), null));
        } catch (IllegalArgumentException e) {
            return error(line, e.getMessage());
        }
        line.setMonthsCharged(r.monthsCharged());
        line.setRemainingLifeMonths(r.remainingLifeMonths());
        if (r.annualRatePercent() != null) line.setAnnualRate(r.annualRatePercent());
        String explanation = (from.isBefore(run.getPeriodStart())
                ? "Catch-up from " + from + " (depreciation was last posted through "
                + (asset.getDepreciatedThrough() != null ? asset.getDepreciatedThrough() : "never") + "). " : "")
                + r.explanation();
        line.setExplanation(truncate(explanation));
        if (r.charge().signum() == 0) {
            line.setStatus(DepreciationLineStatus.SKIPPED);
            line.setMessage(truncate(r.explanation()));
            return line;
        }
        line.setDepreciationAmount(r.charge());
        line.setClosingAccumulatedDepreciation(asset.getAccumulatedDepreciation().add(r.charge()));
        line.setClosingCarryingAmount(asset.getCarryingAmount().subtract(r.charge()));
        line.setStatus(DepreciationLineStatus.CALCULATED);
        return line;
    }

    private static LocalDate windowStart(FixedAsset asset) {
        LocalDate start = asset.getEffectiveDepreciationStart();
        LocalDate next = asset.getDepreciatedThrough() != null ? asset.getDepreciatedThrough().plusDays(1) : start;
        return next.isBefore(start) ? start : next;
    }

    // ------------------------------------------------------------------
    // Workflow
    // ------------------------------------------------------------------

    @Transactional
    public DepreciationRunResponse submit(Long id) {
        DepreciationRun run = getEntity(id);
        requireStatus(run, DepreciationRunStatus.CALCULATED, "submitted");
        if (run.getErrorCount() > 0) {
            throw new BusinessException(run.getErrorCount() + " asset(s) in this run have errors. Fix them and recalculate before submitting.");
        }
        if (run.getTotalDepreciation().signum() == 0) {
            throw new BusinessException("There is no depreciation to post in this run");
        }
        run.setStatus(DepreciationRunStatus.SUBMITTED);
        run.setSubmittedAt(LocalDateTime.now());
        run.setSubmittedBy(SecurityUtils.getCurrentUser());
        run.setStatusReason(null);
        DepreciationRun saved = repository.save(run);
        auditService.recordRun(id, FixedAssetAuditAction.RUN_SUBMITTED, DepreciationRunStatus.CALCULATED, saved.getStatus(), summary(saved));
        return toResponse(saved);
    }

    @Transactional
    public DepreciationRunResponse approve(Long id) {
        DepreciationRun run = getEntity(id);
        requireStatus(run, DepreciationRunStatus.SUBMITTED, "approved");
        run.setStatus(DepreciationRunStatus.APPROVED);
        run.setApprovedAt(LocalDateTime.now());
        run.setApprovedBy(SecurityUtils.getCurrentUser());
        DepreciationRun saved = repository.save(run);
        auditService.recordRun(id, FixedAssetAuditAction.RUN_APPROVED, DepreciationRunStatus.SUBMITTED, saved.getStatus(), summary(saved));
        return toResponse(saved);
    }

    /** Sends a submitted or approved run back to Calculated with a reason. */
    @Transactional
    public DepreciationRunResponse reject(Long id, DepreciationRunActionRequest request) {
        DepreciationRun run = getEntity(id);
        if (run.getStatus() != DepreciationRunStatus.SUBMITTED && run.getStatus() != DepreciationRunStatus.APPROVED) {
            throw new BusinessException("Only a submitted or approved run can be sent back");
        }
        String reason = requireReason(request);
        DepreciationRunStatus before = run.getStatus();
        run.setStatus(DepreciationRunStatus.CALCULATED);
        run.setStatusReason(reason);
        run.setSubmittedAt(null);
        run.setSubmittedBy(null);
        run.setApprovedAt(null);
        run.setApprovedBy(null);
        DepreciationRun saved = repository.save(run);
        auditService.recordRun(id, FixedAssetAuditAction.RUN_REJECTED, before, saved.getStatus(), reason);
        return toResponse(saved);
    }

    @Transactional
    public DepreciationRunResponse cancel(Long id, DepreciationRunActionRequest request) {
        DepreciationRun run = getEntity(id);
        if (!RECALCULABLE.contains(run.getStatus())) {
            throw new BusinessException("Only a run that is not posted yet can be cancelled");
        }
        String reason = requireReason(request);
        DepreciationRunStatus before = run.getStatus();
        run.setStatus(DepreciationRunStatus.CANCELLED);
        run.setActivePeriodId(null);
        run.setStatusReason(reason);
        run.setCancelledAt(LocalDateTime.now());
        DepreciationRun saved = repository.save(run);
        auditService.recordRun(id, FixedAssetAuditAction.RUN_CANCELLED, before, saved.getStatus(), reason);
        return toResponse(saved);
    }

    /**
     * Posts an approved run. Each line is recalculated first; if any asset changed since the run was
     * calculated (a new posting, an estimate change, a disposal), nothing is posted and the run
     * must be recalculated.
     */
    @Transactional
    public DepreciationRunResponse post(Long id) {
        DepreciationRun run = getEntity(id);
        requireStatus(run, DepreciationRunStatus.APPROVED, "posted");
        periodLockGuard.assertPostable(run.getPostingDate());

        List<String> stale = new ArrayList<>();
        List<DepreciationRunLine> toPost = new ArrayList<>();
        for (DepreciationRunLine line : run.getLines()) {
            if (line.getStatus() == DepreciationLineStatus.ERROR) {
                stale.add(line.getAsset().getAssetNumber() + ": " + line.getMessage());
                continue;
            }
            if (line.getStatus() != DepreciationLineStatus.CALCULATED) continue;
            FixedAsset asset = line.getAsset();
            if (asset.getStatus() != FixedAssetStatus.ACTIVE || Boolean.TRUE.equals(asset.getDeleted())) {
                stale.add(asset.getAssetNumber() + " is now " + asset.getStatus().getLabel().toLowerCase());
                continue;
            }
            DepreciationRunLine fresh = lineFor(run, asset);
            if (fresh.getStatus() != DepreciationLineStatus.CALCULATED
                    || !fresh.getFromDate().equals(line.getFromDate())
                    || fresh.getDepreciationAmount().compareTo(line.getDepreciationAmount()) != 0
                    || !sameAccount(fresh.getExpenseAccount(), line.getExpenseAccount())
                    || !sameAccount(fresh.getAccumulatedDepreciationAccount(), line.getAccumulatedDepreciationAccount())) {
                stale.add(asset.getAssetNumber() + " changed since the run was calculated");
                continue;
            }
            toPost.add(line);
        }
        if (!stale.isEmpty()) {
            throw new BusinessException("Run " + run.getRunNumber() + " is out of date. Recalculate it before posting. "
                    + String.join("; ", stale.subList(0, Math.min(5, stale.size())))
                    + (stale.size() > 5 ? " and " + (stale.size() - 5) + " more" : ""));
        }
        if (toPost.isEmpty()) {
            throw new BusinessException("There is no depreciation to post in this run");
        }

        JournalEntry journal = journalService.post(JournalType.DEPRECIATION, run.getPostingDate(), run.getRunNumber(),
                "Depreciation for " + run.getAccountingPeriod().getName() + " (run " + run.getRunNumber() + ").",
                journalLines(toPost), FixedAssetJournalService.SOURCE_RUN, run.getId());

        BigDecimal total = BigDecimal.ZERO;
        for (DepreciationRunLine line : toPost) {
            FixedAsset asset = line.getAsset();
            AssetTransaction tx = new AssetTransaction();
            tx.setType(AssetTransactionType.DEPRECIATION);
            tx.setTransactionDate(line.getToDate());
            tx.setDepreciationAmount(line.getDepreciationAmount());
            tx.setJournal(journal);
            tx.setRunLine(line);
            tx.setPreviousDepreciatedThrough(asset.getDepreciatedThrough());
            tx.setDescription("Depreciation " + line.getFromDate() + " to " + line.getToDate() + " (run " + run.getRunNumber() + ")");
            assetService.record(asset, tx);
            asset.setDepreciatedThrough(line.getToDate());
            FixedAssetStatus before = asset.getStatus();
            assetService.refreshStatus(asset);
            assetRepository.save(asset);
            line.setStatus(DepreciationLineStatus.POSTED);
            total = total.add(line.getDepreciationAmount());
            auditService.record(asset.getId(), run.getId(), FixedAssetAuditAction.DEPRECIATED, before, asset.getStatus(),
                    "Depreciation of " + line.getDepreciationAmount() + " for " + line.getFromDate() + " to " + line.getToDate()
                            + " in journal " + journal.getJournalNumber());
        }
        if (total.compareTo(journal.getTotalDebit()) != 0) {
            throw new BusinessException("Journal " + journal.getJournalNumber() + " total " + journal.getTotalDebit()
                    + " does not match the run total " + total + ". Nothing was posted.");
        }

        run.setStatus(DepreciationRunStatus.POSTED);
        run.setJournal(journal);
        run.setTotalDepreciation(total);
        run.setPostedAt(LocalDateTime.now());
        run.setPostedBy(SecurityUtils.getCurrentUser());
        DepreciationRun saved = repository.save(run);
        auditService.recordRun(id, FixedAssetAuditAction.RUN_POSTED, DepreciationRunStatus.APPROVED, saved.getStatus(),
                "Posted " + total + " for " + toPost.size() + " asset(s) in journal " + journal.getJournalNumber());
        return toResponse(saved);
    }

    /**
     * Reverses a posted run with a mirror journal and returns every asset to where it stood before
     * the run. Refused while any of its assets has a later posting, so history unwinds newest first.
     */
    @Transactional
    public DepreciationRunResponse reverse(Long id, DepreciationRunActionRequest request) {
        DepreciationRun run = getEntity(id);
        requireStatus(run, DepreciationRunStatus.POSTED, "reversed");
        String reason = requireReason(request);
        JournalEntry journal = run.getJournal();
        if (journal == null || journal.getStatus() != JournalStatus.POSTED) {
            throw new BusinessException("The run's journal is not posted and can't be reversed");
        }

        List<AssetTransaction> originals = new ArrayList<>();
        for (DepreciationRunLine line : run.getLines()) {
            if (line.getStatus() != DepreciationLineStatus.POSTED) continue;
            AssetTransaction original = transactionRepository.findByRunLineId(line.getId()).stream()
                    .filter(t -> t.getType() == AssetTransactionType.DEPRECIATION && !t.isReversed())
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("No posted depreciation found for " + line.getAsset().getAssetNumber()));
            List<AssetTransaction> live = transactionRepository.findLiveNewestFirst(line.getAsset().getId());
            if (live.isEmpty() || !live.get(0).getId().equals(original.getId())) {
                AssetTransaction newest = live.isEmpty() ? null : live.get(0);
                throw new BusinessException(line.getAsset().getAssetNumber() + " has a later posting"
                        + (newest != null ? " (" + newest.getType().getLabel().toLowerCase() + " of " + newest.getTransactionDate()
                        + (newest.getRunLine() != null ? ", run " + newest.getRunLine().getRun().getRunNumber() : "") + ")" : "")
                        + ". Reverse that first.");
            }
            originals.add(original);
        }

        LocalDate date = journalService.defaultReversalDate(run.getPostingDate(), request.getReverseDate());
        if (date.isBefore(run.getPostingDate())) {
            throw new BusinessException("The reversal date can't be before the run's posting date " + run.getPostingDate());
        }
        JournalEntry reversal = journalService.reverse(journal, reason, date);

        for (AssetTransaction original : originals) {
            FixedAsset asset = original.getAsset();
            AssetTransaction rev = new AssetTransaction();
            rev.setType(AssetTransactionType.REVERSAL);
            rev.setMovementType(AssetTransactionType.DEPRECIATION);
            rev.setTransactionDate(date);
            rev.setDepreciationAmount(original.getDepreciationAmount().negate());
            rev.setJournal(reversal);
            rev.setReverses(original);
            rev.setDescription("Reversal of run " + run.getRunNumber() + ": " + reason);
            assetService.record(asset, rev);
            original.setReversedAt(LocalDateTime.now());
            transactionRepository.save(original);
            FixedAssetStatus before = asset.getStatus();
            asset.setDepreciatedThrough(original.getPreviousDepreciatedThrough());
            if (asset.getStatus() == FixedAssetStatus.FULLY_DEPRECIATED) asset.setStatus(FixedAssetStatus.ACTIVE);
            assetService.refreshStatus(asset);
            assetRepository.save(asset);
            original.getRunLine().setStatus(DepreciationLineStatus.REVERSED);
            auditService.record(asset.getId(), run.getId(), FixedAssetAuditAction.TRANSACTION_REVERSED, before, asset.getStatus(),
                    "Depreciation of " + original.getDepreciationAmount() + " reversed with run " + run.getRunNumber()
                            + " in journal " + reversal.getJournalNumber() + ". " + reason);
        }

        run.setStatus(DepreciationRunStatus.REVERSED);
        run.setReversalJournal(reversal);
        run.setReversedAt(LocalDateTime.now());
        run.setActivePeriodId(null);
        run.setStatusReason(reason);
        DepreciationRun saved = repository.save(run);
        auditService.recordRun(id, FixedAssetAuditAction.RUN_REVERSED, DepreciationRunStatus.POSTED, saved.getStatus(),
                "Reversed in journal " + reversal.getJournalNumber() + ". " + reason);
        return toResponse(saved);
    }

    // ------------------------------------------------------------------
    // Enquiry
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public DepreciationRunResponse get(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<DepreciationRunListItemResponse> list(RunSearchCriteria criteria, Pageable pageable) {
        return repository.findAll(spec(criteria), pageable).map(FixedAssetMapper::toRunListItem);
    }

    /** Every accounting period with its depreciation state, newest first, for the run picker. */
    @Transactional(readOnly = true)
    public List<PeriodDepreciationStatusResponse> periods() {
        List<PeriodDepreciationStatusResponse> out = new ArrayList<>();
        List<AccountingPeriod> periods = new ArrayList<>(periodRepository.findAll());
        periods.removeIf(p -> Boolean.TRUE.equals(p.getDeleted()));
        periods.sort(Comparator.comparing(AccountingPeriod::getStartDate).reversed());
        for (AccountingPeriod p : periods) {
            PeriodDepreciationStatusResponse r = new PeriodDepreciationStatusResponse();
            r.setPeriodId(p.getId());
            r.setPeriodName(p.getName());
            if (p.getFinancialYear() != null) {
                r.setFinancialYearId(p.getFinancialYear().getId());
                r.setFinancialYearName(p.getFinancialYear().getName());
            }
            r.setStartDate(p.getStartDate());
            r.setEndDate(p.getEndDate());
            r.setPeriodStatus(p.getStatus().name());
            r.setPostable(p.isPostable());
            DepreciationRun run = repository.findByActivePeriodId(p.getId())
                    .orElseGet(() -> repository.findByAccountingPeriodIdOrderByIdDesc(p.getId()).stream().findFirst().orElse(null));
            if (run != null) {
                r.setRunId(run.getId());
                r.setRunNumber(run.getRunNumber());
                r.setRunStatus(run.getStatus());
                r.setTotalDepreciation(run.getTotalDepreciation());
            }
            out.add(r);
        }
        return out;
    }

    public DepreciationRun getEntity(Long id) {
        return repository.findById(id)
                .filter(r -> !Boolean.TRUE.equals(r.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Depreciation run not found with ID: " + id));
    }

    DepreciationRunResponse toResponse(DepreciationRun run) {
        DepreciationRunResponse r = new DepreciationRunResponse();
        r.setId(run.getId());
        r.setRunNumber(run.getRunNumber());
        AccountingPeriod period = run.getAccountingPeriod();
        r.setAccountingPeriodId(period.getId());
        r.setPeriodName(period.getName());
        r.setPeriodStatus(period.getStatus().name());
        r.setPeriodStart(run.getPeriodStart());
        r.setPeriodEnd(run.getPeriodEnd());
        r.setPostingDate(run.getPostingDate());
        r.setStatus(run.getStatus());
        r.setTotalDepreciation(run.getTotalDepreciation());
        r.setAssetCount(run.getAssetCount());
        r.setSkippedCount(run.getSkippedCount());
        r.setErrorCount(run.getErrorCount());
        if (run.getJournal() != null) {
            r.setJournalId(run.getJournal().getId());
            r.setJournalNumber(run.getJournal().getJournalNumber());
        }
        if (run.getReversalJournal() != null) {
            r.setReversalJournalId(run.getReversalJournal().getId());
            r.setReversalJournalNumber(run.getReversalJournal().getJournalNumber());
        }
        r.setNotes(run.getNotes());
        r.setStatusReason(run.getStatusReason());
        r.setCalculatedAt(run.getCalculatedAt());
        r.setCalculatedBy(FixedAssetMapper.userName(run.getCalculatedBy()));
        r.setSubmittedAt(run.getSubmittedAt());
        r.setSubmittedBy(FixedAssetMapper.userName(run.getSubmittedBy()));
        r.setApprovedAt(run.getApprovedAt());
        r.setApprovedBy(FixedAssetMapper.userName(run.getApprovedBy()));
        r.setPostedAt(run.getPostedAt());
        r.setPostedBy(FixedAssetMapper.userName(run.getPostedBy()));
        r.setCancelledAt(run.getCancelledAt());
        r.setReversedAt(run.getReversedAt());
        r.setLines(run.getLines().stream().map(FixedAssetMapper::toLine).toList());
        List<DepreciationRunLine> chargeable = run.getLines().stream()
                .filter(l -> l.getStatus() == DepreciationLineStatus.CALCULATED || l.getStatus() == DepreciationLineStatus.POSTED
                        || l.getStatus() == DepreciationLineStatus.REVERSED)
                .toList();
        r.setJournalPreview(journalLines(chargeable).stream().map(FixedAssetService::previewLine).toList());
        r.setWarnings(warnings(run));
        return r;
    }

    private List<String> warnings(DepreciationRun run) {
        List<String> out = new ArrayList<>();
        if (run.getErrorCount() > 0) {
            out.add(run.getErrorCount() + " asset(s) have errors and are left out. Fix them and recalculate.");
        }
        long catchUps = run.getLines().stream()
                .filter(l -> (l.getStatus() == DepreciationLineStatus.CALCULATED || l.getStatus() == DepreciationLineStatus.POSTED)
                        && l.getFromDate().isBefore(run.getPeriodStart()))
                .count();
        if (catchUps > 0) {
            out.add(catchUps + " asset(s) include catch-up depreciation for days before this period.");
        }
        if (RECALCULABLE.contains(run.getStatus()) && !run.getAccountingPeriod().isPostable()) {
            out.add(run.getAccountingPeriod().getName() + " is " + run.getAccountingPeriod().getStatus().name().toLowerCase()
                    + "; the run can't be posted until it is reopened.");
        }
        Set<Long> accDepAccounts = new LinkedHashSet<>();
        for (DepreciationRunLine l : run.getLines()) {
            if (l.getAccumulatedDepreciationAccount() != null) accDepAccounts.add(l.getAccumulatedDepreciationAccount().getId());
        }
        if (!accDepAccounts.isEmpty()) {
            List<JournalLine> outside = ledgerRepository.outsideLines(accDepAccounts, run.getPeriodStart(), run.getPeriodEnd(),
                    LEDGER, MODULE_SOURCES);
            Set<String> journals = new LinkedHashSet<>();
            BigDecimal credited = BigDecimal.ZERO;
            for (JournalLine l : outside) {
                journals.add(l.getJournalEntry().getJournalNumber());
                credited = credited.add(l.getCreditAmount()).subtract(l.getDebitAmount());
            }
            if (!journals.isEmpty()) {
                out.add("Journals posted outside the fixed asset module also move accumulated depreciation in this period ("
                        + String.join(", ", journals) + ", net credit " + credited
                        + "). If they record depreciation, it is being counted twice.");
            }
        }
        return out;
    }

    private static List<CreateJournalLineRequest> journalLines(List<DepreciationRunLine> lines) {
        List<CreateJournalLineRequest> out = new ArrayList<>();
        for (DepreciationRunLine l : lines) {
            if (l.getExpenseAccount() == null || l.getAccumulatedDepreciationAccount() == null) continue;
            String text = "Depreciation " + l.getAsset().getAssetNumber() + " " + l.getFromDate() + " to " + l.getToDate();
            out.add(FixedAssetJournalService.debit(l.getExpenseAccount(), text, l.getDepreciationAmount()));
            out.add(FixedAssetJournalService.credit(l.getAccumulatedDepreciationAccount(), text, l.getDepreciationAmount()));
        }
        return out;
    }

    private static Specification<DepreciationRun> spec(RunSearchCriteria c) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.isFalse(root.get("deleted")));
            if (c.status() != null) p.add(cb.equal(root.get("status"), c.status()));
            if (c.from() != null) p.add(cb.greaterThanOrEqualTo(root.get("periodEnd"), c.from()));
            if (c.to() != null) p.add(cb.lessThanOrEqualTo(root.get("periodStart"), c.to()));
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    private static DepreciationRunLine skip(DepreciationRunLine line, String why) {
        line.setStatus(DepreciationLineStatus.SKIPPED);
        line.setMessage(truncate(why));
        line.setExplanation(truncate(why));
        return line;
    }

    private static DepreciationRunLine error(DepreciationRunLine line, String why) {
        line.setStatus(DepreciationLineStatus.ERROR);
        line.setMessage(truncate(why));
        return line;
    }

    private static void requireStatus(DepreciationRun run, DepreciationRunStatus status, String action) {
        if (run.getStatus() != status) {
            throw new BusinessException("Run " + run.getRunNumber() + " is " + run.getStatus().getLabel().toLowerCase()
                    + ". Only runs that are " + status.getLabel().toLowerCase() + " can be " + action + ".");
        }
    }

    private static String requireReason(DepreciationRunActionRequest request) {
        String reason = request != null ? blankToNull(request.getReason()) : null;
        if (reason == null) throw new BusinessException("A reason is required");
        return reason.length() > 1000 ? reason.substring(0, 1000) : reason;
    }

    private static boolean sameAccount(AccountEntity a, AccountEntity b) {
        return a != null && b != null && Objects.equals(a.getId(), b.getId());
    }

    private static String summary(DepreciationRun run) {
        return run.getAccountingPeriod().getName() + ": " + run.getAssetCount() + " asset(s), total " + run.getTotalDepreciation()
                + (run.getErrorCount() > 0 ? ", " + run.getErrorCount() + " error(s)" : "")
                + (run.getSkippedCount() > 0 ? ", " + run.getSkippedCount() + " skipped" : "");
    }

    private static String truncate(String s) {
        return s == null ? null : s.length() > 2000 ? s.substring(0, 2000) : s;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
