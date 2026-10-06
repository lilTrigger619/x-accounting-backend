package com.unionsg.xaccounting.controller.banking;

import com.unionsg.xaccounting.dto.FileResponseDto;
import com.unionsg.xaccounting.dto.banking.BankTransferAccountSummary;
import com.unionsg.xaccounting.dto.banking.BankTransferActivityResponse;
import com.unionsg.xaccounting.dto.banking.BankTransferFilter;
import com.unionsg.xaccounting.dto.banking.BankTransferJournalResponse;
import com.unionsg.xaccounting.dto.banking.BankTransferListItemResponse;
import com.unionsg.xaccounting.dto.banking.BankTransferPreviewResponse;
import com.unionsg.xaccounting.dto.banking.BankTransferReasonRequest;
import com.unionsg.xaccounting.dto.banking.BankTransferResponse;
import com.unionsg.xaccounting.dto.banking.SaveBankTransferRequest;
import com.unionsg.xaccounting.enums.banking.BankTransferStatus;
import com.unionsg.xaccounting.security.annotation.RequirePermission;
import com.unionsg.xaccounting.service.FileService.FileStorageService;
import com.unionsg.xaccounting.service.banking.BankTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/bank-transfers")
@RequiredArgsConstructor
public class BankTransferController {

    private static final String GROUP = "Banking";

    private final BankTransferService service;
    private final FileStorageService fileStorageService;

    @GetMapping
    @RequirePermission(value = "view_bank_transfers", group = GROUP)
    public ResponseEntity<Page<BankTransferListItemResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String transferNumber,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) Long sourceBankAccountId,
            @RequestParam(required = false) Long destinationBankAccountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) BankTransferStatus status,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) UUID createdById,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "transferDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        BankTransferFilter filter = BankTransferFilter.builder()
                .search(search)
                .transferNumber(transferNumber)
                .reference(reference)
                .sourceBankAccountId(sourceBankAccountId)
                .destinationBankAccountId(destinationBankAccountId)
                .fromDate(fromDate)
                .toDate(toDate)
                .status(status)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .createdById(createdById)
                .build();
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return ResponseEntity.ok(service.list(filter, PageRequest.of(page, size, Sort.by(direction, sortBy))));
    }

    @GetMapping("/accounts")
    @RequirePermission(value = "view_bank_transfers", group = GROUP)
    public ResponseEntity<List<BankTransferAccountSummary>> accounts() {
        return ResponseEntity.ok(service.listTransferAccounts());
    }

    @GetMapping("/base-currency")
    @RequirePermission(value = "view_bank_transfers", group = GROUP)
    public ResponseEntity<Map<String, String>> baseCurrency() {
        return ResponseEntity.ok(Map.of("currency", service.baseCurrency()));
    }

    @PostMapping("/preview")
    @RequirePermission(value = "manage_bank_transfers", group = GROUP)
    public ResponseEntity<BankTransferPreviewResponse> preview(@Valid @RequestBody SaveBankTransferRequest request) {
        return ResponseEntity.ok(service.preview(request));
    }

    @PostMapping
    @RequirePermission(value = "manage_bank_transfers", group = GROUP)
    public ResponseEntity<BankTransferResponse> create(@Valid @RequestBody SaveBankTransferRequest request) {
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @RequirePermission(value = "view_bank_transfers", group = GROUP)
    public ResponseEntity<BankTransferResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PutMapping("/{id}")
    @RequirePermission(value = "manage_bank_transfers", group = GROUP)
    public ResponseEntity<BankTransferResponse> update(
            @PathVariable Long id, @Valid @RequestBody SaveBankTransferRequest request
    ) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PostMapping("/{id}/post")
    @RequirePermission(value = "post_bank_transfers", group = GROUP)
    public ResponseEntity<BankTransferResponse> post(@PathVariable Long id) {
        return ResponseEntity.ok(service.post(id));
    }

    @PostMapping("/{id}/cancel")
    @RequirePermission(value = "manage_bank_transfers", group = GROUP)
    public ResponseEntity<BankTransferResponse> cancel(
            @PathVariable Long id, @Valid @RequestBody(required = false) BankTransferReasonRequest request
    ) {
        return ResponseEntity.ok(service.cancel(id, request != null ? request.getReason() : null));
    }

    @PostMapping("/{id}/reverse")
    @RequirePermission(value = "reverse_bank_transfers", group = GROUP)
    public ResponseEntity<BankTransferResponse> reverse(
            @PathVariable Long id, @Valid @RequestBody BankTransferReasonRequest request
    ) {
        return ResponseEntity.ok(service.reverse(id, request.getReason()));
    }

    @GetMapping("/{id}/journal")
    @RequirePermission(value = "view_bank_transfers", group = GROUP)
    public ResponseEntity<BankTransferJournalResponse> journal(@PathVariable Long id) {
        return ResponseEntity.ok(service.getJournal(id));
    }

    @GetMapping("/{id}/activity")
    @RequirePermission(value = "view_bank_transfers", group = GROUP)
    public ResponseEntity<List<BankTransferActivityResponse>> activity(@PathVariable Long id) {
        return ResponseEntity.ok(service.getActivity(id));
    }

    @GetMapping("/{id}/attachments")
    @RequirePermission(value = "view_bank_transfers", group = GROUP)
    public ResponseEntity<List<FileResponseDto>> attachments(@PathVariable Long id) {
        return ResponseEntity.ok(service.listAttachments(id));
    }

    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission(value = "manage_bank_transfers", group = GROUP)
    public ResponseEntity<List<FileResponseDto>> attach(
            @PathVariable Long id,
            @RequestPart("files") MultipartFile[] files,
            @RequestParam(required = false) String description
    ) {
        return new ResponseEntity<>(service.addAttachments(id, files, description), HttpStatus.CREATED);
    }

    @GetMapping("/{id}/attachments/{fileId}/download")
    @RequirePermission(value = "view_bank_transfers", group = GROUP)
    public ResponseEntity<Resource> download(@PathVariable Long id, @PathVariable String fileId) {
        FileResponseDto file = service.getAttachment(id, fileId);
        Resource resource = fileStorageService.download(file.getStoragePath());
        String mimeType = file.getMimeType() != null ? file.getMimeType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getOriginalName().replace("\"", "") + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}/attachments/{fileId}")
    @RequirePermission(value = "manage_bank_transfers", group = GROUP)
    public ResponseEntity<Void> removeAttachment(@PathVariable Long id, @PathVariable String fileId) {
        service.removeAttachment(id, fileId);
        return ResponseEntity.noContent().build();
    }
}
