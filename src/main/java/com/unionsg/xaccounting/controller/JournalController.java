package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.dto.journal.ReverseJournalRequest;
import com.unionsg.xaccounting.dto.journal.UpdateJournalRequest;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.service.journal.JournalService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/journals")
public class JournalController {

    private final JournalService journalService;

    public JournalController(
            JournalService journalService
    ) {
        this.journalService = journalService;
    }

    // =====================================================
    // Create Journal
    // =====================================================

    @PostMapping
    public ResponseEntity<JournalResponse> create(
            @Valid @RequestBody CreateJournalRequest request
    ) {

        JournalResponse response =
                journalService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =====================================================
    // Update Journal
    // =====================================================

    @PutMapping("/{id}")
    public ResponseEntity<JournalResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateJournalRequest request
    ) {

        JournalResponse response =
                journalService.update(id, request);

        return ResponseEntity.ok(response);
    }

    // =====================================================
    // Get By ID
    // =====================================================

    @GetMapping("/{id}")
    public ResponseEntity<JournalResponse> getById(
            @PathVariable Long id
    ) {

        JournalResponse response =
                journalService.getById(id);

        return ResponseEntity.ok(response);
    }

    // =====================================================
    // Get By Journal Number
    // =====================================================

    @GetMapping("/number/{journalNumber}")
    public ResponseEntity<JournalResponse> getByNumber(
            @PathVariable String journalNumber
    ) {

        JournalResponse response =
                journalService.getByJournalNumber(
                        journalNumber
                );

        return ResponseEntity.ok(response);
    }

    // =====================================================
    // Get All / Search
    // =====================================================

    @GetMapping
    public ResponseEntity<Page<JournalResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) JournalStatus status,
            @RequestParam(required = false) JournalType journalType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String sourceModule,
            Pageable pageable
    ) {

        Page<JournalResponse> response =
                journalService.getAll(search, status, journalType, fromDate, toDate, sourceModule, pageable);

        return ResponseEntity.ok(response);
    }

    // =====================================================
    // Post Journal
    // =====================================================

    @PostMapping("/{id}/post")
    public ResponseEntity<JournalResponse> post(
            @PathVariable Long id
    ) {

        JournalResponse response =
                journalService.post(id);

        return ResponseEntity.ok(response);
    }

    // =====================================================
    // Reverse Journal
    // =====================================================

    @PostMapping("/{id}/reverse")
    public ResponseEntity<JournalResponse> reverse(
            @PathVariable Long id,
            @Valid @RequestBody ReverseJournalRequest request
    ) {

        JournalResponse response =
                journalService.reverse(
                        id,
                        request.getReason()
                );

        return ResponseEntity.ok(response);
    }

    // =====================================================
    // Delete Draft Journal
    // =====================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {

        journalService.deleteDraft(id);

        return ResponseEntity.noContent().build();
    }
}