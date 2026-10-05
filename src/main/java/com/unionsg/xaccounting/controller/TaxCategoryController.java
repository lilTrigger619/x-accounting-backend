package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.TaxCategoryDTO;
import com.unionsg.xaccounting.security.annotation.RequirePermission;
import com.unionsg.xaccounting.service.TaxCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tax-categories")
@RequiredArgsConstructor
public class TaxCategoryController {

    private final TaxCategoryService taxCategoryService;

    @PostMapping
    @RequirePermission(value = "manage_tax_rates", group = "Settings")
    public ResponseEntity<TaxCategoryDTO> create(@RequestBody TaxCategoryDTO dto) {
        return new ResponseEntity<>(taxCategoryService.create(dto), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TaxCategoryDTO>> getAll(@RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(taxCategoryService.getAll(activeOnly));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaxCategoryDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(taxCategoryService.getById(id));
    }

    @PutMapping("/{id}")
    @RequirePermission(value = "manage_tax_rates", group = "Settings")
    public ResponseEntity<TaxCategoryDTO> update(@PathVariable Long id, @RequestBody TaxCategoryDTO dto) {
        return ResponseEntity.ok(taxCategoryService.update(id, dto));
    }

    @PatchMapping("/{id}/active")
    @RequirePermission(value = "manage_tax_rates", group = "Settings")
    public ResponseEntity<TaxCategoryDTO> setActive(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(taxCategoryService.setActive(id, active));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(value = "manage_tax_rates", group = "Settings")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taxCategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
