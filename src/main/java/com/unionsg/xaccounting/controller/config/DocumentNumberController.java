package com.unionsg.xaccounting.controller.config;

import com.unionsg.xaccounting.dto.config.DocumentNumberConfigDto;
import com.unionsg.xaccounting.dto.config.NumberGenerationResponse;
import com.unionsg.xaccounting.dto.config.UpsertDocumentNumberConfigDto;
import com.unionsg.xaccounting.enums.DocumentModule;
import com.unionsg.xaccounting.service.DocumentNumberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/document-numbers")
@RequiredArgsConstructor
public class DocumentNumberController {

    private final DocumentNumberService documentNumberService;

    /**
     * Generates the next unique document number for the specified module.
     *
     * @param module the module to generate a number for (e.g., INVOICE, ACCOUNT, JOURNAL)
     * @return the generated document number
     */
    @PostMapping("/generate/{module}")
    public ResponseEntity<NumberGenerationResponse> generateNextNumber(
            @PathVariable DocumentModule module
    ) {
        String generatedNumber = documentNumberService.generateNextNumber(module);
        return ResponseEntity.ok(
                NumberGenerationResponse.builder()
                        .module(module.name())
                        .generatedNumber(generatedNumber)
                        .build()
        );
    }

    /**
     * Retrieves the current numbering configuration for the specified module.
     *
     * @param module the module to get the config for
     * @return the numbering configuration
     */
    @GetMapping("/config/{module}")
    public ResponseEntity<DocumentNumberConfigDto> getConfig(
            @PathVariable DocumentModule module
    ) {
        return ResponseEntity.ok(documentNumberService.getConfig(module));
    }

    /**
     * Updates the numbering configuration for the specified module.
     *
     * @param module  the module to update the config for
     * @param request the updated configuration values
     * @return the updated numbering configuration
     */
    @PutMapping("/config/{module}")
    public ResponseEntity<DocumentNumberConfigDto> updateConfig(
            @PathVariable DocumentModule module,
            @RequestBody @Valid UpsertDocumentNumberConfigDto request
    ) {
        return ResponseEntity.ok(documentNumberService.updateConfig(module, request));
    }

    /**
     * Lists all available modules for document number generation.
     *
     * @return list of module names
     */
    @GetMapping("/modules")
    public ResponseEntity<List<DocumentModule>> getAvailableModules() {
        return ResponseEntity.ok(Arrays.asList(DocumentModule.values()));
    }
}

