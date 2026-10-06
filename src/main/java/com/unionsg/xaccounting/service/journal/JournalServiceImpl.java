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

    public JournalServiceImpl(
            JournalEntryRepository journalRepository,
            AccountRepository accountRepository,
            JournalMapper journalMapper,
            JournalPostingService postingService,
            JournalNumberGenerator numberGenerator,
            DocumentNumberService generalSequenceGeneratorService,
            PeriodLockGuard periodLockGuard,
            ConfigValueValidator configValues
    ) {
        this.journalRepository = journalRepository;
        this.accountRepository = accountRepository;
        this.journalMapper = journalMapper;
        this.postingService = postingService;
        this.numberGenerator = numberGenerator;
        this.generalSequenceGeneratorService = generalSequenceGeneratorService;
        this.periodLockGuard = periodLockGuard;
        this.configValues = configValues;
    }

    @Override
    public JournalResponse create(CreateJournalRequest request) {

        JournalEntry journal = new JournalEntry();

//        journal.setJournalNumber(numberGenerator.generate());
        journal.setJournalNumber(generalSequenceGeneratorService.generateNextNumber(DocumentModule.JOURNAL));
        journal.setJournalDate(request.getJournalDate());
        journal.setReference(request.getReference());
        journal.setDescription(request.getDescription());
        journal.setJournalType(request.getJournalType());
        journal.setCurrencyCode(request.getCurrencyCode());

        buildLines(journal, request.getLines());

        calculateTotals(journal);

        validateBalanced(journal);


        JournalEntry saved = journalRepository.save(journal);


        return journalMapper.toResponse(saved);
    }

    @Override
    public JournalResponse createManualJournal(CreateJournalRequest request) {
        assertManualType(request.getJournalType());
        request.setCurrencyCode(configValues.require("currencies", request.getCurrencyCode(), null, "Currency"));
        assertNoControlAccountLines(request.getLines());
        return create(request);
    }

    @Override
    public JournalResponse update(Long id, UpdateJournalRequest request) {

        JournalEntry journal = getEntity(id);

        if (journal.getStatus() != JournalStatus.DRAFT) {
            throw new BadRequestException("Only draft journals can be edited");
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

        journal.getLines().clear();

        buildLines(journal, request.getLines());

        calculateTotals(journal);

        JournalEntry updated = journalRepository.save(journal);

        return journalMapper.toResponse(updated);
    }

    @Override
    public JournalResponse getById(Long id) {
        JournalResponse response = journalMapper.toResponse(getEntity(id));
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

        return journalMapper.toResponse(journal);
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

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return journalRepository.findAll(spec, pageable)
                .map(journalMapper::toResponse);
    }

    @Override
    public void deleteDraft(Long id) {

        JournalEntry journal = getEntity(id);

        if (journal.getStatus() != JournalStatus.DRAFT) {
            throw new BadRequestException(
                    "Only draft journals can be deleted"
            );
        }

        journalRepository.delete(journal);
    }

    @Override
    public JournalResponse post(Long id) {

        JournalEntry journal = getEntity(id);

        postingService.post(journal);

        JournalEntry saved = journalRepository.save(journal);

        return journalMapper.toResponse(saved);
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

        for (JournalLine line : original.getLines()) {

            JournalLine reversalLine = new JournalLine();

            reversalLine.setAccount(line.getAccount());

            reversalLine.setLineNumber(line.getLineNumber());

            reversalLine.setDescription(
                    "Reversal of " + original.getJournalNumber()
            );

            reversalLine.setDebitAmount(line.getCreditAmount());

            reversalLine.setCreditAmount(line.getDebitAmount());

            reversal.addLine(reversalLine);
        }

        calculateTotals(reversal);

        journalRepository.save(reversal);

        original.setStatus(JournalStatus.REVERSED);

        original.setReversedAt(LocalDateTime.now());

        journalRepository.save(original);

        return journalMapper.toResponse(reversal);
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

    private void JournalMapper(){}
}