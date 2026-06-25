package com.unionsg.xaccounting.service.journal;

import com.unionsg.xaccounting.MapperLayer.JournalMapper;
import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.dto.journal.UpdateJournalRequest;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.enums.DocumentModule;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.exception.BadRequestException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.journal.JournalEntryRepository;
import com.unionsg.xaccounting.security.DocumentNumberGeneratorService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class JournalServiceImpl implements JournalService {

    private final JournalEntryRepository journalRepository;
    private final AccountRepository accountRepository;
    private final JournalMapper journalMapper;
    private final JournalPostingService postingService;
    private final JournalNumberGenerator numberGenerator;
    private final DocumentNumberGeneratorService generalSequenceGeneratorService;

    public JournalServiceImpl(
            JournalEntryRepository journalRepository,
            AccountRepository accountRepository,
            JournalMapper journalMapper,
            JournalPostingService postingService,
            JournalNumberGenerator numberGenerator,
            DocumentNumberGeneratorService generalSequenceGeneratorService
    ) {
        this.journalRepository = journalRepository;
        this.accountRepository = accountRepository;
        this.journalMapper = journalMapper;
        this.postingService = postingService;
        this.numberGenerator = numberGenerator;
        this.generalSequenceGeneratorService = generalSequenceGeneratorService;
    }

    @Override
    public JournalResponse create(CreateJournalRequest request) {

        JournalEntry journal = new JournalEntry();

//        journal.setJournalNumber(numberGenerator.generate());
        journal.setJournalNumber(generalSequenceGeneratorService.generate(DocumentModule.JOURNAL));
        journal.setJournalDate(request.getJournalDate());
        journal.setReference(request.getReference());
        journal.setDescription(request.getDescription());
        journal.setJournalType(request.getJournalType());
        journal.setCurrencyCode(request.getCurrencyCode());

        buildLines(journal, request.getLines());

        calculateTotals(journal);

        JournalEntry saved = journalRepository.save(journal);

        return journalMapper.toResponse(saved);
    }

    @Override
    public JournalResponse update(Long id, UpdateJournalRequest request) {

        JournalEntry journal = getEntity(id);

        if (journal.getStatus() != JournalStatus.DRAFT) {
            throw new BadRequestException("Only draft journals can be edited");
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
        return journalMapper.toResponse(getEntity(id));
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
    public List<JournalResponse> getAll() {
        return journalRepository.findAll()
                .stream()
                .map(journalMapper::toResponse)
                .toList();
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

        JournalEntry original = getEntity(id);

        if (original.getStatus() != JournalStatus.POSTED) {
            throw new BadRequestException(
                    "Only posted journals can be reversed"
            );
        }

        JournalEntry reversal = new JournalEntry();

        reversal.setJournalNumber(numberGenerator.generate());
        reversal.setJournalDate(LocalDate.now());
        reversal.setPostingDate(LocalDate.now());
        reversal.setPostedAt(LocalDateTime.now());

        reversal.setStatus(JournalStatus.POSTED);

        reversal.setReference("REV-" + original.getJournalNumber());

        reversal.setDescription(reason);

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

    private JournalEntry getEntity(Long id) {
        return journalRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Journal not found")
                );
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

    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void JournalMapper(){}
}