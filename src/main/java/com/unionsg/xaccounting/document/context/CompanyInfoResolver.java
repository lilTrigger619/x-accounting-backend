package com.unionsg.xaccounting.document.context;

import com.unionsg.xaccounting.dto.config.ConfigDto;
import com.unionsg.xaccounting.dto.config.ConfigItemDto;
import com.unionsg.xaccounting.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Resolves company information from the ConfigService.
 * Reads the COMPANY config key and maps items to a CompanyInfo DTO.
 */
@Component
@RequiredArgsConstructor
public class CompanyInfoResolver {

    private final ConfigService configService;

    public CompanyInfo resolve() {
        try {
            ConfigDto companyConfig = configService.getConfigByKey("COMPANY");

            if (companyConfig == null || companyConfig.getItems() == null) {
                return createDefaultCompanyInfo();
            }

            Map<String, String> configMap = companyConfig.getItems().stream()
                    .filter(item -> item.getCode() != null && item.getValue() != null)
                    .collect(Collectors.toMap(
                            item -> item.getCode().toLowerCase(),
                            ConfigItemDto::getValue,
                            (a, b) -> b
                    ));

            return CompanyInfo.builder()
                    .name(configMap.getOrDefault("name", "Your Company Name"))
                    .logoUrl(configMap.get("logo_url"))
                    .phone(configMap.getOrDefault("phone", ""))
                    .email(configMap.getOrDefault("email", ""))
                    .website(configMap.getOrDefault("website", ""))
                    .addressLine1(configMap.get("address_line1"))
                    .addressLine2(configMap.get("address_line2"))
                    .city(configMap.get("city"))
                    .state(configMap.get("state"))
                    .postalCode(configMap.get("postal_code"))
                    .country(configMap.get("country"))
                    .build();

        } catch (Exception e) {
            return createDefaultCompanyInfo();
        }
    }

    private CompanyInfo createDefaultCompanyInfo() {
        return CompanyInfo.builder()
                .name("Your Company")
                .phone("")
                .email("")
                .website("")
                .build();
    }
}

