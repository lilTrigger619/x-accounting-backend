package com.unionsg.xaccounting.service.banking;

import com.unionsg.xaccounting.MapperLayer.BankTransferMapper;
import com.unionsg.xaccounting.dto.FileResponseDto;
import com.unionsg.xaccounting.dto.FileUploadRequestDto;
import com.unionsg.xaccounting.dto.banking.BankTransferAccountSummary;
import com.unionsg.xaccounting.dto.banking.BankTransferAccountingLine;
import com.unionsg.xaccounting.dto.banking.BankTransferActivityResponse;
import com.unionsg.xaccounting.dto.banking.BankTransferFilter;
import com.unionsg.xaccounting.dto.banking.BankTransferJournalResponse;
import com.unionsg.xaccounting.dto.banking.BankTransferListItemResponse;
import com.unionsg.xaccounting.dto.banking.BankTransferPreviewResponse;
import com.unionsg.xaccounting.dto.banking.BankTransferResponse;
import com.unionsg.xaccounting.dto.banking.SaveBankTransferRequest;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.entity.banking.BankTransfer;
import com.unionsg.xaccounting.entity.banking.BankTransferActivity;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.enums.DocumentModule;
import com.unionsg.xaccounting.enums.EntityType;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.enums.banking.BankTransferAction;
import com.unionsg.xaccounting.enums.banking.BankTransferStatus;
import com.unionsg.xaccounting.enums.settings.BankAccountStatus;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.banking.BankTransferActivityRepository;
import com.unionsg.xaccounting.repository.banking.BankTransferRepository;
import com.unionsg.xaccounting.repository.journal.JournalLineRepository;
import com.unionsg.xaccounting.repository.settings.BankAccountRepository;
import com.unionsg.xaccounting.security.util.SecurityUtils;
import com.unionsg.xaccounting.service.DocumentNumberService;
import com.unionsg.xaccounting.service.FileService.FileService;
import com.unionsg.xaccounting.service.journal.JournalService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Bank transfer lifecycle: DRAFT (editable) -> POSTED (read-only, journal generated) ->
 * REVERSED (reversal journal posted), or DRAFT -> CANCELLED.
 *
 * <p>Double posting is prevented three ways: post/cancel/reverse take a row lock on the
 * transfer and re-check its status under it, the journal reference (the transfer number) is
 * unique, and the transfer's {@code journal_id} is unique. Draft edits carry the version the
 * client loaded and a stale one is refused.</p>
 */
@Service
@RequiredArgsConstructor
public class BankTransferService {

    static final Set<JournalStatus> BALANCE_STATUSES = Set.of(JournalStatus.POSTED, JournalStatus.REVERSED);
    private static final Map<String, String> SORTABLE = Map.of(
            "transferNumber", "transferNumber",
            "transferDate", "transferDate",
            "valueDate", "valueDate",
            "amount", "amount",
            "status", "status",
            "reference", "reference",
            "createdAt", "createdAt"
    );

    private final BankTransferRepository repository;
    private final BankTransferActivityRepository activityRepository;
    private final BankAccountRepository bankAccountRepository;
    private final AccountRepository accountRepository;
    private final JournalLineRepository journalLineRepository;
    private final DocumentNumberService documentNumberService;
    private final BankTransferJournalService transferJournalService;
    private final JournalService journalService;
    private final BaseCurrencyService baseCurrencyService;
    private final FileService fileService;
    private final BankTransferMapper mapper;

    // =====================================================================
    // Create / update
    // =====================================================================

    @Transactional
    public BankTransferResponse create(SaveBankTransferRequest request) {
        BankTransfer transfer = new BankTransfer();
        apply(transfer, request);
        transfer.setStatus(BankTransferStatus.DRAFT);
        transfer.setTransferNumber(documentNumberService.generateNextNumber(DocumentModule.BANK_TRANSFER));
        BankTransfer saved = repository.saveAndFlush(transfer);
        record(saved, BankTransferAction.CREATED, null, BankTransferStatus.DRAFT,
                "Draft created for " + saved.getSourceCurrency() + " " + saved.getAmount().toPlainString()
                        + " from " + saved.getSourceBankAccount().getAccountName()
                        + " to " + saved.getDestinationBankAccount().getAccountName());
        return toDetail(saved);
    }

