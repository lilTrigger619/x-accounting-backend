package com.unionsg.xaccounting.controller.accounting;

import com.unionsg.xaccounting.dto.accounting.CreateRecurringJournalTemplateRequest;
import com.unionsg.xaccounting.dto.accounting.RecurringJournalOccurrenceResponse;
import com.unionsg.xaccounting.dto.accounting.RecurringJournalTemplateResponse;
import com.unionsg.xaccounting.service.accounting.RecurringJournalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recurring-journals")
@RequiredArgsConstructor
public class RecurringJournalController {

    private final RecurringJournalService recurringJournalService;

    @PostMapping
    public ResponseEntity<RecurringJournalTemplateResponse> create(
            @Valid @RequestBody CreateRecurringJournalTemplateRequest request
    ) {
        return new ResponseEntity<>(recurringJournalService.create(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<RecurringJournalTemplateResponse>> getAll() {
        return ResponseEntity.ok(recurringJournalService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecurringJournalTemplateResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(recurringJournalService.getById(id));
    }

    @GetMapping("/{id}/occurrences")
    public ResponseEntity<List<RecurringJournalOccurrenceResponse>> getOccurrences(@PathVariable Long id) {
        return ResponseEntity.ok(recurringJournalService.getOccurrences(id));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<RecurringJournalTemplateResponse> pause(@PathVariable Long id) {
        return ResponseEntity.ok(recurringJournalService.pause(id));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<RecurringJournalTemplateResponse> resume(@PathVariable Long id) {
        return ResponseEntity.ok(recurringJournalService.resume(id));
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<RecurringJournalTemplateResponse> stop(@PathVariable Long id) {
        return ResponseEntity.ok(recurringJournalService.stop(id));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<RecurringJournalTemplateResponse> archive(@PathVariable Long id) {
        return ResponseEntity.ok(recurringJournalService.archive(id));
    }

    @PostMapping("/generate-due")
    public ResponseEntity<Map<String, String>> generateDue() {
        recurringJournalService.generateDueOccurrences();
        return ResponseEntity.ok(Map.of("message", "Due recurring journal occurrences generated"));
    }
}
