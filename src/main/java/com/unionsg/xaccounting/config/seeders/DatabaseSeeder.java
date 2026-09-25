package com.unionsg.xaccounting.config.seeders;

import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.DocumentNumberConfig;
import com.unionsg.xaccounting.entity.User.Permission;
import com.unionsg.xaccounting.entity.User.Role;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.entity.ChartOfAccountClearTo_ENTITY;
import com.unionsg.xaccounting.entity.ChartOfAccount;
import com.unionsg.xaccounting.entity.TaxCategory;
import com.unionsg.xaccounting.enums.AccountType;
import com.unionsg.xaccounting.enums.NormalBalance;
import com.unionsg.xaccounting.enums.TaxCategoryType;
import com.unionsg.xaccounting.enums.UserStatus;
import com.unionsg.xaccounting.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;


@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class DatabaseSeeder implements ApplicationRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ChartOfAccountClearTo_Repository chartOfAccountClearToRepo;
    private final ChartOfAccountRepository chartOfAccountRepo;
    private final DocumentNumberConfigRepository docConfigRepo;

    private final AccountRepository accountRepository;
    private final TaxCategoryRepository taxCategoryRepository;
    private List<ChartOfAccountClearTo_ENTITY> assetsClearTo = new ArrayList<ChartOfAccountClearTo_ENTITY>();
    private List<ChartOfAccountClearTo_ENTITY> liabilityClearTo = new ArrayList<ChartOfAccountClearTo_ENTITY>();
    private List<ChartOfAccountClearTo_ENTITY> equityClearTo = new ArrayList<ChartOfAccountClearTo_ENTITY>();
    private List<ChartOfAccountClearTo_ENTITY> revenueClearTo = new ArrayList<ChartOfAccountClearTo_ENTITY>();
    private List<ChartOfAccountClearTo_ENTITY> expenseClearTo = new ArrayList<ChartOfAccountClearTo_ENTITY>();
    private List<ChartOfAccountClearTo_ENTITY> bankAccountClearTo = new ArrayList<ChartOfAccountClearTo_ENTITY>();
    private List<ChartOfAccountClearTo_ENTITY> creditCardClearTo = new ArrayList<ChartOfAccountClearTo_ENTITY>();
    private List<ChartOfAccountClearTo_ENTITY> costOfGoodsSoldClearTo = new ArrayList<ChartOfAccountClearTo_ENTITY>();
    private List<ChartOfAccountClearTo_ENTITY> accountsPayableClearTo = new ArrayList<ChartOfAccountClearTo_ENTITY>();
    private List<ChartOfAccountClearTo_ENTITY> accountsReceivableClearTo = new ArrayList<ChartOfAccountClearTo_ENTITY>();
    private User adminUser;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
