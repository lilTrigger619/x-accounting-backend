package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.payroll.CreateReimbursementClaimRequest;
import com.unionsg.xaccounting.dto.payroll.ReimbursementClaimResponse;
import com.unionsg.xaccounting.enums.ReimbursementStatus;
import com.unionsg.xaccounting.service.payroll.ReimbursementClaimService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll/reimbursements")
@RequiredArgsConstructor
public class ReimbursementClaimController {

    private final ReimbursementClaimService reimbursementClaimService;

    @PostMapping
    public ResponseEntity<ReimbursementClaimResponse> submit(@Valid @RequestBody CreateReimbursementClaimRequest request) {
        return ResponseEntity.ok(reimbursementClaimService.submit(request));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ReimbursementClaimResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(reimbursementClaimService.approve(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ReimbursementClaimResponse> reject(@PathVariable Long id) {
        return ResponseEntity.ok(reimbursementClaimService.reject(id));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<ReimbursementClaimResponse> markPaid(@PathVariable Long id) {
        return ResponseEntity.ok(reimbursementClaimService.markPaid(id));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ReimbursementClaimResponse>> getByStatus(@PathVariable ReimbursementStatus status) {
        return ResponseEntity.ok(reimbursementClaimService.getByStatus(status));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<ReimbursementClaimResponse>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(reimbursementClaimService.getByEmployee(employeeId));
    }
}
