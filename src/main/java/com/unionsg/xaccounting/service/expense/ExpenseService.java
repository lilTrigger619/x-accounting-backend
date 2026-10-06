package com.unionsg.xaccounting.service.expense;

import com.unionsg.xaccounting.MapperLayer.ExpenseMapper;
import com.unionsg.xaccounting.dto.FileResponseDto;
import com.unionsg.xaccounting.dto.FileUploadRequestDto;
import com.unionsg.xaccounting.dto.expense.ExpenseAccountingLine;
import com.unionsg.xaccounting.dto.expense.ExpenseActivityResponse;
import com.unionsg.xaccounting.dto.expense.ExpenseFilter;
import com.unionsg.xaccounting.dto.expense.ExpenseJournalResponse;
import com.unionsg.xaccounting.dto.expense.ExpenseListItemResponse;
import com.unionsg.xaccounting.dto.expense.ExpensePreviewResponse;
import com.unionsg.xaccounting.dto.expense.ExpenseResponse;
import com.unionsg.xaccounting.dto.expense.SaveExpenseLineRequest;
import com.unionsg.xaccounting.dto.expense.SaveExpenseRequest;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.entity.expense.Expense;
import com.unionsg.xaccounting.entity.expense.ExpenseActivity;
import com.unionsg.xaccounting.entity.expense.ExpenseLine;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.entity.supplier.Supplier;
import com.unionsg.xaccounting.enums.AccountType;
import com.unionsg.xaccounting.enums.CustomerStatus;
import com.unionsg.xaccounting.enums.DocumentModule;
import com.unionsg.xaccounting.enums.EntityType;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.enums.expense.ExpenseAction;
import com.unionsg.xaccounting.enums.expense.ExpenseStatus;
import com.unionsg.xaccounting.enums.settings.BankAccountStatus;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.SupplierRepository;
import com.unionsg.xaccounting.repository.expense.ExpenseActivityRepository;
import com.unionsg.xaccounting.repository.expense.ExpenseRepository;
import com.unionsg.xaccounting.repository.journal.JournalLineRepository;
import com.unionsg.xaccounting.repository.settings.BankAccountRepository;
import com.unionsg.xaccounting.security.util.SecurityUtils;
import com.unionsg.xaccounting.service.DocumentNumberService;
import com.unionsg.xaccounting.service.FileService.FileService;
import com.unionsg.xaccounting.service.banking.BaseCurrencyService;
import com.unionsg.xaccounting.service.config.ConfigValueValidator;
import com.unionsg.xaccounting.service.journal.JournalService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Expense lifecycle: DRAFT (editable, deletable) -> POSTED (read-only, journal generated) ->
 * REVERSED (reversal journal posted).
 *
 * <p>Double posting is prevented three ways: update/post/reverse/delete take a row lock on the
 * expense and re-check its status under it, the expense's {@code journal_id} is unique, and the
 * journal reference is the expense number. Draft edits carry the version the client loaded and
 * a stale one is refused.</p>
 */
@Service
@RequiredArgsConstructor
public class ExpenseService {

    static final Set<JournalStatus> BALANCE_STATUSES = Set.of(JournalStatus.POSTED, JournalStatus.REVERSED);
    private static final Map<String, String> SORTABLE = Map.of(
            "expenseNumber", "expenseNumber",
            "paymentDate", "paymentDate",
            "totalAmount", "totalAmount",
            "status", "status",
            "reference", "reference",
            "createdAt", "createdAt"
    );

    private final ExpenseRepository repository;
    private final ExpenseActivityRepository activityRepository;
    private final BankAccountRepository bankAccountRepository;
    private final AccountRepository accountRepository;
    private final SupplierRepository supplierRepository;
    private final JournalLineRepository journalLineRepository;
    private final DocumentNumberService documentNumberService;
    private final ExpenseJournalService expenseJournalService;
    private final JournalService journalService;
    private final BaseCurrencyService baseCurrencyService;
    private final ConfigValueValidator configValues;
    private final FileService fileService;
    private final ExpenseMapper mapper;

    // =====================================================================
    // Create / update / delete
    // =====================================================================

