package com.unionsg.xaccounting.config.seeders;

import com.unionsg.xaccounting.entity.configuration.Config;
import com.unionsg.xaccounting.entity.configuration.ConfigItem;
import com.unionsg.xaccounting.repository.config.ConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Seeds the COMPANY configuration used by the document rendering pipeline
 * (see {@code CompanyInfoResolver} which reads config key "COMPANY").
 *
 * <p>This provides realistic company details so that invoice templates
 * (classic/modern/professional) render company name, address, phone, email,
 * and website when previewing.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyConfigSeeder implements ApplicationRunner {

    private static final String COMPANY_CONFIG_KEY = "COMPANY";

    private final ConfigRepository configRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedCompanyConfig();
    }

    private void seedCompanyConfig() {
        if (configRepository.findByConfigKey(COMPANY_CONFIG_KEY).isPresent()) {
            log.info("COMPANY config already exists, skipping seed.");
            return;
        }

        Config config = Config.builder()
                .configKey(COMPANY_CONFIG_KEY)
                .title("Company Information")
                .description("Business details used across invoices and generated documents.")
                .itemLabel("Company Detail")
                .showValueField(true)
                .valueFieldLabel("Value")
                .valueFieldPlaceholder("e.g. Acme Business Solutions Ltd.")
                .systemDefined(true)
                .status("ACTIVE")
                .sortOrder(1)
                .build();

        List<ConfigItem> items = new ArrayList<>();
        items.add(item("name", "COMPANY_NAME", "Acme Business Solutions Ltd.", "Legal/registered business name", true, 1));
        items.add(item("logo_url", "COMPANY_LOGO_URL", null, "URL or file id of the company logo", false, 2));
        items.add(item("phone", "COMPANY_PHONE", "+233 20 123 4567", "Primary business phone number", false, 3));
        items.add(item("email", "COMPANY_EMAIL", "billing@acme.example", "Primary billing/contact email", false, 4));
        items.add(item("website", "COMPANY_WEBSITE", "www.acme.example", "Company website", false, 5));
        items.add(item("address_line1", "COMPANY_ADDRESS_LINE1", "12 Independence Avenue", "Street address line 1", false, 6));
        items.add(item("address_line2", "COMPANY_ADDRESS_LINE2", null, "Street address line 2", false, 7));
        items.add(item("city", "COMPANY_CITY", "Accra", "City", false, 8));
        items.add(item("state", "COMPANY_STATE", "Greater Accra", "State/Region", false, 9));
        items.add(item("postal_code", "COMPANY_POSTAL_CODE", null, "Postal / ZIP code", false, 10));
        items.add(item("country", "COMPANY_COUNTRY", "Ghana", "Country", false, 11));

        config.setItems(items);
        items.forEach(i -> i.setConfig(config));

        configRepository.save(config);
        log.info("Seeded COMPANY config with {} items.", items.size());
    }

    private ConfigItem item(
            String code,
            String name,
            String value,
            String description,
            Boolean isDefault,
            Integer sortOrder
    ) {
        return ConfigItem.builder()
                .name(name)
                .code(code)
                .value(value)
                .description(description)
                .isDefault(isDefault)
                .sortOrder(sortOrder)
                .status("ACTIVE")
                .build();
    }
}
