package com.unionsg.xaccounting.document.context;

import com.unionsg.xaccounting.dto.settings.OrganizationResponse;
import com.unionsg.xaccounting.service.settings.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolves the company information shown on outbound documents from the Organization
 * profile (Settings & Setup §2) - the single source of truth for invoices, quotes,
 * receipts, statements, and payslips. Previously read from the generic "COMPANY" Config
 * category; that data is migrated into the Organization row by {@code SettingsSeeder}.
 */
@Component
@RequiredArgsConstructor
public class CompanyInfoResolver {

    private final OrganizationService organizationService;

    public CompanyInfo resolve() {
        try {
            OrganizationResponse org = organizationService.get();

            String name = org.getTradingName() != null && !org.getTradingName().isBlank()
                    ? org.getTradingName()
                    : (org.getLegalName() != null && !org.getLegalName().isBlank()
                            ? org.getLegalName() : "Your Company Name");

            return CompanyInfo.builder()
                    .name(name)
                    .logoUrl(org.getLogoFileId())
                    .phone(org.getPhone() != null ? org.getPhone() : "")
                    .email(org.getEmail() != null ? org.getEmail() : "")
                    .website(org.getWebsite() != null ? org.getWebsite() : "")
                    .addressLine1(org.getPrimaryAddressLine1())
                    .addressLine2(org.getPrimaryAddressLine2())
                    .city(org.getPrimaryCity())
                    .state(org.getPrimaryState())
                    .postalCode(org.getPrimaryPostalCode())
                    .country(org.getPrimaryCountry())
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

