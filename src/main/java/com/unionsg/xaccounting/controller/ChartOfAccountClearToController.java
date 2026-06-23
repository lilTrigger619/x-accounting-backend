package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.ChartClearToResponseDto;
import com.unionsg.xaccounting.service.ChartOfAccountClearToService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chart-of-accounts/clear-to")
@RequiredArgsConstructor
public class ChartOfAccountClearToController {

    private final ChartOfAccountClearToService service;

    // GET /api/chart-of-accounts/clear-to/{clearToCode}
    @GetMapping("/{clearToCode}")
    public ResponseEntity<ChartClearToResponseDto> getByClearToCode(@PathVariable Long clearToCode) {
        return new ResponseEntity<>(service.getByClearToCode(clearToCode), HttpStatus.OK);
    }

    // GET /api/chart-of-accounts/clear-to
    @GetMapping
    public ResponseEntity<List<ChartClearToResponseDto>> getAllActive() {
        return new ResponseEntity<>(service.getAllActive(), HttpStatus.OK);
    }
}

