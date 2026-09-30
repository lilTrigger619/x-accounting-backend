package com.unionsg.xaccounting.controller.settings;

import com.unionsg.xaccounting.dto.settings.AddQuickAccessItemRequest;
import com.unionsg.xaccounting.dto.settings.QuickAccessItemResponse;
import com.unionsg.xaccounting.dto.settings.ReorderQuickAccessRequest;
import com.unionsg.xaccounting.service.settings.QuickAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/quick-access")
@RequiredArgsConstructor
public class QuickAccessController {

    private final QuickAccessService quickAccessService;

    @GetMapping
    public ResponseEntity<List<QuickAccessItemResponse>> list() {
        return ResponseEntity.ok(quickAccessService.list());
    }

    @PostMapping
    public ResponseEntity<QuickAccessItemResponse> add(@RequestBody AddQuickAccessItemRequest request) {
        return new ResponseEntity<>(quickAccessService.add(request), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        quickAccessService.remove(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorder(@RequestBody ReorderQuickAccessRequest request) {
        quickAccessService.reorder(request);
        return ResponseEntity.noContent().build();
    }
}
