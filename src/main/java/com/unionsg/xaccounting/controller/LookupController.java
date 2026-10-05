package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.lookup.LookupDefinitionDto;
import com.unionsg.xaccounting.service.lookup.LookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Read-only system-controlled values for pickers and list filters. */
@RestController
@RequestMapping("/api/lookups")
@RequiredArgsConstructor
public class LookupController {

    private final LookupService lookupService;

    @GetMapping
    public ResponseEntity<List<LookupDefinitionDto>> getAll() {
        return ResponseEntity.ok(lookupService.getAll());
    }

    @GetMapping("/{key}")
    public ResponseEntity<LookupDefinitionDto> get(@PathVariable String key) {
        return ResponseEntity.ok(lookupService.get(key));
    }
}
