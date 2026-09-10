package com.unionsg.xaccounting.service.accounting;

import com.unionsg.xaccounting.dto.accounting.CreateRecurringJournalTemplateRequest;
import com.unionsg.xaccounting.dto.accounting.RecurringJournalOccurrenceResponse;
import com.unionsg.xaccounting.dto.accounting.RecurringJournalTemplateLineRequest;
import com.unionsg.xaccounting.dto.accounting.RecurringJournalTemplateLineResponse;
import com.unionsg.xaccounting.dto.accounting.RecurringJournalTemplateResponse;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.entity.accounting.RecurringJournalOccurrence;
import com.unionsg.xaccounting.entity.accounting.RecurringJournalTemplate;
import com.unionsg.xaccounting.entity.accounting.RecurringJournalTemplateLine;
import com.unionsg.xaccounting.enums.DocumentModule;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.enums.RecurringJournalFrequency;
import com.unionsg.xaccounting.enums.RecurringJournalStatus;
import com.unionsg.xaccounting.enums.RecurringOccurrenceStatus;
import com.unionsg.xaccounting.exception.BadRequestException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.accounting.RecurringJournalOccurrenceRepository;
import com.unionsg.xaccounting.repository.accounting.RecurringJournalTemplateRepository;
import com.unionsg.xaccounting.repository.journal.JournalEntryRepository;
import com.unionsg.xaccounting.security.DocumentNumberGeneratorService;
import com.unionsg.xaccounting.service.journal.JournalPostingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages Recurring Journal templates and generates their due occurrences into real,
 * posted JournalEntries. Generation respects {@link PeriodLockGuard}: a due date that
 * falls in a locked/closed period is recorded as a PENDING occurrence and retried on
 * the next run rather than skipped or forced through.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringJournalService {

    public static final String SOURCE_MODULE = "RECURRING_JOURNAL";

    private final RecurringJournalTemplateRepository templateRepository;
    private final RecurringJournalOccurrenceRepository occurrenceRepository;
    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalPostingService journalPostingService;
    private final DocumentNumberGeneratorService generalSequenceGeneratorService;
    private final PeriodLockGuard periodLockGuard;

    @Transactional
    public RecurringJournalTemplateResponse create(CreateRecurringJournalTemplateRequest request) {
        if (request.getEndDate() != null && !request.getEndDate().isAfter(request.getStartDate())) {
            throw new BadRequestException("End date must be after start date");
        }

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        RecurringJournalTemplate template = new RecurringJournalTemplate();
        template.setDescription(request.getDescription());
        template.setReference(request.getReference());
        template.setJournalType(request.getJournalType());
        template.setFrequency(request.getFrequency());
        template.setStartDate(request.getStartDate());
        template.setEndDate(request.getEndDate());
        template.setMaxOccurrences(request.getMaxOccurrences());
        template.setNextRunDate(request.getStartDate());
        template.setStatus(RecurringJournalStatus.ACTIVE);

        int lineNumber = 1;
        for (RecurringJournalTemplateLineRequest lineRequest : request.getLines()) {
            BigDecimal debit = defaultAmount(lineRequest.getDebitAmount());
            BigDecimal credit = defaultAmount(lineRequest.getCreditAmount());
            boolean hasDebit = debit.compareTo(BigDecimal.ZERO) > 0;
            boolean hasCredit = credit.compareTo(BigDecimal.ZERO) > 0;
            if (hasDebit == hasCredit) {
                throw new BadRequestException("Each line must contain either a debit or a credit amount");
            }

            AccountEntity account = accountRepository.findById(lineRequest.getAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

            RecurringJournalTemplateLine line = new RecurringJournalTemplateLine();
            line.setAccount(account);
            line.setLineNumber(lineNumber++);
            line.setDescription(lineRequest.getDescription());
            line.setDebitAmount(debit);
            line.setCreditAmount(credit);
            template.addLine(line);

            totalDebit = totalDebit.add(debit);
            totalCredit = totalCredit.add(credit);
        }

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BadRequestException("Template lines must balance (total debit must equal total credit)");
        }

        RecurringJournalTemplate saved = templateRepository.save(template);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RecurringJournalTemplateResponse> getAll() {
        return templateRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RecurringJournalTemplateResponse getById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<RecurringJournalOccurrenceResponse> getOccurrences(Long templateId) {
        return occurrenceRepository.findByTemplateIdOrderByScheduledDateDesc(templateId).stream()
                .map(this::toOccurrenceResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RecurringJournalTemplateResponse pause(Long id) {
        RecurringJournalTemplate template = getEntity(id);
        if (template.getStatus() != RecurringJournalStatus.ACTIVE) {
            throw new BadRequestException("Only an active recurring journal can be paused");
        }
        template.setStatus(RecurringJournalStatus.PAUSED);
        return toResponse(templateRepository.save(template));
    }

    @Transactional
    public RecurringJournalTemplateResponse resume(Long id) {
        RecurringJournalTemplate template = getEntity(id);
        if (template.getStatus() != RecurringJournalStatus.PAUSED) {
            throw new BadRequestException("Only a paused recurring journal can be resumed");
        }
        template.setStatus(RecurringJournalStatus.ACTIVE);
        if (template.getNextRunDate() != null && template.getNextRunDate().isBefore(LocalDate.now())) {
            template.setNextRunDate(LocalDate.now());
        }
        return toResponse(templateRepository.save(template));
    }

    @Transactional
    public RecurringJournalTemplateResponse stop(Long id) {
        RecurringJournalTemplate template = getEntity(id);
        if (template.getStatus() == RecurringJournalStatus.STOPPED
                || template.getStatus() == RecurringJournalStatus.ARCHIVED) {
            throw new BadRequestException("Recurring journal is already stopped");
        }
        template.setStatus(RecurringJournalStatus.STOPPED);
        template.setNextRunDate(null);
        return toResponse(templateRepository.save(template));
    }

    @Transactional
    public RecurringJournalTemplateResponse archive(Long id) {
        RecurringJournalTemplate template = getEntity(id);
        if (template.getStatus() != RecurringJournalStatus.STOPPED) {
            throw new BadRequestException("Only a stopped recurring journal can be archived");
        }
        template.setStatus(RecurringJournalStatus.ARCHIVED);
        return toResponse(templateRepository.save(template));
    }

    /** Generates every due occurrence across all active templates. Called by the daily
     * scheduled job and by the on-demand "generate due now" endpoint. */
    @Transactional
    public void generateDueOccurrences() {
        List<RecurringJournalTemplate> due = templateRepository
                .findByStatusAndNextRunDateLessThanEqual(RecurringJournalStatus.ACTIVE, LocalDate.now());

        for (RecurringJournalTemplate template : due) {
            try {
                generateOccurrenceFor(template);
            } catch (Exception e) {
                log.error("Failed to generate recurring journal occurrence for template {}: {}",
                        template.getId(), e.getMessage(), e);
            }
        }
    }

    private void generateOccurrenceFor(RecurringJournalTemplate template) {
        LocalDate dueDate = template.getNextRunDate();
        if (dueDate == null) {
            return;
        }

        if (template.getEndDate() != null && dueDate.isAfter(template.getEndDate())) {
            template.setStatus(RecurringJournalStatus.STOPPED);
            template.setNextRunDate(null);
            templateRepository.save(template);
            return;
        }
        if (template.getMaxOccurrences() != null && template.getOccurrencesGenerated() >= template.getMaxOccurrences()) {
            template.setStatus(RecurringJournalStatus.STOPPED);
            template.setNextRunDate(null);
            templateRepository.save(template);
            return;
        }

        RecurringJournalOccurrence occurrence = occurrenceRepository
                .findByTemplateIdAndScheduledDate(template.getId(), dueDate)
                .orElse(null);

        if (occurrence != null && occurrence.getStatus() == RecurringOccurrenceStatus.GENERATED) {
            advanceNextRunDate(template, dueDate);
            templateRepository.save(template);
            return;
        }

        if (!periodLockGuard.isPostable(dueDate)) {
            if (occurrence == null) {
                occurrence = RecurringJournalOccurrence.builder()
                        .template(template)
                        .scheduledDate(dueDate)
                        .status(RecurringOccurrenceStatus.PENDING)
                        .failureReason("Accounting period for " + dueDate + " is locked or closed")
                        .build();
                occurrenceRepository.save(occurrence);
            }
            return;
        }

        JournalEntry entry = buildJournalFromTemplate(template, dueDate);

        try {
            journalPostingService.post(entry);
        } catch (Exception e) {
            if (occurrence == null) {
                occurrence = RecurringJournalOccurrence.builder()
                        .template(template)
                        .scheduledDate(dueDate)
                        .status(RecurringOccurrenceStatus.FAILED)
                        .failureReason(e.getMessage())
                        .build();
            } else {
                occurrence.setStatus(RecurringOccurrenceStatus.FAILED);
                occurrence.setFailureReason(e.getMessage());
            }
            occurrenceRepository.save(occurrence);
            return;
        }

        JournalEntry saved = journalEntryRepository.save(entry);

        if (occurrence == null) {
            occurrence = RecurringJournalOccurrence.builder()
                    .template(template)
                    .scheduledDate(dueDate)
                    .build();
        }
        occurrence.setStatus(RecurringOccurrenceStatus.GENERATED);
        occurrence.setGeneratedJournalId(saved.getId());
        occurrence.setGeneratedAt(LocalDateTime.now());
        occurrence.setFailureReason(null);
        occurrenceRepository.save(occurrence);

        template.setOccurrencesGenerated(template.getOccurrencesGenerated() + 1);
        advanceNextRunDate(template, dueDate);
        templateRepository.save(template);
    }

    private JournalEntry buildJournalFromTemplate(RecurringJournalTemplate template, LocalDate dueDate) {
        JournalEntry entry = new JournalEntry();
        entry.setJournalNumber(generalSequenceGeneratorService.generate(DocumentModule.JOURNAL));
        entry.setJournalDate(dueDate);
        entry.setReference(template.getReference());
        entry.setDescription(template.getDescription() + " (recurring, " + dueDate + ")");
        entry.setJournalType(template.getJournalType());
        entry.setStatus(JournalStatus.DRAFT);
        entry.setSourceModule(SOURCE_MODULE);
        entry.setSourceEntityId(template.getId());

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        int lineNumber = 1;
        for (RecurringJournalTemplateLine templateLine : template.getLines()) {
            JournalLine line = new JournalLine();
            line.setAccount(templateLine.getAccount());
            line.setLineNumber(lineNumber++);
            line.setDescription(templateLine.getDescription());
            line.setDebitAmount(defaultAmount(templateLine.getDebitAmount()));
            line.setCreditAmount(defaultAmount(templateLine.getCreditAmount()));
            entry.addLine(line);

            totalDebit = totalDebit.add(line.getDebitAmount());
            totalCredit = totalCredit.add(line.getCreditAmount());
        }
        entry.setTotalDebit(totalDebit);
        entry.setTotalCredit(totalCredit);

        return entry;
    }

    private void advanceNextRunDate(RecurringJournalTemplate template, LocalDate fromDate) {
        LocalDate next = computeNextRunDate(fromDate, template.getFrequency());
        if (template.getEndDate() != null && next.isAfter(template.getEndDate())) {
            template.setStatus(RecurringJournalStatus.STOPPED);
            template.setNextRunDate(null);
            return;
        }
        if (template.getMaxOccurrences() != null && template.getOccurrencesGenerated() >= template.getMaxOccurrences()) {
            template.setStatus(RecurringJournalStatus.STOPPED);
            template.setNextRunDate(null);
            return;
        }
        template.setNextRunDate(next);
    }

    private LocalDate computeNextRunDate(LocalDate from, RecurringJournalFrequency frequency) {
        return switch (frequency) {
            case DAILY -> from.plusDays(1);
            case WEEKLY -> from.plusWeeks(1);
            case MONTHLY -> from.plusMonths(1);
            case QUARTERLY -> from.plusMonths(3);
            case SEMI_ANNUALLY -> from.plusMonths(6);
            case ANNUALLY -> from.plusYears(1);
        };
    }

    private RecurringJournalTemplate getEntity(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring journal template not found"));
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private RecurringJournalTemplateResponse toResponse(RecurringJournalTemplate template) {
        List<RecurringJournalTemplateLineResponse> lines = template.getLines().stream()
                .map(line -> RecurringJournalTemplateLineResponse.builder()
                        .id(line.getId())
                        .accountId(line.getAccount().getId())
                        .accountCode(line.getAccount().getAccountId())
                        .accountName(line.getAccount().getAccountName())
                        .lineNumber(line.getLineNumber())
                        .description(line.getDescription())
                        .debitAmount(line.getDebitAmount())
                        .creditAmount(line.getCreditAmount())
                        .build())
                .collect(Collectors.toList());

        return RecurringJournalTemplateResponse.builder()
                .id(template.getId())
                .description(template.getDescription())
                .reference(template.getReference())
                .journalType(template.getJournalType())
                .frequency(template.getFrequency())
                .startDate(template.getStartDate())
                .endDate(template.getEndDate())
                .maxOccurrences(template.getMaxOccurrences())
                .occurrencesGenerated(template.getOccurrencesGenerated())
                .nextRunDate(template.getNextRunDate())
                .status(template.getStatus())
                .lines(lines)
                .build();
    }

    private RecurringJournalOccurrenceResponse toOccurrenceResponse(RecurringJournalOccurrence occurrence) {
        return RecurringJournalOccurrenceResponse.builder()
                .id(occurrence.getId())
                .templateId(occurrence.getTemplate().getId())
                .scheduledDate(occurrence.getScheduledDate())
                .status(occurrence.getStatus())
                .generatedJournalId(occurrence.getGeneratedJournalId())
                .failureReason(occurrence.getFailureReason())
                .generatedAt(occurrence.getGeneratedAt())
                .build();
    }
}
