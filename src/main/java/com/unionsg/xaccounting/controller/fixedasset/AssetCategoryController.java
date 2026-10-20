package com.unionsg.xaccounting.controller.fixedasset;

import com.unionsg.xaccounting.dto.fixedasset.AssetCategoryResponse;
import com.unionsg.xaccounting.dto.fixedasset.SaveAssetCategoryRequest;
import com.unionsg.xaccounting.security.annotation.RequirePermission;
import com.unionsg.xaccounting.service.fixedasset.AssetCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asset-categories")
@RequiredArgsConstructor
public class AssetCategoryController {

    private static final String GROUP = "Settings";

    private final AssetCategoryService service;

    @PostMapping
    @RequirePermission(value = "manage_asset_categories", group = GROUP)
    public ResponseEntity<AssetCategoryResponse> create(@RequestBody SaveAssetCategoryRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @GetMapping
    public ResponseEntity<List<AssetCategoryResponse>> list(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(service.list(activeOnly));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssetCategoryResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PutMapping("/{id}")
    @RequirePermission(value = "manage_asset_categories", group = GROUP)
    public ResponseEntity<AssetCategoryResponse> update(@PathVariable Long id, @RequestBody SaveAssetCategoryRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/{id}/active")
    @RequirePermission(value = "manage_asset_categories", group = GROUP)
    public ResponseEntity<AssetCategoryResponse> setActive(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(service.setActive(id, active));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(value = "manage_asset_categories", group = GROUP)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