//        if (permissionRepository.count() > 0) {
//            log.info("Database already seeded, skipping...");
//            return;
//        }
        if (userRepository.count() == 0){
            log.info("Seeding database...");

            // =============================
            // PERMISSION GROUPS
            // =============================

            // User Management
            List<Permission> userManagement = seedPermissionGroup("User Management", List.of(
                    "create_user",
                    "edit_user",
                    "delete_user",
                    "view_user",
                    "view_users",
                    "update_user"
            ));

            // Invoice Management
            List<Permission> invoiceManagement = seedPermissionGroup("Invoice Management", List.of(
                    "create_invoice",
                    "edit_invoice",
                    "delete_invoice",
                    "view_invoice"
            ));

            // Role Management
            List<Permission> roleManagement = seedPermissionGroup("Role Management", List.of(
                    "create_role",
                    "edit_role",
                    "delete_role",
                    "view_role",
                    "assign_role"
            ));

            // Report Management
            List<Permission> reportManagement = seedPermissionGroup("Report Management", List.of(
                    "view_report",
                    "export_report"
            ));
//            log.info("Database already seeded, skipping...");

            // =============================
            // ROLES
            // =============================
            Set<Permission> allPermissions = new HashSet<>();
            allPermissions.addAll(userManagement);
            allPermissions.addAll(invoiceManagement);
            allPermissions.addAll(roleManagement);
            allPermissions.addAll(reportManagement);

            Role superAdmin = createRole("Super Admin", "web", allPermissions);

            Role accountant = createRole("Accountant", "web", new HashSet<>(Set.of(
                    findPermission(invoiceManagement, "create_invoice"),
                    findPermission(invoiceManagement, "edit_invoice"),
                    findPermission(invoiceManagement, "view_invoice"),
                    findPermission(reportManagement,  "view_report"),
                    findPermission(reportManagement,  "export_report"),
                    findPermission(userManagement,    "view_user")
            )));

            Role viewer = createRole("Viewer", "web", new HashSet<>(Set.of(
                    findPermission(invoiceManagement, "view_invoice"),
                    findPermission(reportManagement,  "view_report"),
                    findPermission(userManagement,    "view_user")
            )));

            // =============================
            // USERS
            // =============================
            this.adminUser = createUser("admin@xaccounting.com",      "Admin",  "User",  "Admin@1234",      Set.of(superAdmin), Set.of());
            createUser("accountant@xaccounting.com", "John",   "Doe",   "Accountant@1234", Set.of(accountant), Set.of());
            createUser("viewer@xaccounting.com",     "Jane",   "Smith", "Viewer@1234",     Set.of(viewer),     Set.of());
        }

        // ==================================
        // Chart Code clear to
        // ==================================
        if (chartOfAccountClearToRepo.count() == 0){
            // all assets clear to
            // assetsClearTo.add( createChartClearToCode(1, "Current Asset"));
            // assetsClearTo.add(createChartClearToCode(2, "Fixed Asset"));
            // assetsClearTo.add(createChartClearToCode(3, "Other Asset"));
            // assetsClearTo.add(createChartClearToCode(4, "Bank"));
            // assetsClearTo.add(createChartClearToCode(5, "Cash"));
            // assetsClearTo.add(createChartClearToCode(6, "Inventory"));
            // assetsClearTo.add(createChartClearToCode(7, "Accounts receivable"));


            // Fixed Assets clear to codes
            assetsClearTo.add(createChartClearToCode(1, "Buildings"));
            assetsClearTo.add(createChartClearToCode(2, "Land"));
            assetsClearTo.add(createChartClearToCode(3, "Machinery & Equipment"));
            assetsClearTo.add(createChartClearToCode(4, "Furniture & Fixtures"));
            assetsClearTo.add(createChartClearToCode(5, "Vehicles"));
            assetsClearTo.add(createChartClearToCode(6, "Computers"));
            assetsClearTo.add(createChartClearToCode(7, "Software"));
            assetsClearTo.add(createChartClearToCode(8, "Leasehold Improvements"));
            assetsClearTo.add(createChartClearToCode(9, "Intangible Assets"));
            assetsClearTo.add(createChartClearToCode(10, "Accumulated Depreciation"));
            assetsClearTo.add(createChartClearToCode(11, "Accumulated Amortization"));
            assetsClearTo.add(createChartClearToCode(12, "Other Fixed Assets"));

// Liabilities clear to codes
            liabilityClearTo.add(createChartClearToCode(13, "Long term liability"));
            liabilityClearTo.add(createChartClearToCode(14, "Current liability"));
            liabilityClearTo.add(createChartClearToCode(15, "Accounts payable"));
            liabilityClearTo.add(createChartClearToCode(16, "Credit card"));
            liabilityClearTo.add(createChartClearToCode(17, "Other liability"));

// Equity clear to codes
            equityClearTo.add(createChartClearToCode(18, "Owners equity"));
            equityClearTo.add(createChartClearToCode(19, "Retained earnings"));
            equityClearTo.add(createChartClearToCode(20, "Other equity"));

// Revenue clear to codes
            revenueClearTo.add(createChartClearToCode(21, "Operating revenue"));
            revenueClearTo.add(createChartClearToCode(22, "Other income"));
            revenueClearTo.add(createChartClearToCode(23, "Sales"));
            revenueClearTo.add(createChartClearToCode(24, "Sales revenue"));

// Expense clear to codes
            expenseClearTo.add(createChartClearToCode(25, "Direct cost"));
            expenseClearTo.add(createChartClearToCode(26, "Operating expenses"));
            expenseClearTo.add(createChartClearToCode(27, "Payroll expense"));
            expenseClearTo.add(createChartClearToCode(28, "Other expense"));

// Bank account subtypes
            bankAccountClearTo.add(createChartClearToCode(29, "Cash on Hand"));
            bankAccountClearTo.add(createChartClearToCode(30, "Checking"));
            bankAccountClearTo.add(createChartClearToCode(31, "Savings"));
            bankAccountClearTo.add(createChartClearToCode(32, "Money Market"));
            bankAccountClearTo.add(createChartClearToCode(33, "Trust Account"));
            bankAccountClearTo.add(createChartClearToCode(34, "Rents Held in Trust"));

// Cost of Goods Sold
            costOfGoodsSoldClearTo.add(createChartClearToCode(35, "Cost of Goods Sold"));
            costOfGoodsSoldClearTo.add(createChartClearToCode(36, "Job Materials"));
            costOfGoodsSoldClearTo.add(createChartClearToCode(37, "Equipment Rental"));
            costOfGoodsSoldClearTo.add(createChartClearToCode(38, "Shipping/Freight"));
            costOfGoodsSoldClearTo.add(createChartClearToCode(39, "Subcontractor Costs"));
            costOfGoodsSoldClearTo.add(createChartClearToCode(40, "Direct Labor"));
            costOfGoodsSoldClearTo.add(createChartClearToCode(41, "Merchant Account Fees"));
            costOfGoodsSoldClearTo.add(createChartClearToCode(42, "Other Direct Costs"));

// Credit Card
            creditCardClearTo.add(createChartClearToCode(43, "Credit Card"));

// Accounts Payable
            accountsPayableClearTo.add(createChartClearToCode(44, "Accounts Payable"));

// Accounts Receivable
            accountsReceivableClearTo.add(createChartClearToCode(45, "Accounts Receivable"));
        }



        // =================================
        // Chart of account
        // ================================

        if (chartOfAccountRepo.count() == 0){
            createChartOfAccounts(1, "Asset", AccountType.ASSET, NormalBalance.DEBIT,assetsClearTo);
            createChartOfAccounts(2, "Liability", AccountType.LIABILITY, NormalBalance.CREDIT,liabilityClearTo);
            createChartOfAccounts(3, "Equity", AccountType.EQUITY, NormalBalance.CREDIT,equityClearTo);
            createChartOfAccounts(4, "Income", AccountType.INCOME, NormalBalance.CREDIT,revenueClearTo);
            createChartOfAccounts(5, "Expense", AccountType.EXPENSE , NormalBalance.DEBIT,expenseClearTo);
            createChartOfAccounts(6, "Bank Account", AccountType.ASSET, NormalBalance.DEBIT,bankAccountClearTo);
            createChartOfAccounts(7, "Credit Card", AccountType.LIABILITY, NormalBalance.CREDIT, creditCardClearTo);
            createChartOfAccounts(8, "Cost of Goods Sold", AccountType.EXPENSE,  NormalBalance.DEBIT, costOfGoodsSoldClearTo);
            createChartOfAccounts(9, "Accounts payable", AccountType.LIABILITY, NormalBalance.CREDIT, accountsPayableClearTo);
            createChartOfAccounts(10, "Accounts receivable", AccountType.ASSET, NormalBalance.DEBIT,accountsReceivableClearTo);


            log.info("Database seeding complete.");
        }


        // Document config seeding for setting account codes and invoice numbers.
        // we need this to set atleast one invoice number.
        if (docConfigRepo.count() == 0 ){
            // document config for document number
            DocumentNumberConfig invoiceConfig =  new DocumentNumberConfig();
            invoiceConfig.setModule("INVOICE");
            invoiceConfig.setPrefix("INV");
            invoiceConfig.setPadding(5);
            invoiceConfig.setLastNumber(0L);
            invoiceConfig.setSeparator("-");
            invoiceConfig.setIncludeYear(true);
            invoiceConfig.setResetMonthly(false);
            invoiceConfig.setIncludeMonth(false);
            invoiceConfig.setResetYearly(true);
            invoiceConfig.setCompanyId(1L);
            invoiceConfig.setBranchId(1L);
            invoiceConfig.setCreatedAt(LocalDateTime.now());
            docConfigRepo.save(invoiceConfig);


            DocumentNumberConfig journalConfig = new DocumentNumberConfig();
            journalConfig.setModule("JOURNAL");
            journalConfig.setPrefix("JNL");
            journalConfig.setPadding(5);
            journalConfig.setLastNumber(0L);
            journalConfig.setSeparator("-");
            journalConfig.setIncludeYear(true);
            journalConfig.setIncludeMonth(false);
            journalConfig.setResetMonthly(false);
            journalConfig.setResetYearly(true);
            journalConfig.setCompanyId(1L);
            journalConfig.setBranchId(1L);
            journalConfig.setCreatedAt(LocalDateTime.now());
            docConfigRepo.save(journalConfig);
        }


        if (accountRepository.count() == 0){
            //assets
            createAccount("1000", "Cash on Hand", 29L, chartOfAccountClearToRepo);
            createAccount("1010", "Checking Account", 30L, chartOfAccountClearToRepo);
            createAccount("1020", "Savings Account", 31L, chartOfAccountClearToRepo);
            createAccount("1030", "Money Market Account", 32L, chartOfAccountClearToRepo);

            //fixed assets
            createAccount("1500", "Buildings", 1L, chartOfAccountClearToRepo);
            createAccount("1510", "Land", 2L, chartOfAccountClearToRepo);
            createAccount("1520", "Machinery & Equipment", 3L, chartOfAccountClearToRepo);
            createAccount("1530", "Furniture & Fixtures", 4L, chartOfAccountClearToRepo);

            //fixed/ intangible assets
            createAccount("1600", "Vehicles", 5L, chartOfAccountClearToRepo);
            createAccount("1610", "Computers", 6L, chartOfAccountClearToRepo);
            createAccount("1620", "Software", 7L, chartOfAccountClearToRepo);
            createAccount("1630", "Leasehold Improvements", 8L, chartOfAccountClearToRepo);

            //Other assets/ adjusments
            createAccount("1700", "Intangible Assets", 9L, chartOfAccountClearToRepo);
            createAccount("1710", "Accumulated Depreciation", 10L, chartOfAccountClearToRepo);
            createAccount("1720", "Accumulated Amortization", 11L, chartOfAccountClearToRepo);
            createAccount("1730", "Other Fixed Assets", 12L, chartOfAccountClearToRepo);

            //liabilities
            createAccount("2000", "Long Term Liability", 13L, chartOfAccountClearToRepo);
            createAccount("2010", "Current Liability", 14L, chartOfAccountClearToRepo);
            createAccount("2020", "Other Creditors", 15L, chartOfAccountClearToRepo);
            createAccount("2030", "Credit Card Liability", 16L, chartOfAccountClearToRepo);

            //other liabilities
            createAccount("2040", "Other Liability", 17L, chartOfAccountClearToRepo);
            createAccount("2050", "Trust Account Liability", 33L, chartOfAccountClearToRepo);
            createAccount("2060", "Rents Held in Trust", 34L, chartOfAccountClearToRepo);
            createAccount("2070", "Merchant Account Fees Payable", 41L, chartOfAccountClearToRepo);

            // equity
            createAccount("3000", "Owners Equity", 18L, chartOfAccountClearToRepo);
            createAccount("3010", "Retained Earnings", 19L, chartOfAccountClearToRepo);
            createAccount("3020", "Other Equity", 20L, chartOfAccountClearToRepo);
            createAccount("3030", "Capital Contributions", 1L, chartOfAccountClearToRepo); // fallback grouping if needed

            //revenue
            createAccount("4000", "Operating Revenue", 21L, chartOfAccountClearToRepo);
            createAccount("4010", "Other Income", 22L, chartOfAccountClearToRepo);
            createAccount("4020", "Sales", 23L, chartOfAccountClearToRepo);
            createAccount("4030", "Sales Revenue", 24L, chartOfAccountClearToRepo);

            //expenses
            createAccount("5000", "Operating Expenses", 25L, chartOfAccountClearToRepo);
            createAccount("5010", "Payroll Expense", 26L, chartOfAccountClearToRepo);
            createAccount("5020", "Other Expense", 27L, chartOfAccountClearToRepo);
            createAccount("5030", "Shipping / Freight", 38L, chartOfAccountClearToRepo);

            //cost of goods sold
            createAccount("6000", "Cost of Goods Sold", 35L, chartOfAccountClearToRepo);
            createAccount("6010", "Job Materials", 36L, chartOfAccountClearToRepo);
            createAccount("6020", "Equipment Rental", 37L, chartOfAccountClearToRepo);
            createAccount("6030", "Subcontractor Costs", 39L, chartOfAccountClearToRepo);

            //direct cost
            createAccount("6100", "Direct Cost", 40L, chartOfAccountClearToRepo);
            createAccount("6110", "Direct Labor", 42L, chartOfAccountClearToRepo);
            createAccount("6120", "Other Direct Costs", 42L, chartOfAccountClearToRepo);
            createAccount("6130", "Credit Card Fees", 41L, chartOfAccountClearToRepo);

            //bank / financial fees /liability accounts
            createAccount("6200", "Credit Card Account", 43L, chartOfAccountClearToRepo);
            createAccount("6210", "Accounts Payable", 44L, chartOfAccountClearToRepo);
            createAccount("6220", "Accounts Receivable", 45L, chartOfAccountClearToRepo);


        }

        if (taxCategoryRepository.count() == 0) {
            taxCategoryRepository.save(TaxCategory.builder().name("Sales Tax (Standard)").type(TaxCategoryType.SALES_TAX).rate(new BigDecimal("5.00")).build());
            taxCategoryRepository.save(TaxCategory.builder().name("Sales Tax (Reduced)").type(TaxCategoryType.SALES_TAX).rate(new BigDecimal("2.50")).build());
            taxCategoryRepository.save(TaxCategory.builder().name("VAT (Standard)").type(TaxCategoryType.VAT).rate(new BigDecimal("15.00")).build());
            taxCategoryRepository.save(TaxCategory.builder().name("VAT (Zero-Rated)").type(TaxCategoryType.VAT).rate(BigDecimal.ZERO).build());
            taxCategoryRepository.save(TaxCategory.builder().name("Withholding Tax 5%").type(TaxCategoryType.WITHHOLDING_TAX).rate(new BigDecimal("5.00")).build());
            taxCategoryRepository.save(TaxCategory.builder().name("Withholding Tax 10%").type(TaxCategoryType.WITHHOLDING_TAX).rate(new BigDecimal("10.00")).build());
        }

        // Runs unconditionally (each guarded individually by existsByAccountId) so these two
        // control accounts get added even on a database that was already seeded before they
        // existed - unlike the blocks above, which only ever run once on a brand-new database.
        seedAdditionalControlAccountsIfMissing();
        seedPayrollDocumentConfigsIfMissing();
        seedControlAccountFlagsIfMissing();
    }

    /**
     * Flags the accounts every automated posting engine reconciles against a subledger
     * (Settings & Setup §8) as control accounts, so the manual Journal Entry screen refuses to
     * post to them directly - a manual entry here would silently desync the GL from the AR/AP
     * aging, payroll, or loan/advance subledger it mirrors. Runs unconditionally like the other
     * idempotent seed methods above, so an already-seeded database picks the flag up too.
     */
    private void seedControlAccountFlagsIfMissing() {
        List.of(
                "6220", // Accounts Receivable
                "6210", // Accounts Payable
                "2080", // Customer Deposits
                "1740", // Supplier Advances
                "2090", // Sales Tax Payable
                "2100", // Salary Payable
                "2110", // Employee Income Tax Payable
                "2120", // Statutory Contributions Payable - Employee
                "2130", // Statutory Contributions Payable - Employer
                "2140", // Employee Reimbursements Payable
                "1750", // Employee Loans Receivable
                "1760", // Salary Advances Receivable
                "1770", // Purchase Tax Receivable
                "2150"  // Withholding Tax Payable
        ).forEach(code -> accountRepository.findByAccountId(code).ifPresent(account -> {
            if (!Boolean.TRUE.equals(account.getIsControlAccount())) {
                account.setIsControlAccount(true);
                accountRepository.save(account);
            }
        }));
    }

    /**
     * Supplier Advances (a prepayment asset), Customer Deposits (unearned-revenue liability), and
     * Sales Tax Payable are referenced by {@code supplierpayment.journal.supplier-advances-account-id},
     * {@code payment.journal.customer-advances-account-id}, and
     * {@code invoice.journal.sales-tax-payable-account-id} in application.properties, but were
     * missing from the original chart-of-accounts seed - meaning every AR/AP posting involving an
     * over/under-payment (and every invoice GL posting) failed with "Account not found". Added
     * under the existing Asset/Liability ChartOfAccount groupings rather than a new chart type.
     */
    private void seedAdditionalControlAccountsIfMissing() {
        ChartOfAccount assetChart = chartOfAccountRepo.findByCoaCode(1L).orElse(null);
        ChartOfAccount liabilityChart = chartOfAccountRepo.findByCoaCode(2L).orElse(null);
        ChartOfAccount expenseChart = chartOfAccountRepo.findByCoaCode(5L).orElse(null);
        if (assetChart == null || liabilityChart == null || expenseChart == null) {
            log.warn("Asset/Liability/Expense chart-of-account groupings not found; skipping control account seeding");
            return;
        }

        if (!accountRepository.existsByAccountId("1740")) {
            ChartOfAccountClearTo_ENTITY supplierAdvancesClearTo = chartOfAccountClearToRepo.findByClearToCode(46L)
                    .orElseGet(() -> chartOfAccountClearToRepo.save(
                            ChartOfAccountClearTo_ENTITY.builder()
                                    .clearToCode(46L)
                                    .description("Supplier Advances")
                                    .chartOfAccount(assetChart)
                                    .build()));
            createAccount("1740", "Supplier Advances", supplierAdvancesClearTo.getClearToCode(), chartOfAccountClearToRepo);
        }

        if (!accountRepository.existsByAccountId("2080")) {
            ChartOfAccountClearTo_ENTITY customerDepositsClearTo = chartOfAccountClearToRepo.findByClearToCode(47L)
                    .orElseGet(() -> chartOfAccountClearToRepo.save(
                            ChartOfAccountClearTo_ENTITY.builder()
                                    .clearToCode(47L)
                                    .description("Customer Deposits")
                                    .chartOfAccount(liabilityChart)
                                    .build()));
            createAccount("2080", "Customer Deposits", customerDepositsClearTo.getClearToCode(), chartOfAccountClearToRepo);
        }

        if (!accountRepository.existsByAccountId("2090")) {
            ChartOfAccountClearTo_ENTITY salesTaxPayableClearTo = chartOfAccountClearToRepo.findByClearToCode(48L)
                    .orElseGet(() -> chartOfAccountClearToRepo.save(
                            ChartOfAccountClearTo_ENTITY.builder()
                                    .clearToCode(48L)
                                    .description("Sales Tax Payable")
                                    .chartOfAccount(liabilityChart)
                                    .build()));
            createAccount("2090", "Sales Tax Payable", salesTaxPayableClearTo.getClearToCode(), chartOfAccountClearToRepo);
        }

        seedPayrollControlAccountsIfMissing(assetChart, liabilityChart, expenseChart);
        addAccountIfMissing("1770", "Purchase Tax Receivable", assetChart, 60L);
        addAccountIfMissing("2150", "Withholding Tax Payable", liabilityChart, 61L);
    }

    /**
     * The payroll module's control accounts (§23-§26, §33, §53) - Salary Payable and the two
     * employee-balance receivables are single control accounts referenced from
     * application.properties; the expense/liability accounts are the defaults new PayComponent
     * and StatutoryScheme rows can point at out of the box. Added the same way the AR/AP control
     * accounts above were: idempotently, so an already-seeded database picks these up too.
     */
    private void seedPayrollControlAccountsIfMissing(ChartOfAccount assetChart, ChartOfAccount liabilityChart, ChartOfAccount expenseChart) {
        addAccountIfMissing("1750", "Employee Loans Receivable", assetChart, 49L);
        addAccountIfMissing("1760", "Salary Advances Receivable", assetChart, 50L);
        addAccountIfMissing("2100", "Salary Payable", liabilityChart, 51L);
        addAccountIfMissing("2110", "Employee Income Tax Payable", liabilityChart, 52L);
        addAccountIfMissing("2120", "Statutory Contributions Payable - Employee", liabilityChart, 53L);
        addAccountIfMissing("2130", "Statutory Contributions Payable - Employer", liabilityChart, 54L);
        addAccountIfMissing("2140", "Employee Reimbursements Payable", liabilityChart, 55L);
        addAccountIfMissing("5040", "Salaries and Wages Expense", expenseChart, 56L);
        addAccountIfMissing("5050", "Employer Statutory Contributions Expense", expenseChart, 57L);
        addAccountIfMissing("5060", "Employee Benefits Expense", expenseChart, 58L);
        addAccountIfMissing("5070", "Employee Reimbursement Expense", expenseChart, 59L);
    }

    private void addAccountIfMissing(String accountCode, String accountName, ChartOfAccount chart, Long clearToCode) {
        if (accountRepository.existsByAccountId(accountCode)) {
            return;
        }
        ChartOfAccountClearTo_ENTITY clearTo = chartOfAccountClearToRepo.findByClearToCode(clearToCode)
                .orElseGet(() -> chartOfAccountClearToRepo.save(
                        ChartOfAccountClearTo_ENTITY.builder()
                                .clearToCode(clearToCode)
                                .description(accountName)
                                .chartOfAccount(chart)
                                .build()));
        createAccount(accountCode, accountName, clearTo.getClearToCode(), chartOfAccountClearToRepo);
    }

    /** Idempotently seeds the EMPLOYEE and PAYROLL_RUN document-numbering configs (§20, §2). */
    private void seedPayrollDocumentConfigsIfMissing() {
        if (!docConfigRepo.existsByModuleAndCompanyIdAndBranchId("EMPLOYEE", 1L, 1L)) {
            DocumentNumberConfig employeeConfig = new DocumentNumberConfig();
            employeeConfig.setModule("EMPLOYEE");
            employeeConfig.setPrefix("EMP");
            employeeConfig.setPadding(4);
            employeeConfig.setLastNumber(0L);
            employeeConfig.setSeparator("-");
            employeeConfig.setIncludeYear(false);
            employeeConfig.setIncludeMonth(false);
            employeeConfig.setResetMonthly(false);
            employeeConfig.setResetYearly(false);
            employeeConfig.setCompanyId(1L);
            employeeConfig.setBranchId(1L);
            employeeConfig.setCreatedAt(LocalDateTime.now());
            docConfigRepo.save(employeeConfig);
        }

        if (!docConfigRepo.existsByModuleAndCompanyIdAndBranchId("PAYROLL_RUN", 1L, 1L)) {
            DocumentNumberConfig payrollRunConfig = new DocumentNumberConfig();
            payrollRunConfig.setModule("PAYROLL_RUN");
            payrollRunConfig.setPrefix("PR");
            payrollRunConfig.setPadding(5);
            payrollRunConfig.setLastNumber(0L);
            payrollRunConfig.setSeparator("-");
            payrollRunConfig.setIncludeYear(true);
            payrollRunConfig.setIncludeMonth(false);
            payrollRunConfig.setResetMonthly(false);
            payrollRunConfig.setResetYearly(true);
            payrollRunConfig.setCompanyId(1L);
            payrollRunConfig.setBranchId(1L);
            payrollRunConfig.setCreatedAt(LocalDateTime.now());
            docConfigRepo.save(payrollRunConfig);
        }
    }

    // =============================
    // HELPERS
    // =============================

    private List<Permission> seedPermissionGroup(String group, List<String> permissionNames) {
        log.info("Seeding permission group: {}", group);
        return permissionNames.stream()
                .map(name -> {
                    Permission p = new Permission();
                    p.setName(name);
                    p.setGuardName(group);   // guardName stores the group name
                    return permissionRepository.save(p);
                })
                .toList();
    }

    private Permission findPermission(List<Permission> group, String name) {
        return group.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Permission not found: " + name));
    }

    private Role createRole(String name, String guardName, Set<Permission> permissions) {
        Role r = new Role();
        r.setName(name);
        r.setGuardName(guardName);
        r.setPermissions(permissions);
        return roleRepository.save(r);
    }

    private User createUser(String email, String firstName, String lastName,
                            String rawPassword, Set<Role> roles, Set<Permission> permissions) {
        User u = User.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .password(passwordEncoder.encode(rawPassword))
                .status(UserStatus.ACTIVE)
                .roles(roles)
                .permissions(permissions)
                .build();
      return   userRepository.save(u);
    }

    private ChartOfAccountClearTo_ENTITY createChartClearToCode(long clearToCode, String description){
        ChartOfAccountClearTo_ENTITY ch = ChartOfAccountClearTo_ENTITY.builder()
                .clearToCode(clearToCode)
                .description(description)
                .build();
        return chartOfAccountClearToRepo.save(ch);
    }

    private void createChartOfAccounts(int chartCode, String chartDescription, AccountType accountType, NormalBalance normalBalance, List<ChartOfAccountClearTo_ENTITY> coa_ct){
        System.out.println("clear to account "+ coa_ct.size());
       ChartOfAccount coa = ChartOfAccount.builder()
               .coaCode( (long) chartCode)
               .coa_description(chartDescription)
               .coaClearTo(coa_ct)
               .accountType(accountType)
               .normalBalance(normalBalance)
               .build();
        coa.getCoaClearTo().forEach(coaClearTo -> coaClearTo.setChartOfAccount(coa));
       chartOfAccountRepo.save(coa);
    }

    private void createAccount(
            String accountNumber,
            String accountName,
            Long clearToCode,
            ChartOfAccountClearTo_Repository clearToRepo
    ) {

        ChartOfAccountClearTo_ENTITY clearTo =
                clearToRepo.findByClearToCode(clearToCode)
                        .orElseThrow(() ->
                                new RuntimeException("ClearTo not found: " + clearToCode)
                        );

        this.adminUser = userRepository.findByEmail("admin@xaccounting.com")   .orElseThrow(() ->
                                new RuntimeException("ClearTo not found: " + clearToCode)
                        );

        AccountEntity account = new AccountEntity();

        account.setAccountId(accountNumber);
        account.setAccountName(accountName);
        account.setCoaClearTo(clearTo);
        account.setCreatedBy(this.adminUser);

        account.setIsActive(true);
        account.setDeleted(false);

        accountRepository.save(account);
    }
}