package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.FileResponseDto;
import com.unionsg.xaccounting.dto.expense.ExpenseActivityResponse;
import com.unionsg.xaccounting.dto.expense.ExpenseFilter;
import com.unionsg.xaccounting.dto.expense.ExpenseJournalResponse;
import com.unionsg.xaccounting.dto.expense.ExpenseListItemResponse;
import com.unionsg.xaccounting.dto.expense.ExpensePreviewResponse;
import com.unionsg.xaccounting.dto.expense.ExpenseReasonRequest;
import com.unionsg.xaccounting.dto.expense.ExpenseResponse;
import com.unionsg.xaccounting.dto.expense.SaveExpenseRequest;
import com.unionsg.xaccounting.enums.PaymentMethod;
import com.unionsg.xaccounting.enums.expense.ExpenseStatus;
import com.unionsg.xaccounting.security.annotation.RequirePermission;
import com.unionsg.xaccounting.service.FileService.FileStorageService;
import com.unionsg.xaccounting.service.expense.ExpenseService;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private static final String GROUP = "Expenses";

    private final ExpenseService service;
    private final FileStorageService fileStorageService;

    @GetMapping
    @RequirePermission(value = "view_expenses", group = GROUP)
    public ResponseEntity<Page<ExpenseListItemResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ExpenseStatus status,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long paymentAccountId,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) UUID createdById,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "paymentDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        ExpenseFilter filter = ExpenseFilter.builder()
                .search(search)
                .status(status)
                .supplierId(supplierId)
                .paymentAccountId(paymentAccountId)
                .paymentMethod(paymentMethod)
                .accountId(accountId)
                .category(category)
                .fromDate(fromDate)
                .toDate(toDate)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .createdById(createdById)
                .build();
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return ResponseEntity.ok(service.list(filter, PageRequest.of(page, size, Sort.by(direction, sortBy))));
    }

    @PostMapping("/preview")
    @RequirePermission(value = "manage_expenses", group = GROUP)
    public ResponseEntity<ExpensePreviewResponse> preview(@Valid @RequestBody SaveExpenseRequest request) {
        return ResponseEntity.ok(service.preview(request));
    }

    @PostMapping
    @RequirePermission(value = "manage_expenses", group = GROUP)
    public ResponseEntity<ExpenseResponse> create(@Valid @RequestBody SaveExpenseRequest request) {
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @RequirePermission(value = "view_expenses", group = GROUP)
    public ResponseEntity<ExpenseResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PutMapping("/{id}")
    @RequirePermission(value = "manage_expenses", group = GROUP)
    public ResponseEntity<ExpenseResponse> update(@PathVariable Long id, @Valid @RequestBody SaveExpenseRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(value = "manage_expenses", group = GROUP)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/post")
    @RequirePermission(value = "post_expenses", group = GROUP)
    public ResponseEntity<ExpenseResponse> post(@PathVariable Long id) {
        return ResponseEntity.ok(service.post(id));
    }

    @PostMapping("/{id}/reverse")
    @RequirePermission(value = "reverse_expenses", group = GROUP)
    public ResponseEntity<ExpenseResponse> reverse(@PathVariable Long id, @Valid @RequestBody ExpenseReasonRequest request) {
        return ResponseEntity.ok(service.reverse(id, request.getReason()));
    }

    @GetMapping("/{id}/journal")
    @RequirePermission(value = "view_expenses", group = GROUP)
    public ResponseEntity<ExpenseJournalResponse> journal(@PathVariable Long id) {
        return ResponseEntity.ok(service.getJournal(id));
    }

    @GetMapping("/{id}/activity")
    @RequirePermission(value = "view_expenses", group = GROUP)
    public ResponseEntity<List<ExpenseActivityResponse>> activity(@PathVariable Long id) {
        return ResponseEntity.ok(service.getActivity(id));
    }

    @GetMapping("/{id}/attachments")
    @RequirePermission(value = "view_expenses", group = GROUP)
    public ResponseEntity<List<FileResponseDto>> attachments(@PathVariable Long id) {
        return ResponseEntity.ok(service.listAttachments(id));
    }

    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission(value = "manage_expenses", group = GROUP)
    public ResponseEntity<List<FileResponseDto>> attach(
            @PathVariable Long id,
            @RequestPart("files") MultipartFile[] files,
            @RequestParam(required = false) String description
    ) {
        return new ResponseEntity<>(service.addAttachments(id, files, description), HttpStatus.CREATED);
    }

    @GetMapping("/{id}/attachments/{fileId}/download")
    @RequirePermission(value = "view_expenses", group = GROUP)
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
    @RequirePermission(value = "manage_expenses", group = GROUP)
    public ResponseEntity<Void> removeAttachment(@PathVariable Long id, @PathVariable String fileId) {
        service.removeAttachment(id, fileId);
        return ResponseEntity.noContent().build();
    }
}
