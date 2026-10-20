package com.unionsg.xaccounting.service.fixedasset;

import com.unionsg.xaccounting.MapperLayer.FixedAssetMapper;
import com.unionsg.xaccounting.dto.fixedasset.AccountRoleResponse;
import com.unionsg.xaccounting.dto.fixedasset.AssetActionRequest;
import com.unionsg.xaccounting.dto.fixedasset.AssetEstimateChangeResponse;
import com.unionsg.xaccounting.dto.fixedasset.AssetTransactionResponse;
import com.unionsg.xaccounting.dto.fixedasset.ChangeEstimateRequest;
import com.unionsg.xaccounting.dto.fixedasset.DisposalPreviewResponse;
import com.unionsg.xaccounting.dto.fixedasset.DisposeAssetRequest;
import com.unionsg.xaccounting.dto.fixedasset.FixedAssetAuditLogResponse;
import com.unionsg.xaccounting.dto.fixedasset.FixedAssetListItemResponse;
import com.unionsg.xaccounting.dto.fixedasset.FixedAssetResponse;
import com.unionsg.xaccounting.dto.fixedasset.ImpairAssetRequest;
import com.unionsg.xaccounting.dto.fixedasset.JournalPreviewLine;
import com.unionsg.xaccounting.dto.fixedasset.SaveFixedAssetRequest;
import com.unionsg.xaccounting.dto.fixedasset.ScheduleLineResponse;
import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.entity.fixedasset.AssetCategory;
import com.unionsg.xaccounting.entity.fixedasset.AssetEstimateChange;
import com.unionsg.xaccounting.entity.fixedasset.AssetTransaction;
import com.unionsg.xaccounting.entity.fixedasset.DepreciationRunLine;
import com.unionsg.xaccounting.entity.fixedasset.FixedAsset;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.entity.supplier.Supplier;
import com.unionsg.xaccounting.enums.DocumentModule;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.enums.fixedasset.AssetAcquisitionSource;
import com.unionsg.xaccounting.enums.fixedasset.AssetDisposalType;
import com.unionsg.xaccounting.enums.fixedasset.AssetTransactionType;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationLineStatus;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationMethod;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationRunStatus;
import com.unionsg.xaccounting.enums.fixedasset.FixedAssetAuditAction;
import com.unionsg.xaccounting.enums.fixedasset.FixedAssetStatus;
import com.unionsg.xaccounting.enums.settings.MappingKey;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.SupplierRepository;
import com.unionsg.xaccounting.repository.accounting.AccountingPeriodRepository;
import com.unionsg.xaccounting.repository.fixedasset.AssetCategoryRepository;
import com.unionsg.xaccounting.repository.fixedasset.AssetEstimateChangeRepository;
import com.unionsg.xaccounting.repository.fixedasset.AssetTransactionRepository;
import com.unionsg.xaccounting.repository.fixedasset.DepreciationRunLineRepository;
import com.unionsg.xaccounting.repository.fixedasset.FixedAssetAuditLogRepository;
import com.unionsg.xaccounting.repository.fixedasset.FixedAssetRepository;
import com.unionsg.xaccounting.repository.journal.JournalEntryRepository;
import com.unionsg.xaccounting.repository.payroll.DepartmentRepository;
import com.unionsg.xaccounting.repository.payroll.WorkLocationRepository;
import com.unionsg.xaccounting.repository.settings.BankAccountRepository;
import com.unionsg.xaccounting.service.DocumentNumberService;
import com.unionsg.xaccounting.service.accounting.PeriodLockGuard;
import com.unionsg.xaccounting.service.settings.AccountingMappingService;
import com.unionsg.xaccounting.service.fixedasset.FixedAssetAccountRules.Role;
import com.unionsg.xaccounting.service.fixedasset.depreciation.DepreciationEngine;
import com.unionsg.xaccounting.service.fixedasset.depreciation.DepreciationInput;
import com.unionsg.xaccounting.service.fixedasset.depreciation.DepreciationResult;
import com.unionsg.xaccounting.service.fixedasset.depreciation.ReducingBalanceCalculator;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The fixed asset register: creating and editing assets, activating (capitalizing) them, changes
 * of estimate, impairment, disposal and retirement, and reversing any of those. Depreciation runs
 * live in {@link DepreciationRunService}; both write the asset's subledger through
 * {@link #record} so the asset's totals always equal its posted transactions.
 */
@Service
@RequiredArgsConstructor
public class FixedAssetService {

    /** Statuses whose asset is on the books. */
    static final Set<FixedAssetStatus> ON_BOOKS = EnumSet.of(FixedAssetStatus.ACTIVE, FixedAssetStatus.FULLY_DEPRECIATED);
    /** Run statuses whose lines are not posted yet. */
    static final Set<DepreciationRunStatus> OPEN_RUNS = EnumSet.of(
            DepreciationRunStatus.CALCULATED, DepreciationRunStatus.SUBMITTED, DepreciationRunStatus.APPROVED);
    private static final int MAX_LIFE_MONTHS = 1200;
    private static final int PROJECTION_MONTHS = 600;

    private final FixedAssetRepository repository;
    private final AssetCategoryRepository categoryRepository;
    private final AssetTransactionRepository transactionRepository;
    private final AssetEstimateChangeRepository estimateChangeRepository;
    private final DepreciationRunLineRepository runLineRepository;
    private final FixedAssetAuditLogRepository auditLogRepository;
    private final WorkLocationRepository locationRepository;
    private final DepartmentRepository departmentRepository;
    private final SupplierRepository supplierRepository;
    private final BankAccountRepository bankAccountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final AccountingPeriodRepository periodRepository;
    private final DocumentNumberService documentNumberService;
    private final PeriodLockGuard periodLockGuard;
    private final FixedAssetAccountRules rules;
    private final FixedAssetJournalService journalService;
    private final FixedAssetAuditService auditService;
    private final DepreciationEngine engine;
    private final AccountingMappingService accountingMappingService;

    public record AssetSearchCriteria(String search, Long categoryId, FixedAssetStatus status, Long locationId,
                                      Long departmentId, LocalDate acquiredFrom, LocalDate acquiredTo) {
    }

    // ------------------------------------------------------------------
    // Register
    // ------------------------------------------------------------------

    @Transactional
    public FixedAssetResponse create(SaveFixedAssetRequest request) {
        FixedAsset asset = new FixedAsset();
        String number = request.getAssetNumber() != null && !request.getAssetNumber().isBlank()
                ? request.getAssetNumber().trim() : null;
        if (number != null) {
            if (number.length() > 50) throw new BusinessException("An asset code can be at most 50 characters");
            if (repository.existsByAssetNumberIgnoreCaseAndDeletedFalse(number)) {
                throw new BusinessException("An asset with code \"" + number + "\" already exists");
            }
        }
        applyDraft(asset, request, true);
        asset.setAssetNumber(number != null ? number : documentNumberService.generateNextNumber(DocumentModule.FIXED_ASSET));
        asset.setStatus(FixedAssetStatus.DRAFT);
        FixedAsset saved = repository.save(asset);
        auditService.recordAsset(saved.getId(), FixedAssetAuditAction.CREATED, null, saved.getStatus(),
                "Registered " + saved.getAssetNumber() + " " + saved.getName() + " at cost " + saved.getAcquisitionCost());
        return toResponse(saved);
    }

    /**
     * A draft can change in full. Once the asset is on the books only descriptive fields can change
     * here; estimates change through {@link #changeEstimate}, and cost or accounts only by reversing
     * the acquisition back to draft.
     */
    @Transactional
    public FixedAssetResponse update(Long id, SaveFixedAssetRequest request) {
        FixedAsset asset = getEntity(id);
        if (asset.getStatus() == FixedAssetStatus.DRAFT) {
            if (request.getAssetNumber() != null && !request.getAssetNumber().isBlank()
                    && !request.getAssetNumber().trim().equalsIgnoreCase(asset.getAssetNumber())) {
                String number = request.getAssetNumber().trim();
                if (repository.existsByAssetNumberIgnoreCaseAndDeletedFalseAndIdNot(number, id)) {
                    throw new BusinessException("An asset with code \"" + number + "\" already exists");
                }
                asset.setAssetNumber(number);
            }
            applyDraft(asset, request, false);
        } else if (ON_BOOKS.contains(asset.getStatus())) {
            assertOnlyDescriptiveChanges(asset, request);
            applyDescriptive(asset, request);
        } else {
            throw new BusinessException("A " + asset.getStatus().getLabel().toLowerCase() + " asset can't be edited");
        }
        FixedAsset saved = repository.save(asset);
        auditService.recordAsset(saved.getId(), FixedAssetAuditAction.UPDATED, saved.getStatus(), saved.getStatus(), "Asset details updated");
        return toResponse(saved);
    }

    /** Soft-deletes a draft. An asset that was ever on the books is disposed of or retired instead. */
    @Transactional
    public void delete(Long id) {
        FixedAsset asset = getEntity(id);
        if (asset.getStatus() != FixedAssetStatus.DRAFT) {
            throw new BusinessException("Only a draft asset can be deleted. Dispose of or retire "
                    + asset.getAssetNumber() + " instead.");
        }
        if (!transactionRepository.findByAssetIdOrderByTransactionDateAscIdAsc(id).isEmpty()) {
            throw new BusinessException(asset.getAssetNumber() + " has accounting history and can't be deleted.");
        }
        asset.setDeleted(true);
        asset.setDeletedAt(LocalDateTime.now());
        repository.save(asset);
        auditService.recordAsset(id, FixedAssetAuditAction.DELETED, FixedAssetStatus.DRAFT, null, "Draft deleted");
    }

    @Transactional(readOnly = true)
    public FixedAssetResponse get(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<FixedAssetListItemResponse> list(AssetSearchCriteria criteria, Pageable pageable) {
        return repository.findAll(spec(criteria), pageable).map(FixedAssetMapper::toListItem);
    }

    // ------------------------------------------------------------------
    // Activation
    // ------------------------------------------------------------------

    /**
     * Puts a draft on the books: resolves and fixes its accounts, posts the acquisition journal
     * (unless the cost is already in the ledger) and records the acquisition in the subledger.
     */
    @Transactional
    public FixedAssetResponse activate(Long id) {
        FixedAsset asset = getEntity(id);
        if (asset.getStatus() != FixedAssetStatus.DRAFT) {
            throw new BusinessException(asset.getAssetNumber() + " is already " + asset.getStatus().getLabel().toLowerCase());
        }
        validateForActivation(asset);
        fixAccounts(asset);

        BigDecimal cost = asset.getAcquisitionCost();
        BigDecimal broughtForward = asset.getOpeningAccumulatedDepreciation();
        JournalEntry journal = null;
        if (asset.getAcquisitionSource() != AssetAcquisitionSource.ALREADY_IN_LEDGER) {
            AccountEntity credit = asset.getAcquisitionSource() == AssetAcquisitionSource.BANK_PAYMENT
                    ? bankGlAccount(asset.getBankAccount())
                    : rules.validateOffset(asset.getOffsetAccount());
            String no = asset.getAssetNumber();
            List<CreateJournalLineRequest> lines = new ArrayList<>();
            lines.add(FixedAssetJournalService.debit(asset.getCostAccount(), "Cost of " + no + " " + asset.getName(), cost));
            if (broughtForward.signum() > 0) {
                lines.add(FixedAssetJournalService.credit(asset.getAccumulatedDepreciationAccount(),
                        "Depreciation brought forward: " + no, broughtForward));
            }
            lines.add(FixedAssetJournalService.credit(credit, "Acquisition of " + no, cost.subtract(broughtForward)));
            journal = journalService.post(JournalType.FIXED_ASSET, asset.getAcquisitionDate(), reference(asset, "ACQ"),
                    "Acquisition of fixed asset " + no + " " + asset.getName() + ".", lines,
                    FixedAssetJournalService.SOURCE_ASSET, asset.getId());
        } else {
            assertCostAlreadyInLedger(asset);
        }

        AssetTransaction tx = new AssetTransaction();
        tx.setType(AssetTransactionType.ACQUISITION);
        tx.setTransactionDate(asset.getAcquisitionDate());
        tx.setCostAmount(cost);
        tx.setDepreciationAmount(broughtForward);
        tx.setJournal(journal != null ? journal : asset.getSourceJournal());
        tx.setDescription(asset.getAcquisitionSource() == AssetAcquisitionSource.ALREADY_IN_LEDGER
                ? "Acquisition recorded from the ledger" : "Acquisition");
        record(asset, tx);

        asset.setAcquisitionJournal(journal);
        asset.setDepreciatedThrough(broughtForward.signum() > 0 ? asset.getOpeningDepreciationDate() : null);
        asset.setActivatedAt(LocalDateTime.now());
        asset.setStatus(FixedAssetStatus.ACTIVE);
        refreshStatus(asset);
        FixedAsset saved = repository.save(asset);
        auditService.recordAsset(saved.getId(), FixedAssetAuditAction.ACTIVATED, FixedAssetStatus.DRAFT, saved.getStatus(),
                "Capitalized at " + cost + (journal != null ? " in journal " + journal.getJournalNumber() : " (already in the ledger)"));
        return toResponse(saved);
    }

    // ------------------------------------------------------------------
    // Change of estimate
    // ------------------------------------------------------------------

    /** Changes useful life, residual value, method, convention or rate from the next unposted day on. */
    @Transactional
    public FixedAssetResponse changeEstimate(Long id, ChangeEstimateRequest request) {
        FixedAsset asset = getEntity(id);
        if (!ON_BOOKS.contains(asset.getStatus())) {
            throw new BusinessException("Estimates can only change on an asset that is on the books. Edit the draft instead.");
        }
        String reason = requireReason(request.getReason());
        DepreciationMethod method = request.getDepreciationMethod() != null ? request.getDepreciationMethod() : asset.getDepreciationMethod();
        Integer life = request.getUsefulLifeMonths() != null ? request.getUsefulLifeMonths() : asset.getUsefulLifeMonths();
        BigDecimal residual = request.getResidualValue() != null ? request.getResidualValue() : asset.getResidualValue();
        BigDecimal rate = request.getAnnualRate() != null ? (request.getAnnualRate().signum() == 0 ? null : request.getAnnualRate()) : asset.getAnnualRate();
        validateEstimates(method, life, residual, rate, asset.getCostBalance());

        AssetEstimateChange change = new AssetEstimateChange();
        change.setAsset(asset);
        change.setEffectiveDate(asset.getDepreciatedThrough() != null ? asset.getDepreciatedThrough().plusDays(1) : asset.getEffectiveDepreciationStart());
        change.setPreviousUsefulLifeMonths(asset.getUsefulLifeMonths());
        change.setNewUsefulLifeMonths(life);
        change.setPreviousResidualValue(asset.getResidualValue());
        change.setNewResidualValue(residual);
        change.setPreviousMethod(asset.getDepreciationMethod());
        change.setNewMethod(method);
        change.setPreviousConvention(asset.getDepreciationConvention());
        change.setNewConvention(request.getDepreciationConvention() != null ? request.getDepreciationConvention() : asset.getDepreciationConvention());
        change.setPreviousAnnualRate(asset.getAnnualRate());
        change.setNewAnnualRate(rate);
        change.setReason(reason);
        if (Objects.equals(change.getPreviousUsefulLifeMonths(), change.getNewUsefulLifeMonths())
                && same(change.getPreviousResidualValue(), change.getNewResidualValue())
                && change.getPreviousMethod() == change.getNewMethod()
                && change.getPreviousConvention() == change.getNewConvention()
                && same(change.getPreviousAnnualRate(), change.getNewAnnualRate())) {
            throw new BusinessException("Nothing changed.");
        }
        estimateChangeRepository.save(change);

        FixedAssetStatus before = asset.getStatus();
        asset.setUsefulLifeMonths(life);
        asset.setResidualValue(residual);
        asset.setDepreciationMethod(method);
        asset.setDepreciationConvention(change.getNewConvention());
        asset.setAnnualRate(rate);
        refreshStatus(asset);
        FixedAsset saved = repository.save(asset);
        auditService.recordAsset(id, FixedAssetAuditAction.ESTIMATE_CHANGED, before, saved.getStatus(),
                "From " + change.getEffectiveDate() + ": life " + change.getPreviousUsefulLifeMonths() + " -> " + life
                        + " months, residual " + change.getPreviousResidualValue() + " -> " + residual
                        + ", method " + change.getPreviousMethod() + " -> " + method + ". " + reason);
        return toResponse(saved);
    }

    // ------------------------------------------------------------------
    // Impairment
    // ------------------------------------------------------------------

    @Transactional
    public FixedAssetResponse impair(Long id, ImpairAssetRequest request) {
        FixedAsset asset = getEntity(id);
        if (!ON_BOOKS.contains(asset.getStatus())) {
            throw new BusinessException("Only an asset on the books can be impaired");
        }
        String reason = requireReason(request.getReason());
        LocalDate date = request.getImpairmentDate() != null ? request.getImpairmentDate() : LocalDate.now();
        BigDecimal amount = money(request.getAmount());
        if (amount == null || amount.signum() <= 0) throw new BusinessException("Enter an impairment amount above zero");
        if (amount.compareTo(asset.getCarryingAmount()) > 0) {
            throw new BusinessException("The impairment can't exceed the carrying amount of " + asset.getCarryingAmount());
        }
        assertChronology(asset, date, "impairment");

        String no = asset.getAssetNumber();
        JournalEntry journal = journalService.post(JournalType.FIXED_ASSET, date, reference(asset, "IMP"),
                "Impairment of fixed asset " + no + ". " + reason,
                List.of(FixedAssetJournalService.debit(rules.resolve(asset, Role.IMPAIRMENT_LOSS), "Impairment loss: " + no, amount),
                        FixedAssetJournalService.credit(asset.getAccumulatedImpairmentAccount(), "Accumulated impairment: " + no, amount)),
                FixedAssetJournalService.SOURCE_ASSET, asset.getId());

        AssetTransaction tx = new AssetTransaction();
        tx.setType(AssetTransactionType.IMPAIRMENT);
        tx.setTransactionDate(date);
        tx.setImpairmentAmount(amount);
        tx.setJournal(journal);
        tx.setDescription(reason);
        record(asset, tx);

        FixedAssetStatus before = asset.getStatus();
        refreshStatus(asset);
        FixedAsset saved = repository.save(asset);
        auditService.recordAsset(id, FixedAssetAuditAction.IMPAIRED, before, saved.getStatus(),
                "Impaired by " + amount + " on " + date + " in journal " + journal.getJournalNumber() + ". " + reason);
        return toResponse(saved);
    }

    // ------------------------------------------------------------------
    // Disposal and retirement
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public DisposalPreviewResponse previewDisposal(Long id, DisposeAssetRequest request) {
        FixedAsset asset = getEntity(id);
        return plan(asset, request).preview();
    }

    /**
     * Charges depreciation up to the disposal date (under the asset's convention), then removes cost,
     * accumulated depreciation and impairment, books any proceeds and the gain or loss, all in one
     * journal and one transaction.
     */
    @Transactional
    public FixedAssetResponse dispose(Long id, DisposeAssetRequest request) {
        FixedAsset asset = getEntity(id);
        DisposalPlan plan = plan(asset, request);
        String no = asset.getAssetNumber();
        boolean retirement = plan.type == AssetDisposalType.RETIREMENT;
        String reason = request.getReason() != null && !request.getReason().isBlank() ? request.getReason().trim() : null;

        JournalEntry journal = journalService.post(JournalType.FIXED_ASSET, plan.date, reference(asset, retirement ? "RET" : "DSP"),
                (retirement ? "Retirement" : "Disposal") + " of fixed asset " + no + " " + asset.getName()
                        + " (" + plan.type.getLabel() + ")." + (reason != null ? " " + reason : ""),
                plan.lines, FixedAssetJournalService.SOURCE_ASSET, asset.getId());

        LocalDate previousThrough = asset.getDepreciatedThrough();
        if (plan.catchUp.charge().signum() > 0) {
            AssetTransaction dep = new AssetTransaction();
            dep.setType(AssetTransactionType.DEPRECIATION);
            dep.setTransactionDate(plan.date);
            dep.setDepreciationAmount(plan.catchUp.charge());
            dep.setJournal(journal);
            dep.setDescription("Depreciation to the disposal date: " + plan.catchUp.explanation());
            record(asset, dep);
            asset.setDepreciatedThrough(plan.date);
        }

        AssetTransaction tx = new AssetTransaction();
        tx.setType(retirement ? AssetTransactionType.RETIREMENT : AssetTransactionType.DISPOSAL);
        tx.setTransactionDate(plan.date);
        tx.setCostAmount(plan.cost.negate());
        tx.setDepreciationAmount(plan.accumulatedDepreciation.negate());
        tx.setImpairmentAmount(plan.accumulatedImpairment.negate());
        tx.setProceedsAmount(plan.proceeds);
        tx.setGainLossAmount(plan.gainLoss);
        tx.setJournal(journal);
        tx.setPreviousDepreciatedThrough(previousThrough);
        tx.setDescription(plan.type.getLabel() + (reason != null ? ": " + reason : ""));
        record(asset, tx);

        FixedAssetStatus before = asset.getStatus();
        asset.setStatus(retirement ? FixedAssetStatus.RETIRED : FixedAssetStatus.DISPOSED);
        asset.setDisposalType(plan.type);
        asset.setDisposalDate(plan.date);
        asset.setDisposalProceeds(plan.proceeds);
        asset.setDisposalGainLoss(plan.gainLoss);
        asset.setDisposalJournal(journal);
        asset.setDisposedAt(LocalDateTime.now());
        FixedAsset saved = repository.save(asset);
        auditService.recordAsset(id, retirement ? FixedAssetAuditAction.RETIRED : FixedAssetAuditAction.DISPOSED, before, saved.getStatus(),
                plan.type.getLabel() + " on " + plan.date + ": carrying " + plan.carrying + ", proceeds " + plan.proceeds
                        + ", " + (plan.gainLoss.signum() >= 0 ? "gain " : "loss ") + plan.gainLoss.abs()
                        + " in journal " + journal.getJournalNumber());
        return toResponse(saved);
    }

    private DisposalPlan plan(FixedAsset asset, DisposeAssetRequest request) {
        if (!ON_BOOKS.contains(asset.getStatus())) {
            throw new BusinessException("Only an asset on the books can be disposed of or retired");
        }
        AssetDisposalType type = request.getDisposalType() != null ? request.getDisposalType() : AssetDisposalType.SALE;
        LocalDate date = request.getDisposalDate();
        if (date == null) throw new BusinessException("A disposal date is required");
        if (date.isAfter(LocalDate.now())) throw new BusinessException("The disposal date can't be in the future");
        if (date.isBefore(asset.getAcquisitionDate())) throw new BusinessException("The disposal date can't be before the acquisition date");
        if (asset.getDepreciatedThrough() != null && asset.getDepreciatedThrough().isAfter(date)) {
            throw new BusinessException("Depreciation is posted through " + asset.getDepreciatedThrough()
                    + ", after the disposal date. Reverse the depreciation runs after " + date + " first.");
        }
        assertChronology(asset, date, "disposal");
        periodLockGuard.assertPostable(date);

        BigDecimal proceeds = money(request.getProceeds());
        if (proceeds == null) proceeds = BigDecimal.ZERO;
        if (proceeds.signum() < 0) throw new BusinessException("Proceeds can't be negative");
        if (type == AssetDisposalType.RETIREMENT && proceeds.signum() > 0) {
            throw new BusinessException("A retirement has no proceeds. Record a sale instead.");
        }
        AccountEntity proceedsAccount = null;
        if (proceeds.signum() > 0) {
            if (request.getProceedsAccountId() != null) {
                proceedsAccount = rules.validateProceeds(rules.byId(request.getProceedsAccountId(), "Proceeds"));
            } else if (request.getBankAccountId() != null) {
                proceedsAccount = bankGlAccount(bankAccountRepository.findById(request.getBankAccountId())
                        .orElseThrow(() -> new BusinessException("Bank account not found")));
            } else {
                proceedsAccount = bankGlAccount(null);
            }
        }

        // Depreciation from the last posted day to the disposal date.
        LocalDate start = asset.getEffectiveDepreciationStart();
        LocalDate from = asset.getDepreciatedThrough() != null ? asset.getDepreciatedThrough().plusDays(1) : start;
        DepreciationResult catchUp;
        if (start == null || from == null || from.isAfter(date)) {
            catchUp = engine.calculate(inputFor(asset, from, from == null ? null : from.minusDays(1), date));
        } else {
            catchUp = engine.calculate(inputFor(asset, from, date, date));
        }

        BigDecimal cost = asset.getCostBalance();
        BigDecimal accDep = asset.getAccumulatedDepreciation().add(catchUp.charge());
        BigDecimal accImp = asset.getAccumulatedImpairment();
        BigDecimal carrying = cost.subtract(accDep).subtract(accImp);
        BigDecimal gainLoss = proceeds.subtract(carrying);
        String no = asset.getAssetNumber();

        List<CreateJournalLineRequest> lines = new ArrayList<>();
        if (catchUp.charge().signum() > 0) {
            lines.add(FixedAssetJournalService.debit(rules.resolve(asset, Role.DEPRECIATION_EXPENSE), "Depreciation to disposal: " + no, catchUp.charge()));
            lines.add(FixedAssetJournalService.credit(asset.getAccumulatedDepreciationAccount(), "Depreciation to disposal: " + no, catchUp.charge()));
        }
        if (accDep.signum() > 0) {
            lines.add(FixedAssetJournalService.debit(asset.getAccumulatedDepreciationAccount(), "Remove accumulated depreciation: " + no, accDep));
        }
        if (accImp.signum() > 0) {
            lines.add(FixedAssetJournalService.debit(asset.getAccumulatedImpairmentAccount(), "Remove accumulated impairment: " + no, accImp));
        }
        if (proceeds.signum() > 0) {
            lines.add(FixedAssetJournalService.debit(proceedsAccount, "Disposal proceeds: " + no, proceeds));
        }
        if (gainLoss.signum() < 0) {
            lines.add(FixedAssetJournalService.debit(rules.resolve(asset, Role.DISPOSAL_LOSS), "Loss on disposal: " + no, gainLoss.negate()));
        }
        lines.add(FixedAssetJournalService.credit(asset.getCostAccount(), "Remove cost: " + no, cost));
        if (gainLoss.signum() > 0) {
            lines.add(FixedAssetJournalService.credit(rules.resolve(asset, Role.DISPOSAL_GAIN), "Gain on disposal: " + no, gainLoss));
        }
        return new DisposalPlan(type, date, cost, asset.getAccumulatedDepreciation(), catchUp, accDep, accImp, carrying, proceeds, gainLoss, lines);
    }

    private record DisposalPlan(AssetDisposalType type, LocalDate date, BigDecimal cost, BigDecimal accDepBefore,
                                DepreciationResult catchUp, BigDecimal accumulatedDepreciation, BigDecimal accumulatedImpairment,
                                BigDecimal carrying, BigDecimal proceeds, BigDecimal gainLoss, List<CreateJournalLineRequest> lines) {
        DisposalPreviewResponse preview() {
            DisposalPreviewResponse r = new DisposalPreviewResponse();
            r.setDisposalDate(date);
            r.setDisposalType(type);
            r.setCost(cost);
            r.setAccumulatedDepreciationBefore(accDepBefore);
            r.setCatchUpDepreciation(catchUp.charge());
            r.setCatchUpExplanation(catchUp.explanation());
            r.setAccumulatedDepreciation(accumulatedDepreciation);
            r.setAccumulatedImpairment(accumulatedImpairment);
            r.setCarryingAmount(carrying);
            r.setProceeds(proceeds);
            r.setGainLoss(gainLoss);
            r.setJournalLines(lines.stream().map(FixedAssetService::previewLine).toList());
            return r;
        }
    }

    // ------------------------------------------------------------------
    // Reversal
    // ------------------------------------------------------------------

    /**
     * Reverses the asset's newest live transaction (acquisition, impairment, disposal or retirement)
     * with a mirror journal and opposite subledger rows. Depreciation from a run is reversed with
     * its run. Reversing the acquisition returns the asset to draft.
     */
    @Transactional
    public FixedAssetResponse reverseTransaction(Long id, Long transactionId, AssetActionRequest request) {
        FixedAsset asset = getEntity(id);
        String reason = requireReason(request.getReason());
        AssetTransaction target = transactionRepository.findById(transactionId)
                .filter(t -> t.getAsset().getId().equals(id))
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        if (target.getType() == AssetTransactionType.REVERSAL || target.isReversed()) {
            throw new BusinessException("This transaction is already reversed");
        }
        if (target.getRunLine() != null) {
            throw new BusinessException("Depreciation from run " + target.getRunLine().getRun().getRunNumber()
                    + " is reversed from the run itself");
        }
        List<AssetTransaction> live = transactionRepository.findLiveNewestFirst(id);
        List<AssetTransaction> group = live.stream()
                .filter(t -> target.getJournal() != null ? t.getJournal() != null && t.getJournal().getId().equals(target.getJournal().getId())
                        : t.getId().equals(target.getId()))
                .toList();
        if (live.isEmpty() || !group.contains(live.get(0))) {
            AssetTransaction newest = live.isEmpty() ? null : live.get(0);
            String what = newest == null ? "" : newest.getRunLine() != null
                    ? " Reverse depreciation run " + newest.getRunLine().getRun().getRunNumber() + " first."
                    : " Reverse the " + newest.getType().getLabel().toLowerCase() + " of " + newest.getTransactionDate() + " first.";
            throw new BusinessException("Only the newest transaction can be reversed." + what);
        }

        JournalEntry journal = target.getJournal();
        boolean ownJournal = journal != null && FixedAssetJournalService.SOURCE_ASSET.equals(journal.getSourceModule())
                && Objects.equals(journal.getSourceEntityId(), asset.getId());
        JournalEntry reversal = null;
        LocalDate date;
        if (ownJournal) {
            if (journal.getStatus() != JournalStatus.POSTED) {
                throw new BusinessException("Journal " + journal.getJournalNumber() + " is " + journal.getStatus() + " and can't be reversed");
            }
            date = journalService.defaultReversalDate(journal.getJournalDate(), request.getDate());
            reversal = journalService.reverse(journal, reason, date);
        } else {
            date = request.getDate() != null ? request.getDate() : target.getTransactionDate();
        }

        LocalDate restoreThrough = asset.getDepreciatedThrough();
        for (AssetTransaction original : group) {
            AssetTransaction rev = new AssetTransaction();
            rev.setType(AssetTransactionType.REVERSAL);
            rev.setMovementType(original.getType());
            rev.setTransactionDate(date);
            rev.setCostAmount(original.getCostAmount().negate());
            rev.setDepreciationAmount(original.getDepreciationAmount().negate());
            rev.setImpairmentAmount(original.getImpairmentAmount().negate());
            rev.setProceedsAmount(original.getProceedsAmount() != null ? original.getProceedsAmount().negate() : null);
            rev.setGainLossAmount(original.getGainLossAmount() != null ? original.getGainLossAmount().negate() : null);
            rev.setJournal(reversal);
            rev.setReverses(original);
            rev.setDescription("Reversal of " + original.getType().getLabel().toLowerCase() + ": " + reason);
            record(asset, rev);
            original.setReversedAt(LocalDateTime.now());
            transactionRepository.save(original);
            if (original.getPreviousDepreciatedThrough() != null || original.getType() == AssetTransactionType.DISPOSAL
                    || original.getType() == AssetTransactionType.RETIREMENT) {
                restoreThrough = original.getPreviousDepreciatedThrough();
            }
        }

        FixedAssetStatus before = asset.getStatus();
        switch (target.getType()) {
            case ACQUISITION -> {
                asset.setStatus(FixedAssetStatus.DRAFT);
                asset.setAcquisitionJournal(null);
                asset.setActivatedAt(null);
                asset.setDepreciatedThrough(null);
            }
            case DISPOSAL, RETIREMENT -> {
                asset.setStatus(FixedAssetStatus.ACTIVE);
                asset.setDepreciatedThrough(restoreThrough);
                asset.setDisposalType(null);
                asset.setDisposalDate(null);
                asset.setDisposalProceeds(null);
                asset.setDisposalGainLoss(null);
                asset.setDisposalJournal(null);
                asset.setDisposedAt(null);
                refreshStatus(asset);
            }
            default -> refreshStatus(asset);
        }
        FixedAsset saved = repository.save(asset);
        auditService.recordAsset(id, FixedAssetAuditAction.TRANSACTION_REVERSED, before, saved.getStatus(),
                "Reversed the " + target.getType().getLabel().toLowerCase() + " of " + target.getTransactionDate()
                        + (reversal != null ? " in journal " + reversal.getJournalNumber() : "") + ". " + reason);
        return toResponse(saved);
    }

    // ------------------------------------------------------------------
    // Enquiry
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AssetTransactionResponse> transactions(Long id) {
        getEntity(id);
        List<AssetTransaction> all = transactionRepository.findByAssetIdOrderByTransactionDateAscIdAsc(id);
        List<AssetTransaction> live = transactionRepository.findLiveNewestFirst(id);
        Long newestJournal = !live.isEmpty() && live.get(0).getRunLine() == null && live.get(0).getJournal() != null
                ? live.get(0).getJournal().getId() : null;
        Long newestId = !live.isEmpty() && live.get(0).getRunLine() == null ? live.get(0).getId() : null;
        return all.stream().map(t -> FixedAssetMapper.toTransaction(t,
                t.getType() != AssetTransactionType.REVERSAL && !t.isReversed() && t.getRunLine() == null
                        && (Objects.equals(t.getId(), newestId)
                        || (newestJournal != null && t.getJournal() != null && newestJournal.equals(t.getJournal().getId())))))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FixedAssetAuditLogResponse> activity(Long id) {
        getEntity(id);
        return auditLogRepository.findByAssetIdOrderByPerformedAtDescIdDesc(id).stream().map(FixedAssetMapper::toAudit).toList();
    }

    @Transactional(readOnly = true)
    public List<AssetEstimateChangeResponse> estimateChanges(Long id) {
        getEntity(id);
        return estimateChangeRepository.findByAssetIdOrderByIdDesc(id).stream().map(FixedAssetMapper::toEstimateChange).toList();
    }

    /**
     * The asset's depreciation schedule: every charge already posted (or reversed), charges sitting in
     * runs not yet posted, then a month-by-month projection to the end of its useful life.
     */
    @Transactional(readOnly = true)
    public List<ScheduleLineResponse> schedule(Long id) {
        FixedAsset asset = getEntity(id);
        List<ScheduleLineResponse> rows = new ArrayList<>();

        BigDecimal cost = asset.getStatus() == FixedAssetStatus.DRAFT ? asset.getAcquisitionCost() : asset.getCostBalance();
        BigDecimal impairment = asset.getAccumulatedImpairment();
        BigDecimal opening = asset.getStatus() == FixedAssetStatus.DRAFT ? asset.getOpeningAccumulatedDepreciation() : BigDecimal.ZERO;

        if (asset.getStatus() != FixedAssetStatus.DRAFT) {
            BigDecimal accumulated = BigDecimal.ZERO;
            for (AssetTransaction t : transactionRepository.findByAssetIdOrderByTransactionDateAscIdAsc(id)) {
                AssetTransactionType kind = t.getType() == AssetTransactionType.REVERSAL ? t.getMovementType() : t.getType();
                if (kind == AssetTransactionType.ACQUISITION && t.getDepreciationAmount().signum() != 0) {
                    accumulated = accumulated.add(t.getDepreciationAmount());
                    rows.add(scheduleRow(t.getType() == AssetTransactionType.REVERSAL ? "REVERSED" : "BROUGHT_FORWARD",
                            null, t.getTransactionDate(), null, t.getDepreciationAmount(), accumulated, null, t, "Depreciation brought forward"));
                } else if (kind == AssetTransactionType.DEPRECIATION) {
                    accumulated = accumulated.add(t.getDepreciationAmount());
                    DepreciationRunLine line = t.getRunLine() != null ? t.getRunLine()
                            : t.getReverses() != null ? t.getReverses().getRunLine() : null;
                    String status = t.getType() == AssetTransactionType.REVERSAL ? "REVERSED" : "POSTED";
                    ScheduleLineResponse row = scheduleRow(status, line != null ? line.getFromDate() : null,
                            line != null ? line.getToDate() : t.getTransactionDate(),
                            line != null ? line.getOpeningCarryingAmount() : null, t.getDepreciationAmount(), accumulated,
                            line != null ? line.getMonthsCharged() : null, t,
                            line != null ? line.getExplanation() : t.getDescription());
                    if (line != null) {
                        row.setRunId(line.getRun().getId());
                        row.setRunNumber(line.getRun().getRunNumber());
                        row.setPeriodName(line.getRun().getAccountingPeriod().getName());
                    }
                    rows.add(row);
                }
            }
            for (DepreciationRunLine l : runLineRepository.findByAssetInRunStatuses(id, OPEN_RUNS)) {
                if (l.getStatus() != DepreciationLineStatus.CALCULATED) continue;
                accumulated = accumulated.add(l.getDepreciationAmount());
                ScheduleLineResponse row = scheduleRow("CALCULATED", l.getFromDate(), l.getToDate(), l.getOpeningCarryingAmount(),
                        l.getDepreciationAmount(), asset.getAccumulatedDepreciation().add(l.getDepreciationAmount()), l.getMonthsCharged(), null,
                        l.getExplanation());
                row.setClosingCarryingAmount(l.getClosingCarryingAmount());
                row.setRunId(l.getRun().getId());
                row.setRunNumber(l.getRun().getRunNumber());
                row.setPeriodName(l.getRun().getAccountingPeriod().getName());
                rows.add(row);
                opening = opening.add(l.getDepreciationAmount());
            }
        }

        boolean projectable = asset.getStatus() == FixedAssetStatus.DRAFT || asset.getStatus() == FixedAssetStatus.ACTIVE;
        LocalDate start = asset.getEffectiveDepreciationStart();
        if (projectable && start != null && asset.getDepreciationMethod() != DepreciationMethod.NOT_DEPRECIATED
                && (asset.getUsefulLifeMonths() != null || !engine.requiresUsefulLife(asset.getDepreciationMethod()))) {
            LocalDate through = rows.stream().map(ScheduleLineResponse::getToDate).filter(Objects::nonNull)
                    .max(Comparator.naturalOrder()).orElse(null);
            if (asset.getStatus() == FixedAssetStatus.DRAFT && asset.getOpeningAccumulatedDepreciation().signum() > 0) {
                through = asset.getOpeningDepreciationDate();
            }
            if (asset.getDepreciatedThrough() != null && (through == null || asset.getDepreciatedThrough().isAfter(through))) {
                through = asset.getDepreciatedThrough();
            }
            BigDecimal accumulated = asset.getStatus() == FixedAssetStatus.DRAFT ? opening : asset.getAccumulatedDepreciation().add(opening);
            DepreciationInput base = DepreciationInput.builder()
                    .method(asset.getDepreciationMethod()).convention(asset.getDepreciationConvention()).cost(cost)
                    .accumulatedDepreciation(accumulated).accumulatedImpairment(impairment).residualValue(asset.getResidualValue())
                    .usefulLifeMonths(asset.getUsefulLifeMonths()).annualRatePercent(asset.getAnnualRate()).depreciationStart(start)
                    .build();
            LocalDate from = through != null ? through.plusDays(1) : start;
            for (DepreciationEngine.ProjectedCharge p : engine.project(base, from, PROJECTION_MONTHS)) {
                DepreciationResult r = p.result();
                accumulated = accumulated.add(r.charge());
                ScheduleLineResponse row = scheduleRow("PROJECTED", p.from(), p.to(), r.openingCarrying(), r.charge(), accumulated,
                        r.monthsCharged(), null, r.explanation());
                row.setPeriodName(YearMonth.from(p.from()).toString());
                rows.add(row);
            }
        }
        return rows;
    }

    // ------------------------------------------------------------------
    // Shared with the run service and reports
    // ------------------------------------------------------------------

    /** Saves a subledger row and refreshes the asset's posted totals from all of its rows. */
    @Transactional
    public AssetTransaction record(FixedAsset asset, AssetTransaction tx) {
        tx.setAsset(asset);
        if (tx.getMovementType() == null) tx.setMovementType(tx.getType());
        AssetTransaction saved = transactionRepository.save(tx);
        refreshTotals(asset);
        return saved;
    }

    /** Recomputes cost, accumulated depreciation and impairment from the posted subledger. */
    public void refreshTotals(FixedAsset asset) {
        BigDecimal cost = BigDecimal.ZERO, dep = BigDecimal.ZERO, imp = BigDecimal.ZERO;
        for (AssetTransaction t : transactionRepository.findByAssetIdOrderByTransactionDateAscIdAsc(asset.getId())) {
            cost = cost.add(t.getCostAmount());
            dep = dep.add(t.getDepreciationAmount());
            imp = imp.add(t.getImpairmentAmount());
        }
        asset.setCostBalance(cost);
        asset.setAccumulatedDepreciation(dep);
        asset.setAccumulatedImpairment(imp);
    }

    /** ACTIVE while the asset still has something to depreciate, FULLY_DEPRECIATED once it doesn't. */
    public void refreshStatus(FixedAsset asset) {
        if (!ON_BOOKS.contains(asset.getStatus())) return;
        boolean done = asset.getDepreciationMethod() != DepreciationMethod.NOT_DEPRECIATED
                && asset.getCostBalance().signum() > 0
                && asset.getCarryingAmount().compareTo(nz(asset.getResidualValue())) <= 0;
        asset.setStatus(done ? FixedAssetStatus.FULLY_DEPRECIATED : FixedAssetStatus.ACTIVE);
    }

    /** The engine input for charging {@code [from, to]} on the asset's current posted balances. */
    public DepreciationInput inputFor(FixedAsset asset, LocalDate from, LocalDate to, LocalDate cutoff) {
        return DepreciationInput.builder()
                .method(asset.getDepreciationMethod())
                .convention(asset.getDepreciationConvention())
                .cost(asset.getCostBalance())
                .accumulatedDepreciation(asset.getAccumulatedDepreciation())
                .accumulatedImpairment(asset.getAccumulatedImpairment())
                .residualValue(asset.getResidualValue())
                .usefulLifeMonths(asset.getUsefulLifeMonths())
                .annualRatePercent(asset.getAnnualRate())
                .depreciationStart(asset.getEffectiveDepreciationStart())
                .windowFrom(from)
                .windowTo(to)
                .cutoffDate(cutoff)
                .build();
    }

    public FixedAsset getEntity(Long id) {
        return repository.findById(id)
                .filter(a -> !Boolean.TRUE.equals(a.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Fixed asset not found with ID: " + id));
    }

    public FixedAssetResponse toResponse(FixedAsset a) {
        FixedAssetResponse r = new FixedAssetResponse();
        r.setId(a.getId());
        r.setAssetNumber(a.getAssetNumber());
        r.setName(a.getName());
        r.setCategoryId(a.getCategory().getId());
        r.setCategoryName(a.getCategory().getName());
        r.setDescription(a.getDescription());
        r.setSerialNumber(a.getSerialNumber());
        if (a.getLocation() != null) {
            r.setLocationId(a.getLocation().getId());
            r.setLocationName(a.getLocation().getName());
        }
        if (a.getDepartment() != null) {
            r.setDepartmentId(a.getDepartment().getId());
            r.setDepartmentName(a.getDepartment().getName());
        }
        if (a.getSupplier() != null) {
            r.setSupplierId(a.getSupplier().getId());
            r.setSupplierName(supplierName(a.getSupplier()));
        }
        r.setAcquisitionDate(a.getAcquisitionDate());
        r.setAvailableForUseDate(a.getAvailableForUseDate());
        r.setDepreciationStartDate(a.getDepreciationStartDate());
        r.setEffectiveDepreciationStart(a.getEffectiveDepreciationStart());
        r.setAcquisitionCost(a.getAcquisitionCost());
        r.setResidualValue(a.getResidualValue());
        r.setUsefulLifeMonths(a.getUsefulLifeMonths());
        r.setDepreciationMethod(a.getDepreciationMethod());
        r.setDepreciationConvention(a.getDepreciationConvention());
        r.setAnnualRate(a.getAnnualRate());
        if (a.getDepreciationMethod() == DepreciationMethod.REDUCING_BALANCE) {
            try {
                r.setEffectiveAnnualRate(ReducingBalanceCalculator.rateFor(inputFor(a, null, null, null).toBuilder()
                        .cost(a.getAcquisitionCost()).build()));
            } catch (IllegalArgumentException ignored) {
                r.setEffectiveAnnualRate(null);
            }
        }
        r.setAcquisitionSource(a.getAcquisitionSource());
        if (a.getBankAccount() != null) {
            r.setBankAccountId(a.getBankAccount().getId());
            r.setBankAccountName(a.getBankAccount().getAccountName());
        }
        r.setOffsetAccount(FixedAssetMapper.toRef(a.getOffsetAccount()));
        if (a.getSourceJournal() != null) {
            r.setSourceJournalId(a.getSourceJournal().getId());
            r.setSourceJournalNumber(a.getSourceJournal().getJournalNumber());
        }
        r.setOpeningAccumulatedDepreciation(a.getOpeningAccumulatedDepreciation());
        r.setOpeningDepreciationDate(a.getOpeningDepreciationDate());
        r.setAccounts(accountRoles(a));
        r.setStatus(a.getStatus());
        r.setCostBalance(a.getCostBalance());
        r.setAccumulatedDepreciation(a.getAccumulatedDepreciation());
        r.setAccumulatedImpairment(a.getAccumulatedImpairment());
        r.setCarryingAmount(a.getCarryingAmount());
        r.setCurrentPeriodDepreciation(currentPeriodDepreciation(a));
        r.setDepreciatedThrough(a.getDepreciatedThrough());
        if (a.getEffectiveDepreciationStart() != null && a.getUsefulLifeMonths() != null) {
            r.setUsefulLifeEndDate(a.getEffectiveDepreciationStart().plusMonths(a.getUsefulLifeMonths()).minusDays(1));
        }
        if (a.getAcquisitionJournal() != null) {
            r.setAcquisitionJournalId(a.getAcquisitionJournal().getId());
            r.setAcquisitionJournalNumber(a.getAcquisitionJournal().getJournalNumber());
        }
        r.setDisposalType(a.getDisposalType());
        r.setDisposalDate(a.getDisposalDate());
        r.setDisposalProceeds(a.getDisposalProceeds());
        r.setDisposalGainLoss(a.getDisposalGainLoss());
        if (a.getDisposalJournal() != null) {
            r.setDisposalJournalId(a.getDisposalJournal().getId());
            r.setDisposalJournalNumber(a.getDisposalJournal().getJournalNumber());
        }
        r.setNotes(a.getNotes());
        r.setActivatedAt(a.getActivatedAt());
        r.setDisposedAt(a.getDisposedAt());
        r.setCreatedAt(a.getCreatedAt());
        r.setCreatedBy(FixedAssetMapper.userName(a.getCreatedBy()));
        r.setEditable(a.getStatus() == FixedAssetStatus.DRAFT || ON_BOOKS.contains(a.getStatus()));
        r.setDeletable(a.getStatus() == FixedAssetStatus.DRAFT);
        return r;
    }

    /** Each posting role with the asset's own account, what it resolves to, and any problem. */
    public List<AccountRoleResponse> accountRoles(FixedAsset a) {
        List<AccountRoleResponse> out = new ArrayList<>();
        for (Role role : Role.values()) {
            AccountRoleResponse r = new AccountRoleResponse();
            r.setRole(role.name());
            r.setLabel(role.getLabel());
            AccountEntity own = switch (role) {
                case COST -> a.getCostAccount();
                case ACCUMULATED_DEPRECIATION -> a.getAccumulatedDepreciationAccount();
                case DEPRECIATION_EXPENSE -> a.getDepreciationExpenseAccount();
                case ACCUMULATED_IMPAIRMENT -> a.getAccumulatedImpairmentAccount();
                case IMPAIRMENT_LOSS -> a.getImpairmentLossAccount();
                case DISPOSAL_GAIN -> a.getDisposalGainAccount();
                case DISPOSAL_LOSS -> a.getDisposalLossAccount();
            };
            r.setOwn(FixedAssetMapper.toRef(own));
            r.setSource(own != null ? "ASSET" : a.getCategory() != null && FixedAssetAccountRules.categoryAccount(a.getCategory(), role) != null ? "CATEGORY" : "MAPPING");
            try {
                r.setResolved(FixedAssetMapper.toRef(rules.resolve(a, role)));
            } catch (BusinessException e) {
                r.setError(e.getMessage());
            }
            out.add(r);
        }
        return out;
    }

    // ------------------------------------------------------------------
    // Validation and helpers
    // ------------------------------------------------------------------

    private void applyDraft(FixedAsset asset, SaveFixedAssetRequest r, boolean creating) {
        AssetCategory category = r.getCategoryId() != null
                ? categoryRepository.findById(r.getCategoryId()).filter(c -> !Boolean.TRUE.equals(c.getDeleted()))
                .orElseThrow(() -> new BusinessException("Asset category not found"))
                : asset.getCategory();
        if (category == null) throw new BusinessException("An asset category is required");
        if (!Boolean.TRUE.equals(category.getActive()) && (asset.getCategory() == null || !asset.getCategory().getId().equals(category.getId()))) {
            throw new BusinessException("Asset category \"" + category.getName() + "\" is inactive");
        }
        asset.setCategory(category);
        applyDescriptive(asset, r);
        if (asset.getName() == null || asset.getName().isBlank()) throw new BusinessException("An asset name is required");

        if (r.getAcquisitionDate() == null) throw new BusinessException("An acquisition date is required");
        if (r.getAcquisitionDate().isAfter(LocalDate.now())) throw new BusinessException("The acquisition date can't be in the future");
        BigDecimal cost = money(r.getAcquisitionCost());
        if (cost == null || cost.signum() <= 0) throw new BusinessException("Enter an acquisition cost above zero");
        if (r.getAvailableForUseDate() != null && r.getAvailableForUseDate().isBefore(r.getAcquisitionDate())) {
            throw new BusinessException("The date available for use can't be before the acquisition date");
        }
        if (r.getDepreciationStartDate() != null) {
            if (r.getAvailableForUseDate() == null) {
                throw new BusinessException("Set the date available for use before a depreciation start date");
            }
            if (r.getDepreciationStartDate().isBefore(r.getAvailableForUseDate())) {
                throw new BusinessException("Depreciation can't start before the asset is available for use");
            }
        }
        asset.setAcquisitionDate(r.getAcquisitionDate());
        asset.setAvailableForUseDate(r.getAvailableForUseDate());
        asset.setDepreciationStartDate(r.getDepreciationStartDate());
        asset.setAcquisitionCost(cost);

        DepreciationMethod method = r.getDepreciationMethod() != null ? r.getDepreciationMethod() : category.getDefaultMethod();
        Integer life = r.getUsefulLifeMonths() != null ? r.getUsefulLifeMonths() : (creating ? category.getDefaultUsefulLifeMonths() : null);
        BigDecimal residual = money(r.getResidualValue());
        if (residual == null) {
            residual = creating && category.getDefaultResidualRate() != null
                    ? cost.multiply(category.getDefaultResidualRate()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
        }
        BigDecimal rate = r.getAnnualRate() != null ? (r.getAnnualRate().signum() == 0 ? null : r.getAnnualRate())
                : (creating ? category.getDefaultAnnualRate() : null);
        validateEstimates(method, life, residual, rate, cost);
        asset.setDepreciationMethod(method);
        asset.setDepreciationConvention(r.getDepreciationConvention() != null ? r.getDepreciationConvention() : category.getDefaultConvention());
        asset.setUsefulLifeMonths(life);
        asset.setResidualValue(residual);
        asset.setAnnualRate(method == DepreciationMethod.REDUCING_BALANCE ? rate : null);

        AssetAcquisitionSource source = r.getAcquisitionSource() != null ? r.getAcquisitionSource() : AssetAcquisitionSource.BANK_PAYMENT;
        asset.setAcquisitionSource(source);
        asset.setBankAccount(null);
        asset.setOffsetAccount(null);
        asset.setSourceJournal(null);
        switch (source) {
            case BANK_PAYMENT -> {
                if (r.getBankAccountId() != null) {
                    asset.setBankAccount(bankAccountRepository.findById(r.getBankAccountId())
                            .orElseThrow(() -> new BusinessException("Bank account not found")));
                }
            }
            case OFFSET_ACCOUNT -> {
                if (r.getOffsetAccountId() == null) throw new BusinessException("Pick the account the acquisition is credited to");
                asset.setOffsetAccount(rules.validateOffset(rules.byId(r.getOffsetAccountId(), "Offset")));
            }
            case ALREADY_IN_LEDGER -> {
                if (r.getSourceJournalId() != null) {
                    JournalEntry j = journalEntryRepository.findById(r.getSourceJournalId())
                            .orElseThrow(() -> new BusinessException("Source journal not found"));
                    if (j.getStatus() != JournalStatus.POSTED) {
                        throw new BusinessException("Journal " + j.getJournalNumber() + " is not posted");
                    }
                    asset.setSourceJournal(j);
                }
            }
        }

        BigDecimal bf = money(r.getOpeningAccumulatedDepreciation());
        if (bf == null) bf = BigDecimal.ZERO;
        if (bf.signum() < 0) throw new BusinessException("Depreciation brought forward can't be negative");
        if (bf.signum() > 0) {
            if (source == AssetAcquisitionSource.BANK_PAYMENT) {
                throw new BusinessException("Depreciation brought forward is for assets taken on from earlier records. "
                        + "Use \"Against Another Account\" or \"Already in the Ledger\".");
            }
            if (bf.compareTo(cost.subtract(residual)) > 0) {
                throw new BusinessException("Depreciation brought forward can't exceed cost less residual value ("
                        + cost.subtract(residual) + ")");
            }
            if (r.getOpeningDepreciationDate() == null) {
                throw new BusinessException("Enter the date the brought-forward depreciation runs to");
            }
            LocalDate start = r.getDepreciationStartDate() != null ? r.getDepreciationStartDate() : r.getAvailableForUseDate();
            if (start != null && r.getOpeningDepreciationDate().isBefore(start)) {
                throw new BusinessException("The brought-forward date can't be before depreciation starts");
            }
            if (r.getOpeningDepreciationDate().isAfter(LocalDate.now())) {
                throw new BusinessException("The brought-forward date can't be in the future");
            }
        }
        asset.setOpeningAccumulatedDepreciation(bf);
        asset.setOpeningDepreciationDate(bf.signum() > 0 ? r.getOpeningDepreciationDate() : null);

        asset.setCostAccount(override(r.getCostAccountId(), Role.COST));
        asset.setAccumulatedDepreciationAccount(override(r.getAccumulatedDepreciationAccountId(), Role.ACCUMULATED_DEPRECIATION));
        asset.setDepreciationExpenseAccount(override(r.getDepreciationExpenseAccountId(), Role.DEPRECIATION_EXPENSE));
        asset.setAccumulatedImpairmentAccount(override(r.getAccumulatedImpairmentAccountId(), Role.ACCUMULATED_IMPAIRMENT));
        asset.setImpairmentLossAccount(override(r.getImpairmentLossAccountId(), Role.IMPAIRMENT_LOSS));
        asset.setDisposalGainAccount(override(r.getDisposalGainAccountId(), Role.DISPOSAL_GAIN));
        asset.setDisposalLossAccount(override(r.getDisposalLossAccountId(), Role.DISPOSAL_LOSS));
    }

    private void applyDescriptive(FixedAsset asset, SaveFixedAssetRequest r) {
        if (r.getName() != null) {
            if (r.getName().trim().length() > 200) throw new BusinessException("An asset name can be at most 200 characters");
            asset.setName(r.getName().trim());
        }
        asset.setDescription(blankToNull(r.getDescription()));
        String serial = blankToNull(r.getSerialNumber());
        if (serial != null && serial.length() > 100) throw new BusinessException("A serial number can be at most 100 characters");
        asset.setSerialNumber(serial);
        asset.setLocation(r.getLocationId() != null ? locationRepository.findById(r.getLocationId())
                .orElseThrow(() -> new BusinessException("Location not found")) : null);
        asset.setDepartment(r.getDepartmentId() != null ? departmentRepository.findById(r.getDepartmentId())
                .orElseThrow(() -> new BusinessException("Department not found")) : null);
        asset.setSupplier(r.getSupplierId() != null ? supplierRepository.findById(r.getSupplierId())
                .orElseThrow(() -> new BusinessException("Supplier not found")) : null);
        asset.setNotes(blankToNull(r.getNotes()));
    }

    /** Refuses changes to the financial fields of an asset that is already on the books. */
    private void assertOnlyDescriptiveChanges(FixedAsset a, SaveFixedAssetRequest r) {
        List<String> changed = new ArrayList<>();
        if (r.getCategoryId() != null && !r.getCategoryId().equals(a.getCategory().getId())) changed.add("category");
        if (r.getAcquisitionDate() != null && !r.getAcquisitionDate().equals(a.getAcquisitionDate())) changed.add("acquisition date");
        if (r.getAcquisitionCost() != null && !same(r.getAcquisitionCost(), a.getAcquisitionCost())) changed.add("cost");
        if (r.getAvailableForUseDate() != null && !r.getAvailableForUseDate().equals(a.getAvailableForUseDate())) changed.add("date available for use");
        if (r.getDepreciationStartDate() != null && !r.getDepreciationStartDate().equals(a.getDepreciationStartDate())) changed.add("depreciation start");
        if (r.getUsefulLifeMonths() != null && !r.getUsefulLifeMonths().equals(a.getUsefulLifeMonths())) changed.add("useful life");
        if (r.getResidualValue() != null && !same(r.getResidualValue(), a.getResidualValue())) changed.add("residual value");
        if (r.getDepreciationMethod() != null && r.getDepreciationMethod() != a.getDepreciationMethod()) changed.add("method");
        if (r.getDepreciationConvention() != null && r.getDepreciationConvention() != a.getDepreciationConvention()) changed.add("convention");
        if (r.getAcquisitionSource() != null && r.getAcquisitionSource() != a.getAcquisitionSource()) changed.add("acquisition source");
        if (r.getOpeningAccumulatedDepreciation() != null && !same(r.getOpeningAccumulatedDepreciation(), a.getOpeningAccumulatedDepreciation())) {
            changed.add("depreciation brought forward");
        }
        if (changedAccount(r.getCostAccountId(), a.getCostAccount()) || changedAccount(r.getAccumulatedDepreciationAccountId(), a.getAccumulatedDepreciationAccount())
                || changedAccount(r.getDepreciationExpenseAccountId(), a.getDepreciationExpenseAccount())
                || changedAccount(r.getAccumulatedImpairmentAccountId(), a.getAccumulatedImpairmentAccount())
                || changedAccount(r.getImpairmentLossAccountId(), a.getImpairmentLossAccount())
                || changedAccount(r.getDisposalGainAccountId(), a.getDisposalGainAccount())
                || changedAccount(r.getDisposalLossAccountId(), a.getDisposalLossAccount())) {
            changed.add("GL accounts");
        }
        if (!changed.isEmpty()) {
            throw new BusinessException(a.getAssetNumber() + " is on the books, so its " + String.join(", ", changed)
                    + " can't be edited. Use Change Estimates for life, residual value and method, or reverse the acquisition to return it to draft.");
        }
    }

    private void validateEstimates(DepreciationMethod method, Integer life, BigDecimal residual, BigDecimal rate, BigDecimal cost) {
        if (method == null) throw new BusinessException("A depreciation method is required");
        if (engine.requiresUsefulLife(method) && method != DepreciationMethod.REDUCING_BALANCE && (life == null || life <= 0)) {
            throw new BusinessException("A useful life (in months) is required for " + method.getLabel().toLowerCase());
        }
        if (method == DepreciationMethod.REDUCING_BALANCE && (life == null || life <= 0) && (rate == null || rate.signum() <= 0)) {
            throw new BusinessException("Reducing balance needs a useful life or an annual rate");
        }
        if (life != null && (life <= 0 || life > MAX_LIFE_MONTHS)) {
            throw new BusinessException("Useful life must be between 1 and " + MAX_LIFE_MONTHS + " months");
        }
        if (residual == null || residual.signum() < 0) throw new BusinessException("Residual value can't be negative");
        if (cost != null && method != DepreciationMethod.NOT_DEPRECIATED && residual.compareTo(cost) >= 0) {
            throw new BusinessException("Residual value must be less than cost (" + cost + ")");
        }
        if (rate != null && (rate.signum() <= 0 || rate.compareTo(new BigDecimal("100")) > 0)) {
            throw new BusinessException("The annual rate must be above 0% and at most 100%");
        }
    }

    private void validateForActivation(FixedAsset asset) {
        if (asset.getAvailableForUseDate() == null) {
            throw new BusinessException("Set the date the asset is available for use before activating it");
        }
        if (asset.getAvailableForUseDate().isAfter(LocalDate.now().plusYears(1))) {
            throw new BusinessException("The date available for use is more than a year away");
        }
        if (!Boolean.TRUE.equals(asset.getCategory().getActive())) {
            throw new BusinessException("Asset category \"" + asset.getCategory().getName() + "\" is inactive");
        }
        validateEstimates(asset.getDepreciationMethod(), asset.getUsefulLifeMonths(), asset.getResidualValue(),
                asset.getAnnualRate(), asset.getAcquisitionCost());
    }

    /** Resolves every account once and stores it on the asset, so later setup changes never move it. */
    private void fixAccounts(FixedAsset a) {
        a.setCostAccount(rules.resolve(a, Role.COST));
        a.setAccumulatedDepreciationAccount(rules.resolve(a, Role.ACCUMULATED_DEPRECIATION));
        a.setDepreciationExpenseAccount(rules.resolve(a, Role.DEPRECIATION_EXPENSE));
        a.setAccumulatedImpairmentAccount(rules.resolve(a, Role.ACCUMULATED_IMPAIRMENT));
        a.setImpairmentLossAccount(rules.resolve(a, Role.IMPAIRMENT_LOSS));
        a.setDisposalGainAccount(rules.resolve(a, Role.DISPOSAL_GAIN));
        a.setDisposalLossAccount(rules.resolve(a, Role.DISPOSAL_LOSS));
        rules.assertDistinct(a.getCostAccount(), a.getAccumulatedDepreciationAccount(), a.getAccumulatedImpairmentAccount());
    }

    /** An ALREADY_IN_LEDGER source journal must actually debit the cost account with at least the cost. */
    private void assertCostAlreadyInLedger(FixedAsset asset) {
        JournalEntry j = asset.getSourceJournal();
        if (j == null) return;
        BigDecimal debited = j.getLines().stream()
                .filter(l -> l.getAccount() != null && l.getAccount().getId().equals(asset.getCostAccount().getId()))
                .map(JournalLine::getDebitAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (debited.compareTo(asset.getAcquisitionCost()) < 0) {
            throw new BusinessException("Journal " + j.getJournalNumber() + " debits " + debited + " to "
                    + FixedAssetAccountRules.describe(asset.getCostAccount()) + ", less than the asset's cost of "
                    + asset.getAcquisitionCost() + ".");
        }
    }

    /** Postings after depreciation must not predate what depreciation already covers. */
    private void assertChronology(FixedAsset asset, LocalDate date, String what) {
        if (date.isBefore(asset.getAcquisitionDate())) {
            throw new BusinessException("The " + what + " date can't be before the acquisition date");
        }
        if (asset.getDepreciatedThrough() != null && date.isBefore(asset.getDepreciatedThrough())) {
            throw new BusinessException("Depreciation is posted through " + asset.getDepreciatedThrough()
                    + ". Date the " + what + " on or after that, or reverse the later depreciation first.");
        }
        for (AssetTransaction t : transactionRepository.findLiveNewestFirst(asset.getId())) {
            if (t.getTransactionDate().isAfter(date)) {
                throw new BusinessException("There is a " + t.getType().getLabel().toLowerCase() + " dated "
                        + t.getTransactionDate() + ", after the " + what + " date");
            }
            break;
        }
    }

    private AccountEntity bankGlAccount(BankAccount bank) {
        String code = bank != null ? bank.getGlAccountCode() : null;
        if (code == null) {
            AccountEntity mapped = rules.byCode(rulesMapped(), "Bank");
            return rules.validateProceeds(mapped);
        }
        return rules.validateProceeds(rules.byCode(code, "Bank"));
    }

    private String rulesMapped() {
        return accountingMappingService.resolve(MappingKey.FIXED_ASSET_BANK_ACCOUNT);
    }

    private AccountEntity override(Long accountId, Role role) {
        return accountId == null ? null : rules.validate(rules.byId(accountId, role.getLabel()), role);
    }

    private String reference(FixedAsset asset, String kind) {
        long n = transactionRepository.findByAssetIdOrderByTransactionDateAscIdAsc(asset.getId() == null ? -1L : asset.getId()).stream()
                .filter(t -> t.getJournal() != null).count() + 1;
        return asset.getAssetNumber() + "-" + kind + n;
    }

    private BigDecimal currentPeriodDepreciation(FixedAsset a) {
        if (a.getId() == null || a.getStatus() == FixedAssetStatus.DRAFT) return BigDecimal.ZERO;
        LocalDate today = LocalDate.now();
        LocalDate from;
        LocalDate to;
        var period = periodRepository.findByDateInRange(today);
        if (period.isPresent()) {
            from = period.get().getStartDate();
            to = period.get().getEndDate();
        } else {
            from = today.withDayOfMonth(1);
            to = YearMonth.from(today).atEndOfMonth();
        }
        return transactionRepository.findByAssetIdOrderByTransactionDateAscIdAsc(a.getId()).stream()
                .filter(t -> (t.getType() == AssetTransactionType.DEPRECIATION || t.getMovementType() == AssetTransactionType.DEPRECIATION))
                .filter(t -> !t.getTransactionDate().isBefore(from) && !t.getTransactionDate().isAfter(to))
                .map(AssetTransaction::getDepreciationAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static ScheduleLineResponse scheduleRow(String status, LocalDate from, LocalDate to, BigDecimal opening, BigDecimal amount,
                                                    BigDecimal accumulated, BigDecimal months, AssetTransaction t, String explanation) {
        ScheduleLineResponse row = new ScheduleLineResponse();
        row.setStatus(status);
        row.setFromDate(from);
        row.setToDate(to);
        row.setOpeningCarryingAmount(opening);
        row.setDepreciationAmount(amount);
        row.setAccumulatedDepreciation(accumulated);
        if (opening != null) row.setClosingCarryingAmount(opening.subtract(amount));
        row.setMonthsCharged(months);
        if (t != null && t.getJournal() != null) {
            row.setJournalId(t.getJournal().getId());
            row.setJournalNumber(t.getJournal().getJournalNumber());
        }
        row.setExplanation(explanation);
        return row;
    }

    static JournalPreviewLine previewLine(CreateJournalLineRequest l) {
        JournalPreviewLine p = new JournalPreviewLine();
        p.setAccountCode(String.valueOf(l.getAccountId()));
        p.setDescription(l.getDescription());
        p.setDebit(l.getDebitAmount());
        p.setCredit(l.getCreditAmount());
        return p;
    }

    private static Specification<FixedAsset> spec(AssetSearchCriteria c) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.isFalse(root.get("deleted")));
            if (c != null) {
                if (c.search() != null && !c.search().isBlank()) {
                    String like = "%" + c.search().trim().toLowerCase() + "%";
                    p.add(cb.or(
                            cb.like(cb.lower(root.get("assetNumber")), like),
                            cb.like(cb.lower(root.get("name")), like),
                            cb.like(cb.lower(cb.coalesce(root.get("serialNumber"), "")), like)));
                }
                if (c.categoryId() != null) p.add(cb.equal(root.get("category").get("id"), c.categoryId()));
                if (c.status() != null) p.add(cb.equal(root.get("status"), c.status()));
                if (c.locationId() != null) p.add(cb.equal(root.get("location").get("id"), c.locationId()));
                if (c.departmentId() != null) p.add(cb.equal(root.get("department").get("id"), c.departmentId()));
                if (c.acquiredFrom() != null) p.add(cb.greaterThanOrEqualTo(root.get("acquisitionDate"), c.acquiredFrom()));
                if (c.acquiredTo() != null) p.add(cb.lessThanOrEqualTo(root.get("acquisitionDate"), c.acquiredTo()));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    static String supplierName(Supplier s) {
        return s.getDisplayName() != null && !s.getDisplayName().isBlank() ? s.getDisplayName() : s.getCompanyName();
    }

    private static boolean changedAccount(Long requested, AccountEntity current) {
        return requested != null && (current == null || !requested.equals(current.getId()));
    }

    private static String requireReason(String reason) {
        if (reason == null || reason.isBlank()) throw new BusinessException("A reason is required");
        if (reason.trim().length() > 500) throw new BusinessException("A reason can be at most 500 characters");
        return reason.trim();
    }

    private static boolean same(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return a == b;
        return a.compareTo(b) == 0;
    }

    private static BigDecimal money(BigDecimal v) {
        return v == null ? null : v.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
