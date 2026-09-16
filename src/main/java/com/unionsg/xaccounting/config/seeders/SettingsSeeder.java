package com.unionsg.xaccounting.config.seeders;

import com.unionsg.xaccounting.dto.config.ConfigDto;
import com.unionsg.xaccounting.dto.config.ConfigItemDto;
import com.unionsg.xaccounting.entity.DocumentNumberConfig;
import com.unionsg.xaccounting.entity.DocumentSequence;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.entity.settings.Organization;
import com.unionsg.xaccounting.enums.settings.BankAccountStatus;
import com.unionsg.xaccounting.repository.DocumentNumberConfigRepository;
import com.unionsg.xaccounting.repository.DocumentSequenceRepository;
import com.unionsg.xaccounting.repository.settings.BankAccountRepository;
import com.unionsg.xaccounting.repository.settings.OrganizationRepository;
import com.unionsg.xaccounting.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * One-time, idempotent setup for the Settings & Setup Center (§2, §14, §16). Runs after the
 * other seeders (§102-117's Payroll seeder included) so the Chart of Accounts it links against
 * already exists.
 *
 * <p>Three jobs, each independently idempotent:</p>
 * <ol>
 *   <li>Migrate the ad hoc "COMPANY" Config category into the new {@link Organization} row, so
 *   existing installs don't lose their company info when this replaces that config as the
 *   source of truth for document rendering.</li>
 *   <li>Seed one default {@link BankAccount} against the Bank GL account, so a fresh install
 *   isn't Payments/Banking-broken out of the box.</li>
 *   <li>Migrate Bill/Supplier-Payment/Receipt numbering onto the central
 *   {@code DocumentNumberConfig} system, carrying over each legacy {@link DocumentSequence}'s
 *   current value so already-issued numbers are never reissued (§14: "must never generate
 *   duplicate transaction numbers").</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(4)
public class SettingsSeeder implements ApplicationRunner {

    private final OrganizationRepository organizationRepository;
    private final ConfigService configService;
    private final BankAccountRepository bankAccountRepository;
    private final DocumentNumberConfigRepository documentNumberConfigRepository;
    private final DocumentSequenceRepository documentSequenceRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        migrateCompanyConfigToOrganization();
        seedDefaultBankAccount();
        migrateLegacyNumberSequences();
    }

    private void migrateCompanyConfigToOrganization() {
        if (organizationRepository.count() > 0) {
            return;
        }
        Organization org = new Organization();
        try {
            ConfigDto companyConfig = configService.getConfigByKey("COMPANY");
            if (companyConfig != null && companyConfig.getItems() != null) {
                Map<String, String> values = companyConfig.getItems().stream()
                        .filter(i -> i.getCode() != null && i.getValue() != null)
                        .collect(Collectors.toMap(
                                i -> i.getCode().toUpperCase(),
                                ConfigItemDto::getValue,
                                (a, b) -> b));
                org.setLegalName(values.get("COMPANY_NAME"));
                org.setTradingName(values.get("COMPANY_NAME"));
                org.setLogoFileId(values.get("COMPANY_LOGO_URL"));
                org.setPhone(values.get("COMPANY_PHONE"));
                org.setEmail(values.get("COMPANY_EMAIL"));
                org.setWebsite(values.get("COMPANY_WEBSITE"));
                org.setPrimaryAddressLine1(values.get("COMPANY_ADDRESS_LINE1"));
                org.setPrimaryAddressLine2(values.get("COMPANY_ADDRESS_LINE2"));
                org.setPrimaryCity(values.get("COMPANY_CITY"));
                org.setPrimaryState(values.get("COMPANY_STATE"));
                org.setPrimaryPostalCode(values.get("COMPANY_POSTAL_CODE"));
                org.setPrimaryCountry(values.get("COMPANY_COUNTRY"));
                log.info("Migrated COMPANY config into the Organization profile");
            }
        } catch (Exception e) {
            log.info("No existing COMPANY config to migrate; starting with a blank Organization profile");
        }
        organizationRepository.save(org);
    }

    private void seedDefaultBankAccount() {
        if (bankAccountRepository.count() > 0) {
            return;
        }
        BankAccount account = new BankAccount();
        account.setBankName("Primary Bank");
        account.setAccountName("Operating Account");
        account.setCurrency("USD");
        account.setGlAccountCode("1010");
        account.setStatus(BankAccountStatus.ACTIVE);
        account.setIsDefault(true);
        account.setEnableReconciliation(true);
        bankAccountRepository.save(account);
        log.info("Seeded default bank account against GL account 1010");
    }

    private void migrateLegacyNumberSequences() {
        migrateOneSequence("BILL", "BILL", "BILL");
        migrateOneSequence("SUPPLIER_PAYMENT", "SUPPLIER_PAYMENT", "SPMT");
        migrateOneSequence("PAYMENT", "PAYMENT", "RCP");
    }

    /**
     * @param module           the {@code DocumentModule} name the app now generates numbers under
     * @param legacySequenceCode the {@code DocumentSequence.code} the old standalone generator used
     * @param prefix           the legacy generator's fixed prefix, preserved so number format doesn't change
     */
    private void migrateOneSequence(String module, String legacySequenceCode, String prefix) {
        if (documentNumberConfigRepository.existsByModuleAndCompanyIdAndBranchId(module, 0L, 0L)) {
            return;
        }
        long carriedOverValue = documentSequenceRepository.findByCode(legacySequenceCode)
                .map(DocumentSequence::getCurrentValue)
                .orElse(0L);

        DocumentNumberConfig config = new DocumentNumberConfig();
        config.setModule(module);
        config.setCompanyId(0L);
        config.setBranchId(0L);
        config.setPrefix(prefix);
        config.setLastNumber(carriedOverValue);
        config.setPadding(6);
        config.setIncludeYear(true);
        config.setIncludeMonth(true);
        config.setResetYearly(false);
        config.setResetMonthly(false);
        config.setSeparator("-");
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        documentNumberConfigRepository.save(config);
        log.info("Migrated {} numbering onto the central Numbering & Sequences config (continuing from {})",
                module, carriedOverValue);
    }
}