    @Transactional
    public ExpenseResponse create(SaveExpenseRequest request) {
        Expense expense = new Expense();
        apply(expense, request);
        expense.setStatus(ExpenseStatus.DRAFT);
        expense.setExpenseNumber(documentNumberService.generateNextNumber(DocumentModule.EXPENSE));
        Expense saved = repository.saveAndFlush(expense);
        record(saved, ExpenseAction.CREATED, null, ExpenseStatus.DRAFT, "Draft created: " + summarize(saved));
        return toDetail(saved);
    }

    @Transactional
    public ExpenseResponse update(Long id, SaveExpenseRequest request) {
        Expense expense = lock(id);
        assertDraft(expense, "edited");
        if (request.getVersion() != null && !Objects.equals(request.getVersion(), expense.getVersion())) {
            throw new BusinessException("Expense " + expense.getExpenseNumber()
                    + " was changed by someone else. Reload it and try again.");
        }
        String before = summarize(expense);
        apply(expense, request);
        Expense saved = repository.saveAndFlush(expense);
        String after = summarize(saved);
        record(saved, ExpenseAction.UPDATED, ExpenseStatus.DRAFT, ExpenseStatus.DRAFT,
                before.equals(after) ? "Draft saved with no changes to amounts or accounts" : "Changed from " + before + " to " + after);
        return toDetail(saved);
    }

    /** Soft-deletes a draft. Posted expenses stay on record and are reversed instead. */
    @Transactional
    public void delete(Long id) {
        Expense expense = lock(id);
        if (expense.getStatus() != ExpenseStatus.DRAFT) {
            throw new BusinessException("Expense " + expense.getExpenseNumber() + " has been posted to the ledger "
                    + "and cannot be deleted. Reverse it instead.");
        }
        expense.setDeleted(true);
        expense.setDeletedAt(LocalDateTime.now());
        expense.setDeletedBy(currentUserName());
        repository.save(expense);
    }

    /** What the given form values would post, without saving. */
    @Transactional(readOnly = true)
    public ExpensePreviewResponse preview(SaveExpenseRequest request) {
        Expense expense = new Expense();
        apply(expense, request);
        List<ExpenseAccountingLine> lines = expenseJournalService.buildLines(expense);
        BigDecimal available = availableBalance(expense.getPaymentAccount());
        return ExpensePreviewResponse.builder()
                .currency(expense.getCurrency())
                .baseCurrency(expense.getBaseCurrency())
                .exchangeRate(expense.getExchangeRate())
                .totalAmount(expense.getTotalAmount())
                .baseTotalAmount(expense.getBaseTotalAmount())
                .lines(lines)
                .totalDebit(lines.stream().map(ExpenseAccountingLine::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add))
                .totalCredit(lines.stream().map(ExpenseAccountingLine::getCredit).reduce(BigDecimal.ZERO, BigDecimal::add))
                .availableBalance(available)
                .sufficientBalance(available.compareTo(expense.getBaseTotalAmount()) >= 0)
                .allowsOverdraft(Boolean.TRUE.equals(expense.getPaymentAccount().getAllowOverdraft()))
                .build();
    }

    // =====================================================================
    // Read
    // =====================================================================

    @Transactional(readOnly = true)
    public ExpenseResponse get(Long id) {
        return toDetail(find(id));
    }

    @Transactional(readOnly = true)
    public Page<ExpenseListItemResponse> list(ExpenseFilter filter, Pageable pageable) {
        return repository.findAll(specification(filter == null ? new ExpenseFilter() : filter), sanitize(pageable))
                .map(mapper::toListItem);
    }

