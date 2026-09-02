package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.TaxCategoryDTO;
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
    public ResponseEntity<TaxCategoryDTO> create(@RequestBody TaxCategoryDTO dto) {
        return new ResponseEntity<>(taxCategoryService.create(dto), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TaxCategoryDTO>> getAll() {
        return ResponseEntity.ok(taxCategoryService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaxCategoryDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(taxCategoryService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaxCategoryDTO> update(@PathVariable Long id, @RequestBody TaxCategoryDTO dto) {
        return ResponseEntity.ok(taxCategoryService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taxCategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
