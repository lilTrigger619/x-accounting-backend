package com.unionsg.xaccounting.controller.config;

import com.unionsg.xaccounting.dto.config.ConfigDto;
import com.unionsg.xaccounting.dto.config.ConfigItemDto;
import com.unionsg.xaccounting.dto.config.UpsertConfigDto;
import com.unionsg.xaccounting.dto.config.UpsertConfigItemDto;
import com.unionsg.xaccounting.service.ConfigService;
//import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/configs")
@RequiredArgsConstructor
public class ConfigController {
    private final ConfigService configService;

//    private final ConfigService configService;


    @GetMapping
    public ResponseEntity<List<ConfigDto>> getAllConfigs() {

        return ResponseEntity.ok(
                configService.getAllConfigs()
        );

    }


    @GetMapping("/{key}")
    public ResponseEntity<ConfigDto> getConfigByKey(
            @PathVariable String key
    ) {

        return ResponseEntity.ok(
                configService.getConfigByKey(key)
        );

    }

    @PostMapping
    public ResponseEntity<ConfigDto> createConfig(
            @RequestBody @Valid UpsertConfigDto request
    ) {
        System.out.println("the request key "+ request.getKey());
        System.out.println("the request title "+ request.getTitle());
        return ResponseEntity.ok(configService.createConfig(request));
    }

    @PutMapping("/{key}")
    public ResponseEntity<ConfigDto> updateConfig(
            @PathVariable String key,
            @RequestBody @Valid UpsertConfigDto request
    ) {
        return ResponseEntity.ok(configService.updateConfig(key, request));
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> deleteConfig(@PathVariable String key) {
        configService.deleteConfig(key);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{key}/items")
    public ResponseEntity<ConfigItemDto> addItem(
            @PathVariable String key,
            @RequestBody @Valid UpsertConfigItemDto request
    ) {
        return ResponseEntity.ok(configService.addItem(key, request));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ConfigItemDto> updateItem(
            @PathVariable String itemId,
            @RequestBody @Valid UpsertConfigItemDto request
    ) {
        return ResponseEntity.ok(configService.updateItem(itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable String itemId) {
        configService.deleteItem(itemId);
        return ResponseEntity.noContent().build();
    }
}