    @Transactional(readOnly = true)
    public ExpenseJournalResponse getJournal(Long id) {
        Expense expense = find(id);
        if (expense.getJournal() == null) {
            throw new ResourceNotFoundException("Expense " + expense.getExpenseNumber()
                    + " has not been posted, so it has no journal yet");
        }
        return ExpenseJournalResponse.builder()
                .journal(journalService.getById(expense.getJournal().getId()))
                .reversalJournal(expense.getReversalJournal() != null
                        ? journalService.getById(expense.getReversalJournal().getId()) : null)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ExpenseActivityResponse> getActivity(Long id) {
        find(id);
        return activityRepository.findByExpenseIdOrderByCreatedAtDescIdDesc(id).stream()
                .map(mapper::toActivity)
                .toList();
    }

    // =====================================================================
    // Lifecycle
    // =====================================================================

    @Transactional
    public ExpenseResponse post(Long id) {
        Expense expense = lock(id);
        if (expense.getStatus() == ExpenseStatus.POSTED || expense.getJournal() != null) {
            throw new BusinessException("Expense " + expense.getExpenseNumber() + " has already been posted");
        }
        assertDraft(expense, "posted");

        BankAccount account = requireUsableAccount(expense.getPaymentAccount().getId());
        String currency = currencyOf(account, baseCurrencyService.resolve());
        if (!currency.equals(expense.getCurrency())) {
            throw new BusinessException("The payment account's currency changed from " + expense.getCurrency() + " to "
                    + currency + " after this draft was saved. Edit and save the expense again before posting.");
        }
        for (ExpenseLine line : expense.getLines()) {
            requireExpenseAccount(line.getAccount().getId());
        }
        if (expense.getSupplier() != null) {
            requireActiveSupplier(expense.getSupplier().getId());
        }

        // Serialises postings that draw on the same account, so two payments can't both pass
        // the balance check against the same funds.
        BankAccount locked = bankAccountRepository.findByIdForUpdate(account.getId())
                .orElseThrow(() -> new BusinessException("Payment account not found"));
        assertSufficientBalance(locked, expense);

        JournalEntry journal = expenseJournalService.postExpenseJournal(expense);

        expense.setJournal(journal);
        expense.setStatus(ExpenseStatus.POSTED);
        expense.setPostedAt(LocalDateTime.now());
        expense.setPostedBy(currentUserName());
        Expense saved = repository.saveAndFlush(expense);
        record(saved, ExpenseAction.POSTED, ExpenseStatus.DRAFT, ExpenseStatus.POSTED,
                "Posted as journal " + journal.getJournalNumber());
        return toDetail(saved);
    }

    @Transactional
    public ExpenseResponse reverse(Long id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("A reason is required to reverse an expense");
        }
        Expense expense = lock(id);
        if (expense.getStatus() == ExpenseStatus.REVERSED) {
            throw new BusinessException("Expense " + expense.getExpenseNumber() + " has already been reversed");
        }
        if (expense.getStatus() != ExpenseStatus.POSTED) {
            throw new BusinessException("Only posted expenses can be reversed; expense "
                    + expense.getExpenseNumber() + " is " + expense.getStatus().name().toLowerCase(Locale.ROOT));
        }

        JournalEntry reversal = expenseJournalService.reverseExpenseJournal(expense, reason.trim());

        expense.setReversalJournal(reversal);
        expense.setStatus(ExpenseStatus.REVERSED);
        expense.setReversedAt(LocalDateTime.now());
        expense.setReversedBy(currentUserName());
        expense.setReversalReason(reason.trim());
        Expense saved = repository.saveAndFlush(expense);
        record(saved, ExpenseAction.REVERSED, ExpenseStatus.POSTED, ExpenseStatus.REVERSED,
                "Reversal journal " + reversal.getJournalNumber() + " posted. Reason: " + reason.trim());
        return toDetail(saved);
    }

    // =====================================================================
    // Attachments (generic files table, entity_type = EXPENSE)
    // =====================================================================

    @Transactional(readOnly = true)
    public List<FileResponseDto> listAttachments(Long id) {
        find(id);
        return fileService.getFiles(EntityType.EXPENSE, id.toString(), null, PageRequest.of(0, 200)).getContent();
    }

    @Transactional
    public List<FileResponseDto> addAttachments(Long id, MultipartFile[] files, String description) {
        Expense expense = find(id);
        if (files == null || files.length == 0) {
            throw new BusinessException("Choose at least one file to attach");
        }
        User user = SecurityUtils.getCurrentUser();
        FileUploadRequestDto request = FileUploadRequestDto.builder()
                .entityType(EntityType.EXPENSE)
                .entityId(id.toString())
                .description(blankToNull(description))
                .uploadedBy(user != null ? user.getId() : null)
                .build();
        List<FileResponseDto> uploaded = fileService.uploadFile(files, request);
        for (FileResponseDto file : uploaded) {
            record(expense, ExpenseAction.ATTACHMENT_ADDED, expense.getStatus(), expense.getStatus(),
                    "Attached " + file.getOriginalName());
        }
        return uploaded;
    }