    @Transactional
    public BankTransferResponse update(Long id, SaveBankTransferRequest request) {
        BankTransfer transfer = lock(id);
        if (transfer.getStatus() != BankTransferStatus.DRAFT) {
            throw new BusinessException("Transfer " + transfer.getTransferNumber() + " is "
                    + transfer.getStatus().name().toLowerCase(Locale.ROOT) + " and can no longer be edited");
        }
        if (request.getVersion() != null && !Objects.equals(request.getVersion(), transfer.getVersion())) {
            throw new BusinessException("Transfer " + transfer.getTransferNumber()
                    + " was changed by someone else. Reload it and try again.");
        }
        String before = summarize(transfer);
        apply(transfer, request);
        BankTransfer saved = repository.saveAndFlush(transfer);
        String after = summarize(saved);
        record(saved, BankTransferAction.UPDATED, BankTransferStatus.DRAFT, BankTransferStatus.DRAFT,
                before.equals(after) ? "Draft saved with no changes to amounts or accounts" : "Changed from " + before + " to " + after);
        return toDetail(saved);
    }

    /** What the given form values would post, without saving. */
    @Transactional
    public BankTransferPreviewResponse preview(SaveBankTransferRequest request) {
        BankTransfer transfer = new BankTransfer();
        apply(transfer, request);
        List<BankTransferAccountingLine> lines = transferJournalService.buildLines(transfer);
        BigDecimal available = availableBalance(transfer.getSourceBankAccount());
        boolean overdraft = Boolean.TRUE.equals(transfer.getSourceBankAccount().getAllowOverdraft());
        BigDecimal required = transfer.getBaseAmount().add(transfer.getBaseFeeAmount());
        return BankTransferPreviewResponse.builder()
                .baseCurrency(transfer.getBaseCurrency())
                .sourceCurrency(transfer.getSourceCurrency())
                .destinationCurrency(transfer.getDestinationCurrency())
                .exchangeRate(transfer.getExchangeRate())
                .convertedAmount(transfer.getConvertedAmount())
                .sourceBaseRate(transfer.getSourceBaseRate())
                .destinationBaseRate(transfer.getDestinationBaseRate())
                .baseAmount(transfer.getBaseAmount())
                .baseConvertedAmount(transfer.getBaseConvertedAmount())
                .baseFeeAmount(transfer.getBaseFeeAmount())
                .exchangeGainLoss(transfer.getExchangeGainLoss())
                .lines(lines)
                .totalDebit(lines.stream().map(BankTransferAccountingLine::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add))
                .totalCredit(lines.stream().map(BankTransferAccountingLine::getCredit).reduce(BigDecimal.ZERO, BigDecimal::add))
                .sourceAvailableBalance(available)
                .sufficientBalance(available.compareTo(required) >= 0)
                .sourceAllowsOverdraft(overdraft)
                .build();
    }

    // =====================================================================
    // Read
    // =====================================================================

    /** Not read-only: a draft's accounting preview may seed a default account mapping row. */
    @Transactional
    public BankTransferResponse get(Long id) {
        return toDetail(find(id));
    }

    @Transactional(readOnly = true)
    public Page<BankTransferListItemResponse> list(BankTransferFilter filter, Pageable pageable) {
        return repository.findAll(specification(filter == null ? new BankTransferFilter() : filter), sanitize(pageable))
                .map(mapper::toListItem);
    }

    @Transactional(readOnly = true)
    public BankTransferJournalResponse getJournal(Long id) {
        BankTransfer transfer = find(id);
        if (transfer.getJournal() == null) {
            throw new ResourceNotFoundException("Transfer " + transfer.getTransferNumber()
                    + " has not been posted, so it has no journal yet");
        }
        return BankTransferJournalResponse.builder()
                .journal(journalService.getById(transfer.getJournal().getId()))
                .reversalJournal(transfer.getReversalJournal() != null
                        ? journalService.getById(transfer.getReversalJournal().getId()) : null)
                .build();
    }

    @Transactional(readOnly = true)
    public List<BankTransferActivityResponse> getActivity(Long id) {
        find(id);
        return activityRepository.findByBankTransferIdOrderByCreatedAtDescIdDesc(id).stream()
                .map(mapper::toActivity)
                .toList();
    }

