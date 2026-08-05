package com.unionsg.xaccounting.config.seeders;

import com.unionsg.xaccounting.entity.DocumentNumberConfig;
import com.unionsg.xaccounting.enums.DocumentModule;
import com.unionsg.xaccounting.repository.DocumentNumberConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentTemplateConfigSeeder implements ApplicationRunner {

    private static final long DEFAULT_COMPANY_ID = 1L;
    private static final long DEFAULT_BRANCH_ID = 1L;

    private final DocumentNumberConfigRepository docConfigRepo;

    @Transactional
    public void run(ApplicationArguments args) {
        Map<DocumentModule, String> templateModules = new EnumMap<>(DocumentModule.class);
        templateModules.put(DocumentModule.DOCUMENT_TEMPLATE_INVOICE, "TMPL-INV");
        templateModules.put(DocumentModule.DOCUMENT_TEMPLATE_QUOTE, "TMPL-QTE");
        templateModules.put(DocumentModule.DOCUMENT_TEMPLATE_PURCHASE_ORDER, "TMPL-PO");
        templateModules.put(DocumentModule.DOCUMENT_TEMPLATE_CREDIT_NOTE, "TMPL-CN");
        templateModules.put(DocumentModule.DOCUMENT_TEMPLATE_DELIVERY_NOTE, "TMPL-DN");
        templateModules.put(DocumentModule.DOCUMENT_TEMPLATE_RECEIPT, "TMPL-RCT");

        templateModules.forEach((module, prefix) -> {
            boolean exists = docConfigRepo
                    .findByModuleAndCompanyIdAndBranchId(
                            module.name(),
                            DEFAULT_COMPANY_ID,
                            DEFAULT_BRANCH_ID
                    )
                    .isPresent();

            if (!exists) {
                DocumentNumberConfig config = new DocumentNumberConfig();
                config.setModule(module.name());
                config.setPrefix(prefix);
                config.setPadding(5);
                config.setLastNumber(0L);
                config.setSeparator("-");
                config.setIncludeYear(true);
                config.setIncludeMonth(false);
                config.setResetMonthly(false);
                config.setResetYearly(true);
                config.setCompanyId(DEFAULT_COMPANY_ID);
                config.setBranchId(DEFAULT_BRANCH_ID);
                config.setCreatedAt(LocalDateTime.now());
                docConfigRepo.save(config);
                log.info("Seeded document number config for module: {}", module.name());
            }
        });
    }
}

