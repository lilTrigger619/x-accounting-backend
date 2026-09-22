package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.payroll.ChangeCompensationRequest;
import com.unionsg.xaccounting.dto.payroll.ChangeEmployeeStatusRequest;
import com.unionsg.xaccounting.dto.payroll.CreateEmployeeRequest;
import com.unionsg.xaccounting.dto.payroll.EmployeeCompensationResponse;
import com.unionsg.xaccounting.dto.payroll.EmployeeResponse;
import com.unionsg.xaccounting.dto.payroll.UpdateEmployeeRequest;
import com.unionsg.xaccounting.service.payroll.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/payroll/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody CreateEmployeeRequest request) {
        return ResponseEntity.ok(employeeService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getAll() {
        return ResponseEntity.ok(employeeService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateEmployeeRequest request) {
        return ResponseEntity.ok(employeeService.update(id, request));
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<EmployeeResponse> changeStatus(@PathVariable Long id, @Valid @RequestBody ChangeEmployeeStatusRequest request) {
        return ResponseEntity.ok(employeeService.changeStatus(id, request));
    }

    @PostMapping("/{id}/compensation")
    public ResponseEntity<EmployeeCompensationResponse> changeCompensation(@PathVariable Long id, @Valid @RequestBody ChangeCompensationRequest request) {
        return ResponseEntity.ok(employeeService.changeCompensation(id, request));
    }

    @GetMapping("/{id}/compensation-history")
    public ResponseEntity<List<EmployeeCompensationResponse>> getCompensationHistory(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getCompensationHistory(id));
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EmployeeResponse> uploadPhoto(@PathVariable Long id, @RequestPart("photo") MultipartFile photo) {
        return ResponseEntity.ok(employeeService.uploadPhoto(id, photo));
    }

    @DeleteMapping("/{id}/photo")
    public ResponseEntity<EmployeeResponse> deletePhoto(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.deletePhoto(id));
    }
}