    /** Active bank accounts a transfer can use, with their current GL balance. */
    @Transactional(readOnly = true)
    public List<BankTransferAccountSummary> listTransferAccounts() {
        return bankAccountRepository.findByStatus(BankAccountStatus.ACTIVE).stream()
                .filter(account -> !Boolean.TRUE.equals(account.getDeleted()))
                .sorted(Comparator.comparing(BankAccount::getAccountName, String.CASE_INSENSITIVE_ORDER))
                .map(account -> mapper.toAccountSummary(account, availableBalance(account)))
                .toList();
    }

    public String baseCurrency() {
        return baseCurrencyService.resolve();
    }

    // =====================================================================
    // Lifecycle
    // =====================================================================

    @Transactional
    public BankTransferResponse post(Long id) {
        BankTransfer transfer = lock(id);
        if (transfer.getStatus() == BankTransferStatus.POSTED || transfer.getJournal() != null) {
            throw new BusinessException("Transfer " + transfer.getTransferNumber() + " has already been posted");
        }
        if (transfer.getStatus() != BankTransferStatus.DRAFT) {
            throw new BusinessException("Transfer " + transfer.getTransferNumber() + " is "
                    + transfer.getStatus().name().toLowerCase(Locale.ROOT) + " and cannot be posted");
        }

        BankAccount source = requireUsableAccount(transfer.getSourceBankAccount().getId(), "Source");
        BankAccount destination = requireUsableAccount(transfer.getDestinationBankAccount().getId(), "Destination");
        assertDistinct(source, destination);
        assertCurrencyUnchanged(source, transfer.getSourceCurrency(), "source");
        assertCurrencyUnchanged(destination, transfer.getDestinationCurrency(), "destination");

        // Serialises postings that draw on the same account, so two transfers can't both pass
        // the balance check against the same funds.
        BankAccount lockedSource = bankAccountRepository.findByIdForUpdate(source.getId())
                .orElseThrow(() -> new BusinessException("Source account not found"));
        assertSufficientBalance(lockedSource, transfer);

        JournalEntry journal = transferJournalService.postTransferJournal(transfer);

        transfer.setJournal(journal);
        transfer.setStatus(BankTransferStatus.POSTED);
        transfer.setPostedAt(LocalDateTime.now());
        transfer.setPostedBy(currentUserName());
        BankTransfer saved = repository.saveAndFlush(transfer);
        record(saved, BankTransferAction.POSTED, BankTransferStatus.DRAFT, BankTransferStatus.POSTED,
                "Posted as journal " + journal.getJournalNumber());
        return toDetail(saved);
    }

    @Transactional
    public BankTransferResponse cancel(Long id, String reason) {
        BankTransfer transfer = lock(id);
        if (transfer.getStatus() != BankTransferStatus.DRAFT) {
            throw new BusinessException("Only draft transfers can be cancelled; transfer "
                    + transfer.getTransferNumber() + " is " + transfer.getStatus().name().toLowerCase(Locale.ROOT)
                    + (transfer.getStatus() == BankTransferStatus.POSTED ? ". Reverse it instead." : ""));
        }
        transfer.setStatus(BankTransferStatus.CANCELLED);
        transfer.setCancelledAt(LocalDateTime.now());
        transfer.setCancelledBy(currentUserName());
        transfer.setCancellationReason(blankToNull(reason));
        BankTransfer saved = repository.saveAndFlush(transfer);
        record(saved, BankTransferAction.CANCELLED, BankTransferStatus.DRAFT, BankTransferStatus.CANCELLED,
                blankToNull(reason) != null ? "Reason: " + reason.trim() : "Cancelled");
        return toDetail(saved);
    }

    @Transactional
    public BankTransferResponse reverse(Long id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("A reason is required to reverse a transfer");
        }
        BankTransfer transfer = lock(id);
        if (transfer.getStatus() == BankTransferStatus.REVERSED) {
            throw new BusinessException("Transfer " + transfer.getTransferNumber() + " has already been reversed");
        }
        if (transfer.getStatus() != BankTransferStatus.POSTED) {
            throw new BusinessException("Only posted transfers can be reversed; transfer "
                    + transfer.getTransferNumber() + " is " + transfer.getStatus().name().toLowerCase(Locale.ROOT));
        }

        JournalEntry reversal = transferJournalService.reverseTransferJournal(transfer, reason.trim());