    @Transactional(readOnly = true)
    public FileResponseDto getAttachment(Long id, String fileId) {
        find(id);
        FileResponseDto file = fileService.getFile(fileId);
        if (file.getEntityType() != EntityType.EXPENSE || !id.toString().equals(file.getEntityId())) {
            throw new ResourceNotFoundException("Attachment not found on this expense");
        }
        return file;
    }

    @Transactional
    public void removeAttachment(Long id, String fileId) {
        Expense expense = find(id);
        if (expense.getStatus() != ExpenseStatus.DRAFT) {
            throw new BusinessException("Attachments can only be removed while the expense is a draft");
        }
        FileResponseDto file = getAttachment(id, fileId);
        fileService.deleteFile(fileId);
        record(expense, ExpenseAction.ATTACHMENT_REMOVED, expense.getStatus(), expense.getStatus(),
                "Removed " + file.getOriginalName());
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private void apply(Expense expense, SaveExpenseRequest request) {
        if (request.getPaymentAccountId() == null) {
            throw new BusinessException("Payment account is required");
        }
        if (request.getPaymentDate() == null) {
            throw new BusinessException("Payment date is required");
        }
        if (request.getPaymentMethod() == null) {
            throw new BusinessException("Payment method is required");
        }
        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new BusinessException("Add at least one expense line");
        }

        BankAccount account = requireUsableAccount(request.getPaymentAccountId());
        Supplier supplier = request.getSupplierId() != null ? requireActiveSupplier(request.getSupplierId()) : null;

        String baseCurrency = baseCurrencyService.resolve();
        String currency = currencyOf(account, baseCurrency);
        BigDecimal rate;
        if (currency.equals(baseCurrency)) {
            rate = BigDecimal.ONE;
        } else if (request.getExchangeRate() == null || request.getExchangeRate().signum() <= 0) {
            throw new BusinessException("\"" + account.getAccountName() + "\" is in " + currency
                    + ". Enter the exchange rate: 1 " + currency + " = ? " + baseCurrency);
        } else {
            rate = request.getExchangeRate();
        }

        // Categories already on the draft stay valid even if their item was deactivated since.
        Set<String> previousCategories = expense.getLines().stream().map(ExpenseLine::getCategory).collect(Collectors.toSet());
        Map<Long, AccountEntity> accounts = new HashMap<>();
        List<ExpenseLine> lines = new ArrayList<>();
        int number = 1;
        for (SaveExpenseLineRequest lineRequest : request.getLines()) {
            String label = "Line " + number;
            if (lineRequest.getCategory() == null || lineRequest.getCategory().isBlank()) {
                throw new BusinessException(label + ": choose a category");
            }
            String category = previousCategories.contains(lineRequest.getCategory().trim())
                    ? lineRequest.getCategory().trim()
                    : configValues.require("expense-categories", lineRequest.getCategory(), null, label + " category");
            if (lineRequest.getAccountId() == null) {
                throw new BusinessException(label + ": choose an expense account");
            }
            if (lineRequest.getDescription() == null || lineRequest.getDescription().isBlank()) {
                throw new BusinessException(label + ": enter a description");
            }
            if (lineRequest.getAmount() == null || lineRequest.getAmount().compareTo(new BigDecimal("0.01")) < 0) {
                throw new BusinessException(label + ": the amount must be greater than zero");
            }
            if (lineRequest.getAmount().stripTrailingZeros().scale() > 2) {
                throw new BusinessException(label + ": amounts can have at most 2 decimal places");
            }
            AccountEntity expenseAccount = accounts.computeIfAbsent(lineRequest.getAccountId(), this::requireExpenseAccount);
            ExpenseLine line = new ExpenseLine();
            line.setExpense(expense);
            line.setLineNumber(number++);
            line.setExpenseDate(lineRequest.getExpenseDate() != null ? lineRequest.getExpenseDate() : request.getPaymentDate());
            line.setCategory(category);
            line.setAccount(expenseAccount);
            line.setDescription(lineRequest.getDescription().trim());
            line.setAmount(lineRequest.getAmount().setScale(2, RoundingMode.HALF_UP));
            line.setBaseAmount(lineRequest.getAmount().multiply(rate).setScale(2, RoundingMode.HALF_UP));
            lines.add(line);
        }

        expense.setSupplier(supplier);
        expense.setPaymentAccount(account);
        expense.setPaymentDate(request.getPaymentDate());
        expense.setPaymentMethod(request.getPaymentMethod());
        expense.setReference(blankToNull(request.getReference()));
        expense.setMemo(blankToNull(request.getMemo()));
        expense.setCurrency(currency);
        expense.setBaseCurrency(baseCurrency);
        expense.setExchangeRate(rate);
        // The credit is the sum of the rounded line debits, so the journal always balances.
        expense.setTotalAmount(lines.stream().map(ExpenseLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        expense.setBaseTotalAmount(lines.stream().map(ExpenseLine::getBaseAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        expense.getLines().clear();
        expense.getLines().addAll(lines);
    }

    private BankAccount requireUsableAccount(Long id) {
        BankAccount account = bankAccountRepository.findById(id)
                .filter(a -> !Boolean.TRUE.equals(a.getDeleted()))
                .orElseThrow(() -> new BusinessException("Payment account not found: " + id));
        if (account.getStatus() != BankAccountStatus.ACTIVE) {
            throw new BusinessException("Payment account \"" + account.getAccountName() + "\" is inactive");
        }
        if (account.getGlAccountCode() == null || !accountRepository.existsByAccountId(account.getGlAccountCode())) {
            throw new BusinessException("Payment account \"" + account.getAccountName()
                    + "\" is not linked to a valid Chart of Accounts code");
        }
        return account;
    }

    private AccountEntity requireExpenseAccount(Long id) {
        AccountEntity account = accountRepository.findById(id)
                .filter(a -> !a.isDeleted())
                .orElseThrow(() -> new BusinessException("Expense account not found: " + id));
        AccountType type = account.getCoaClearTo() != null && account.getCoaClearTo().getChartOfAccount() != null
                ? account.getCoaClearTo().getChartOfAccount().getAccountType() : null;
        if (type != AccountType.EXPENSE) {
            throw new BusinessException("\"" + account.getAccountName() + "\" (" + account.getAccountId()
                    + ") is not an expense account, so expense lines can't be booked to it");
        }
        if (Boolean.FALSE.equals(account.getIsActive())) {
            throw new BusinessException("\"" + account.getAccountName() + "\" (" + account.getAccountId() + ") is inactive");
        }
        if (Boolean.TRUE.equals(account.getIsControlAccount())) {
            throw new BusinessException("\"" + account.getAccountName() + "\" (" + account.getAccountId()
                    + ") is a control account and is only updated by the transaction that owns it");
        }
        return account;
    }

    private Supplier requireActiveSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Supplier not found: " + id));
        if (supplier.getStatus() != CustomerStatus.ACTIVE) {
            throw new BusinessException("Supplier \"" + supplier.getDisplayName() + "\" is inactive");
        }
        return supplier;
    }

    private void assertDraft(Expense expense, String verb) {
        if (expense.getStatus() != ExpenseStatus.DRAFT) {
            throw new BusinessException("Expense " + expense.getExpenseNumber() + " is "
                    + expense.getStatus().name().toLowerCase(Locale.ROOT) + " and can no longer be " + verb);
        }
    }

    private void assertSufficientBalance(BankAccount account, Expense expense) {
        if (Boolean.TRUE.equals(account.getAllowOverdraft())) {
            return;
        }
        BigDecimal available = availableBalance(account);
        if (available.compareTo(expense.getBaseTotalAmount()) < 0) {
            throw new BusinessException("Insufficient balance in \"" + account.getAccountName() + "\": available "
                    + expense.getBaseCurrency() + " " + available.toPlainString() + ", this expense needs "
                    + expense.getBaseCurrency() + " " + expense.getBaseTotalAmount().toPlainString()
                    + ". Allow overdraft on the bank account to post it anyway.");
        }
    }

    BigDecimal availableBalance(BankAccount account) {
        BigDecimal balance = journalLineRepository.sumNetMovementByAccountCode(account.getGlAccountCode(), BALANCE_STATUSES);
        return balance == null ? BigDecimal.ZERO : balance.setScale(2, RoundingMode.HALF_UP);
    }

    private static String currencyOf(BankAccount account, String baseCurrency) {
        String currency = account.getCurrency();
        return currency == null || currency.isBlank() ? baseCurrency : currency.trim().toUpperCase(Locale.ROOT);
    }

    private Expense find(Long id) {
        return repository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found: " + id));
    }

    private Expense lock(Long id) {
        return repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found: " + id));
    }

    private ExpenseResponse toDetail(Expense expense) {
        List<ExpenseAccountingLine> lines;
        String previewError = null;
        if (expense.getJournal() != null) {
            lines = expense.getJournal().getLines().stream().map(mapper::toAccountingLine).toList();
        } else {
            try {
                lines = expenseJournalService.buildLines(expense);
            } catch (BusinessException e) {
                lines = List.of();
                previewError = e.getMessage();
            }
        }
        return mapper.toResponse(expense, lines, previewError);
    }

    private void record(Expense expense, ExpenseAction action, ExpenseStatus from, ExpenseStatus to, String details) {
        ExpenseActivity activity = new ExpenseActivity();
        activity.setExpense(expense);
        activity.setAction(action);
        activity.setFromStatus(from);
        activity.setToStatus(to);
        activity.setDetails(details != null && details.length() > 1000 ? details.substring(0, 1000) : details);
        activityRepository.save(activity);
    }

    private static String summarize(Expense e) {
        return e.getCurrency() + " " + e.getTotalAmount().toPlainString()
                + " in " + e.getLines().size() + " line" + (e.getLines().size() == 1 ? "" : "s")
                + " from " + e.getPaymentAccount().getAccountName()
                + (e.getSupplier() != null ? " to " + e.getSupplier().getDisplayName() : "")
                + ", paid " + e.getPaymentDate()
                + (e.getReference() != null ? ", ref " + e.getReference() : "");
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
            orders.add(Sort.Order.desc("paymentDate"));
        }
        orders.add(Sort.Order.desc("id"));
        return PageRequest.of(page, size, Sort.by(orders));
    }

    static Specification<Expense> specification(ExpenseFilter f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));
            if (f.getSearch() != null && !f.getSearch().isBlank()) {
                String like = "%" + f.getSearch().trim().toLowerCase(Locale.ROOT) + "%";
                Join<Expense, Supplier> supplier = root.join("supplier", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("expenseNumber")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("reference"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("memo"), "")), like),
                        cb.like(cb.lower(cb.coalesce(supplier.get("displayName"), "")), like)
                ));
            }
            if (f.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), f.getStatus()));
            }
            if (f.getSupplierId() != null) {
                predicates.add(cb.equal(root.get("supplier").get("id"), f.getSupplierId()));
            }
            if (f.getPaymentAccountId() != null) {
                predicates.add(cb.equal(root.get("paymentAccount").get("id"), f.getPaymentAccountId()));
            }
            if (f.getPaymentMethod() != null) {
                predicates.add(cb.equal(root.get("paymentMethod"), f.getPaymentMethod()));
            }
            if (f.getAccountId() != null) {
                Subquery<Long> withAccount = query.subquery(Long.class);
                var line = withAccount.from(ExpenseLine.class);
                withAccount.select(line.get("expense").get("id"))
                        .where(cb.equal(line.get("account").get("id"), f.getAccountId()));
                predicates.add(root.get("id").in(withAccount));
            }
            if (f.getCategory() != null && !f.getCategory().isBlank()) {
                Subquery<Long> withCategory = query.subquery(Long.class);
                var line = withCategory.from(ExpenseLine.class);
                withCategory.select(line.get("expense").get("id"))
                        .where(cb.equal(line.get("category"), f.getCategory().trim()));
                predicates.add(root.get("id").in(withCategory));
            }
            if (f.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("paymentDate"), f.getFromDate()));
            }
            if (f.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<LocalDate>get("paymentDate"), f.getToDate()));
            }
            if (f.getMinAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("totalAmount"), f.getMinAmount()));
            }
            if (f.getMaxAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("totalAmount"), f.getMaxAmount()));
            }
            if (f.getCreatedById() != null) {
                predicates.add(cb.equal(root.get("createdBy").get("id"), f.getCreatedById()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