        transfer.setReversalJournal(reversal);
        transfer.setStatus(BankTransferStatus.REVERSED);
        transfer.setReversedAt(LocalDateTime.now());
        transfer.setReversedBy(currentUserName());
        transfer.setReversalReason(reason.trim());
        BankTransfer saved = repository.saveAndFlush(transfer);
        record(saved, BankTransferAction.REVERSED, BankTransferStatus.POSTED, BankTransferStatus.REVERSED,
                "Reversal journal " + reversal.getJournalNumber() + " posted. Reason: " + reason.trim());
        return toDetail(saved);
    }

    // =====================================================================
    // Attachments (generic files table, entity_type = BANK_TRANSFER)
    // =====================================================================

    @Transactional(readOnly = true)
    public List<FileResponseDto> listAttachments(Long id) {
        find(id);
        return fileService.getFiles(EntityType.BANK_TRANSFER, id.toString(), null, PageRequest.of(0, 200))
                .getContent();
    }

    @Transactional
    public List<FileResponseDto> addAttachments(Long id, MultipartFile[] files, String description) {
        BankTransfer transfer = find(id);
        if (transfer.getStatus() == BankTransferStatus.CANCELLED) {
            throw new BusinessException("Files cannot be attached to a cancelled transfer");
        }
        if (files == null || files.length == 0) {
            throw new BusinessException("Choose at least one file to attach");
        }
        User user = SecurityUtils.getCurrentUser();
        FileUploadRequestDto request = FileUploadRequestDto.builder()
                .entityType(EntityType.BANK_TRANSFER)
                .entityId(id.toString())
                .description(blankToNull(description))
                .uploadedBy(user != null ? user.getId() : null)
                .build();
        List<FileResponseDto> uploaded = fileService.uploadFile(files, request);
        for (FileResponseDto file : uploaded) {
            record(transfer, BankTransferAction.ATTACHMENT_ADDED, transfer.getStatus(), transfer.getStatus(),
                    "Attached " + file.getOriginalName());
        }
        return uploaded;
    }

    @Transactional(readOnly = true)
    public FileResponseDto getAttachment(Long id, String fileId) {
        find(id);
        FileResponseDto file = fileService.getFile(fileId);
        if (file.getEntityType() != EntityType.BANK_TRANSFER || !id.toString().equals(file.getEntityId())) {
            throw new ResourceNotFoundException("Attachment not found on this transfer");
        }
        return file;
    }

    @Transactional
    public void removeAttachment(Long id, String fileId) {
        BankTransfer transfer = find(id);
        if (transfer.getStatus() != BankTransferStatus.DRAFT) {
            throw new BusinessException("Attachments can only be removed while the transfer is a draft");
        }
        FileResponseDto file = getAttachment(id, fileId);
        fileService.deleteFile(fileId);
        record(transfer, BankTransferAction.ATTACHMENT_REMOVED, transfer.getStatus(), transfer.getStatus(),
                "Removed " + file.getOriginalName());
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private void apply(BankTransfer transfer, SaveBankTransferRequest request) {
        if (request.getSourceBankAccountId() == null) {
            throw new BusinessException("Source account is required");
        }
        if (request.getDestinationBankAccountId() == null) {
            throw new BusinessException("Destination account is required");
        }
        if (request.getTransferDate() == null) {
            throw new BusinessException("Transfer date is required");
        }
        if (request.getValueDate() != null && request.getValueDate().isBefore(request.getTransferDate())) {
            throw new BusinessException("Value date cannot be before the transfer date");
        }
        if (Objects.equals(request.getSourceBankAccountId(), request.getDestinationBankAccountId())) {
            throw new BusinessException("Source and destination accounts must be different");
        }

        BankAccount source = requireUsableAccount(request.getSourceBankAccountId(), "Source");
        BankAccount destination = requireUsableAccount(request.getDestinationBankAccountId(), "Destination");
        assertDistinct(source, destination);

        AccountEntity feeAccount = null;
        String feeCode = blankToNull(request.getFeeAccountCode());
        if (feeCode != null) {
            feeAccount = accountRepository.findByAccountId(feeCode)
                    .orElseThrow(() -> new BusinessException("Charges account " + feeCode
                            + " does not exist in the Chart of Accounts"));
        }

        String baseCurrency = baseCurrencyService.resolve();
        String sourceCurrency = currencyOf(source, baseCurrency);
        String destinationCurrency = currencyOf(destination, baseCurrency);

        BankTransferCalculator.Result amounts = BankTransferCalculator.calculate(new BankTransferCalculator.Input(
                request.getAmount(),
                request.getFeeAmount(),
                sourceCurrency,
                destinationCurrency,
                baseCurrency,
                request.getExchangeRate(),
                request.getSourceBaseRate(),
                request.getDestinationBaseRate()
        ));

        transfer.setSourceBankAccount(source);
        transfer.setDestinationBankAccount(destination);
        transfer.setTransferDate(request.getTransferDate());
        transfer.setValueDate(request.getValueDate());
        transfer.setReference(blankToNull(request.getReference()));
        transfer.setDescription(blankToNull(request.getDescription()));
        transfer.setFeeAccount(feeAccount);
        transfer.setBaseCurrency(baseCurrency);
        transfer.setSourceCurrency(sourceCurrency);
        transfer.setDestinationCurrency(destinationCurrency);
        transfer.setAmount(amounts.amount());
        transfer.setFeeAmount(amounts.feeAmount());
        transfer.setExchangeRate(amounts.exchangeRate());
        transfer.setConvertedAmount(amounts.convertedAmount());
        transfer.setSourceBaseRate(amounts.sourceBaseRate());
        transfer.setDestinationBaseRate(amounts.destinationBaseRate());
        transfer.setBaseAmount(amounts.baseAmount());
        transfer.setBaseConvertedAmount(amounts.baseConvertedAmount());
        transfer.setBaseFeeAmount(amounts.baseFeeAmount());
        transfer.setExchangeGainLoss(amounts.exchangeGainLoss());
    }

    private BankAccount requireUsableAccount(Long id, String label) {
        BankAccount account = bankAccountRepository.findById(id)
                .filter(a -> !Boolean.TRUE.equals(a.getDeleted()))
                .orElseThrow(() -> new BusinessException(label + " account not found: " + id));
        if (account.getStatus() != BankAccountStatus.ACTIVE) {
            throw new BusinessException(label + " account \"" + account.getAccountName() + "\" is inactive");
        }
        if (account.getGlAccountCode() == null || !accountRepository.existsByAccountId(account.getGlAccountCode())) {
            throw new BusinessException(label + " account \"" + account.getAccountName()
                    + "\" is not linked to a valid Chart of Accounts code");
        }
        return account;
    }

    private void assertDistinct(BankAccount source, BankAccount destination) {
        if (Objects.equals(source.getId(), destination.getId())) {
            throw new BusinessException("Source and destination accounts must be different");
        }
        if (source.getGlAccountCode().equals(destination.getGlAccountCode())) {
            throw new BusinessException("\"" + source.getAccountName() + "\" and \"" + destination.getAccountName()
                    + "\" post to the same GL account (" + source.getGlAccountCode()
                    + "), so a transfer between them would have no accounting effect");
        }
    }

    private void assertCurrencyUnchanged(BankAccount account, String expected, String side) {
        String current = currencyOf(account, baseCurrencyService.resolve());
        if (!current.equals(expected)) {
            throw new BusinessException("The " + side + " account's currency changed from " + expected + " to "
                    + current + " after this draft was saved. Edit and save the transfer again before posting.");
        }
    }

    private void assertSufficientBalance(BankAccount source, BankTransfer transfer) {
        if (Boolean.TRUE.equals(source.getAllowOverdraft())) {
            return;
        }
        BigDecimal required = transfer.getBaseAmount().add(transfer.getBaseFeeAmount());
        BigDecimal available = availableBalance(source);
        if (available.compareTo(required) < 0) {
            throw new BusinessException("Insufficient balance in \"" + source.getAccountName() + "\": available "
                    + transfer.getBaseCurrency() + " " + available.toPlainString() + ", this transfer needs "
                    + transfer.getBaseCurrency() + " " + required.toPlainString()
                    + " including charges. Allow overdraft on the bank account to post it anyway.");
        }
    }

    BigDecimal availableBalance(BankAccount account) {
        BigDecimal balance = journalLineRepository.sumNetMovementByAccountCode(account.getGlAccountCode(), BALANCE_STATUSES);
        return balance == null ? BigDecimal.ZERO : balance.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private static String currencyOf(BankAccount account, String baseCurrency) {
        String currency = account.getCurrency();
        return currency == null || currency.isBlank() ? baseCurrency : currency.trim().toUpperCase(Locale.ROOT);
    }

    private BankTransfer find(Long id) {
        return repository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bank transfer not found: " + id));
    }

    private BankTransfer lock(Long id) {
        return repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bank transfer not found: " + id));
    }

    private BankTransferResponse toDetail(BankTransfer transfer) {
        List<BankTransferAccountingLine> lines;
        String previewError = null;
        if (transfer.getJournal() != null) {
            lines = transfer.getJournal().getLines().stream().map(mapper::toAccountingLine).toList();
        } else if (transfer.getStatus() == BankTransferStatus.DRAFT) {
            try {
                lines = transferJournalService.buildLines(transfer);
            } catch (BusinessException e) {
                lines = List.of();
                previewError = e.getMessage();
            }
        } else {
            lines = List.of();
        }
        return mapper.toResponse(transfer, lines, previewError);
    }

    private void record(BankTransfer transfer, BankTransferAction action,
                        BankTransferStatus from, BankTransferStatus to, String details) {
        BankTransferActivity activity = new BankTransferActivity();
        activity.setBankTransfer(transfer);
        activity.setAction(action);
        activity.setFromStatus(from);
        activity.setToStatus(to);
        activity.setDetails(details != null && details.length() > 1000 ? details.substring(0, 1000) : details);
        activityRepository.save(activity);
    }

    private static String summarize(BankTransfer t) {
        return t.getSourceBankAccount().getAccountName() + " -> " + t.getDestinationBankAccount().getAccountName()
                + ", " + t.getSourceCurrency() + " " + t.getAmount().toPlainString()
                + (t.getFeeAmount().signum() > 0 ? " + charges " + t.getFeeAmount().toPlainString() : "")
                + (t.getSourceCurrency().equals(t.getDestinationCurrency()) ? ""
                        : " at " + t.getExchangeRate().stripTrailingZeros().toPlainString())
                + ", dated " + t.getTransferDate()
                + (t.getReference() != null ? ", ref " + t.getReference() : "");
    }

    private static String currentUserName() {
        User user = SecurityUtils.getCurrentUser();
        if (user == null) {
            return "system";
        }
        String first = user.getFirstName() != null ? user.getFirstName() : "";
        String last = user.getLastName() != null ? user.getLastName() : "";
        String name = (first + " " + last).trim();
        return name.isEmpty() ? user.getEmail() : name;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Pageable sanitize(Pageable pageable) {
        int page = pageable == null ? 0 : Math.max(pageable.getPageNumber(), 0);
        int size = pageable == null ? 20 : Math.min(Math.max(pageable.getPageSize(), 1), 200);
        List<Sort.Order> orders = new ArrayList<>();
        if (pageable != null) {
            for (Sort.Order order : pageable.getSort()) {
                String property = SORTABLE.get(order.getProperty());
                if (property != null) {
                    orders.add(new Sort.Order(order.getDirection(), property));
                }
            }
        }
        if (orders.isEmpty()) {
            orders.add(Sort.Order.desc("transferDate"));
        }
        orders.add(Sort.Order.desc("id"));
        return PageRequest.of(page, size, Sort.by(orders));
    }

    static Specification<BankTransfer> specification(BankTransferFilter f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));
            if (f.getSearch() != null && !f.getSearch().isBlank()) {
                String like = "%" + f.getSearch().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("transferNumber")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("reference"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("description"), "")), like)
                ));
            }
            if (f.getTransferNumber() != null && !f.getTransferNumber().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("transferNumber")),
                        "%" + f.getTransferNumber().trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (f.getReference() != null && !f.getReference().isBlank()) {
                predicates.add(cb.like(cb.lower(cb.coalesce(root.get("reference"), "")),
                        "%" + f.getReference().trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (f.getSourceBankAccountId() != null) {
                predicates.add(cb.equal(root.get("sourceBankAccount").get("id"), f.getSourceBankAccountId()));
            }
            if (f.getDestinationBankAccountId() != null) {
                predicates.add(cb.equal(root.get("destinationBankAccount").get("id"), f.getDestinationBankAccountId()));
            }
            if (f.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("transferDate"), f.getFromDate()));
            }
            if (f.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<LocalDate>get("transferDate"), f.getToDate()));
            }
            if (f.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), f.getStatus()));
            }
            if (f.getMinAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("amount"), f.getMinAmount()));
            }
            if (f.getMaxAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("amount"), f.getMaxAmount()));
            }
            if (f.getCreatedById() != null) {
                predicates.add(cb.equal(root.get("createdBy").get("id"), f.getCreatedById()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
