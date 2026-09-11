package com.unionsg.xaccounting.config.seeders;

import com.unionsg.xaccounting.dto.accounting.CreateFinancialYearRequest;
import com.unionsg.xaccounting.dto.accounting.CreateOpeningBalanceRequest;
import com.unionsg.xaccounting.dto.accounting.CreateRecurringJournalTemplateRequest;
import com.unionsg.xaccounting.dto.accounting.FinancialYearResponse;
import com.unionsg.xaccounting.dto.accounting.OpeningBalanceLineRequest;
import com.unionsg.xaccounting.dto.accounting.AccountingPeriodResponse;
import com.unionsg.xaccounting.dto.accounting.RecurringJournalTemplateLineRequest;
import com.unionsg.xaccounting.dto.bill.BillItemRequest;
import com.unionsg.xaccounting.dto.bill.BillResponse;
import com.unionsg.xaccounting.dto.bill.CreateBillRequest;
import com.unionsg.xaccounting.dto.customer.AddressDTO;
import com.unionsg.xaccounting.dto.customer.CreateCustomerRequestDTO;
import com.unionsg.xaccounting.dto.customer.CustomerResponseDTO;
import com.unionsg.xaccounting.dto.customer.PaymentTermsDTO;
import com.unionsg.xaccounting.dto.customer.TaxInfoDTO;
import com.unionsg.xaccounting.dto.invoice.CreateInvoiceRequest;
import com.unionsg.xaccounting.dto.invoice.InvoiceItemRequest;
import com.unionsg.xaccounting.dto.invoice.InvoiceResponse;
import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.payment.AllocatePaymentRequest;
import com.unionsg.xaccounting.dto.payment.CreatePaymentRequest;
import com.unionsg.xaccounting.dto.payment.CreatePaymentResponse;
import com.unionsg.xaccounting.dto.payment.PaymentAllocationRequest;
import com.unionsg.xaccounting.dto.product.CreateProductRequest;
import com.unionsg.xaccounting.dto.supplier.CreateSupplierRequestDTO;
import com.unionsg.xaccounting.dto.supplier.SupplierPaymentTermsDTO;
import com.unionsg.xaccounting.dto.supplier.SupplierResponseDTO;
import com.unionsg.xaccounting.dto.supplier.SupplierTaxInfoDTO;
import com.unionsg.xaccounting.dto.supplierpayment.AllocateSupplierPaymentRequest;
import com.unionsg.xaccounting.dto.supplierpayment.CreateSupplierPaymentRequest;
import com.unionsg.xaccounting.dto.supplierpayment.CreateSupplierPaymentResponse;
import com.unionsg.xaccounting.dto.supplierpayment.SupplierPaymentAllocationRequest;
import com.unionsg.xaccounting.enums.Currency;
import com.unionsg.xaccounting.enums.DiscountType;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.enums.PaymentMethod;
import com.unionsg.xaccounting.enums.ProductItemType;
import com.unionsg.xaccounting.enums.RecurringJournalFrequency;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.TaxCategoryRepository;
import com.unionsg.xaccounting.repository.accounting.FinancialYearRepository;
import com.unionsg.xaccounting.service.accounting.AccountingPeriodService;
import com.unionsg.xaccounting.service.accounting.FinancialYearService;
import com.unionsg.xaccounting.service.accounting.OpeningBalanceService;
import com.unionsg.xaccounting.service.accounting.RecurringJournalService;
import com.unionsg.xaccounting.service.accounting.YearEndClosingService;
import com.unionsg.xaccounting.service.bill.BillService;
import com.unionsg.xaccounting.service.customer.CustomerService;
import com.unionsg.xaccounting.service.invoice.InvoiceService;
import com.unionsg.xaccounting.service.journal.JournalService;
import com.unionsg.xaccounting.service.payment.PaymentAllocationService;
import com.unionsg.xaccounting.service.payment.PaymentService;
import com.unionsg.xaccounting.service.payment.SupplierPaymentAllocationService;
import com.unionsg.xaccounting.service.payment.SupplierPaymentService;
import com.unionsg.xaccounting.service.product.ProductService;
import com.unionsg.xaccounting.service.supplier.SupplierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Populates a fresh install with a realistic, internally-consistent set of demo data by driving
 * the same business services a real user would - not raw repository saves - so the numbers that
 * show up on every report are the product of the product's own double-entry engine actually
 * running: customers/suppliers/products, a closed prior Financial Year (with an opening balance
 * and a real Year-End Closing), an active current Financial Year with two locked periods, ~21
 * months of invoices/bills with a realistic mix of paid/partial/unpaid outcomes, two Recurring
 * Journal templates generating real monthly entries across the year boundary, and one manual
 * adjusting journal.
 *
 * <p>Runs once: guarded by {@code financialYearRepository.count() == 0} so restarting the app
 * never duplicates data. Must run after {@link DatabaseSeeder} (chart of accounts, tax
 * categories, document number sequences) - see {@code @Order}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
public class DemoDataSeeder implements ApplicationRunner {

    private static final String RETAINED_EARNINGS_ACCOUNT = "3010";
    private static final String CHECKING_ACCOUNT = "1010";
    private static final String OWNERS_EQUITY_ACCOUNT = "3000";
    private static final String OPERATING_REVENUE_ACCOUNT = "4020";
    private static final String OPERATING_EXPENSE_ACCOUNT = "5000";
    private static final String ACCUMULATED_DEPRECIATION_ACCOUNT = "1710";
    private static final String OTHER_EXPENSE_ACCOUNT = "5020";
    private static final String SUPPLIER_ADVANCES_ACCOUNT = "1740";

    private final FinancialYearRepository financialYearRepository;
    private final AccountRepository accountRepository;
    private final TaxCategoryRepository taxCategoryRepository;

    private final CustomerService customerService;
    private final SupplierService supplierService;
    private final ProductService productService;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final PaymentAllocationService paymentAllocationService;
    private final BillService billService;
    private final SupplierPaymentService supplierPaymentService;
    private final SupplierPaymentAllocationService supplierPaymentAllocationService;
    private final JournalService journalService;

    private final FinancialYearService financialYearService;
    private final AccountingPeriodService accountingPeriodService;
    private final OpeningBalanceService openingBalanceService;
    private final YearEndClosingService yearEndClosingService;
    private final RecurringJournalService recurringJournalService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        if (financialYearRepository.count() > 0) {
            log.info("Demo data already seeded, skipping.");
            return;
        }
        if (!accountRepository.existsByAccountId(CHECKING_ACCOUNT)) {
            log.warn("Chart of accounts not seeded yet; skipping demo data seeding this run.");
            return;
        }

        log.info("Seeding demo data for investor-facing environment...");

        List<Long> customerIds = seedCustomers();
        List<Long> supplierIds = seedSuppliers();
        seedProducts();

        LocalDate fy2025Start = LocalDate.of(2025, 1, 1);
        LocalDate fy2025End = LocalDate.of(2025, 12, 31);
        FinancialYearResponse fy2025 = financialYearService.create(
                newFinancialYearRequest("FY 2025", fy2025Start, fy2025End)
        );
        financialYearService.activate(fy2025.getId());
        seedOpeningBalance(fy2025.getId(), fy2025Start);

        LocalDate today = LocalDate.now();
        LocalDate seedThrough = today.withDayOfMonth(1);

        seedInvoicesAndPayments(customerIds, fy2025Start, seedThrough);
        seedBillsAndPayments(supplierIds, fy2025Start, seedThrough);
        seedRecurringJournals(fy2025Start);
        seedManualAdjustingJournal();

        yearEndClosingService.closeFinancialYear(fy2025.getId());
        log.info("Closed FY 2025 - net profit/loss transferred to Retained Earnings.");

        LocalDate fy2026Start = LocalDate.of(2026, 1, 1);
        LocalDate fy2026End = LocalDate.of(2026, 12, 31);
        FinancialYearResponse fy2026 = financialYearService.create(
                newFinancialYearRequest("FY 2026", fy2026Start, fy2026End)
        );
        financialYearService.activate(fy2026.getId());
        lockEarlyPeriods(fy2026.getId(), 2);

        log.info("Demo data seeding complete.");
    }

    // ==========================================================================================
    // Customers / Suppliers / Products
    // ==========================================================================================

    private List<Long> seedCustomers() {
        List<Long> ids = new ArrayList<>();
        ids.add(createCustomer("BUSN", "Nova Retail Group", "billing@novaretail.com", "NET30", new BigDecimal("25000")));
        ids.add(createCustomer("BUSN", "Brightline Logistics LLC", "accounts@brightlinelogistics.com", "NET30", new BigDecimal("30000")));
        ids.add(createCustomer("BUSN", "Solstice Media Co.", "finance@solsticemedia.com", "NET15", new BigDecimal("15000")));
        ids.add(createCustomer("BUSN", "Harbor & Finch Consulting", "ap@harborfinch.com", "NET30", new BigDecimal("20000")));
        ids.add(createCustomer("BUSN", "Cedarwood Interiors", "billing@cedarwoodinteriors.com", "NET45", new BigDecimal("18000")));
        ids.add(createCustomer("BUSN", "Pinnacle Fitness Studios", "accounts@pinnaclefitness.com", "NET15", new BigDecimal("10000")));
        ids.add(createCustomer("INDV", "Maren Cole", "maren.cole@example.com", "DUE_ON_RECEIPT", new BigDecimal("5000")));
        ids.add(createCustomer("BUSN", "Quill & Ember Publishing", "finance@quillember.com", "NET30", new BigDecimal("22000")));
        log.info("Seeded {} demo customers.", ids.size());
        return ids;
    }

    private Long createCustomer(String customerType, String displayName, String email, String paymentTermType, BigDecimal creditLimit) {
        CreateCustomerRequestDTO request = new CreateCustomerRequestDTO();
        request.setCustomerType(customerType);
        request.setFirstName(customerType.equals("INDV") ? displayName.split(" ")[0] : null);
        request.setLastName(customerType.equals("INDV") ? displayName.split(" ")[1] : null);
        request.setCompanyName(customerType.equals("BUSN") ? displayName : null);
        request.setDisplayName(displayName);
        request.setStatus("ACTIVE");
        request.setEmail(email);
        request.setPhone("+1-555-0100");

        AddressDTO address = AddressDTO.builder()
                .addressLine("100 Market Street")
                .city("Austin")
                .state("TX")
                .zipCode("73301")
                .country("USA")
                .build();
        request.setBillingAddress(address);
        request.setShippingAddress(address);

        TaxInfoDTO taxInfo = new TaxInfoDTO();
        taxInfo.setTaxId(null);
        taxInfo.setTaxExempt(false);
        request.setTaxInfo(taxInfo);

        PaymentTermsDTO paymentTerms = PaymentTermsDTO.builder()
                .paymentTermType(paymentTermType)
                .creditLimit(creditLimit)
                .currency("USD")
                .build();
        request.setPaymentTerms(paymentTerms);

        CustomerResponseDTO response = customerService.createCustomer(request);
        return response.getId();
    }

    private List<Long> seedSuppliers() {
        List<Long> ids = new ArrayList<>();
        ids.add(createSupplier("Atlas Office Supply Co.", "orders@atlasoffice.com", "OFFICE_SUPPLIES"));
        ids.add(createSupplier("Keystone Cloud Hosting", "billing@keystonecloud.com", "SERVICES"));
        ids.add(createSupplier("Meridian Freight & Logistics", "ap@meridianfreight.com", "LOGISTICS"));
        ids.add(createSupplier("Voltage Utilities Inc.", "billing@voltageutilities.com", "UTILITIES"));
        ids.add(createSupplier("Falcon Equipment Rentals", "accounts@falconrentals.com", "EQUIPMENT"));
        log.info("Seeded {} demo suppliers.", ids.size());
        return ids;
    }

    private Long createSupplier(String displayName, String email, String category) {
        CreateSupplierRequestDTO request = new CreateSupplierRequestDTO();
        request.setSupplierType("BUSN");
        request.setCompanyName(displayName);
        request.setContactPerson("Accounts Team");
        request.setDisplayName(displayName);
        request.setStatus("ACTIVE");
        request.setCategory(category);
        request.setEmail(email);
        request.setPhone("+1-555-0200");

        AddressDTO address = AddressDTO.builder()
                .addressLine("500 Commerce Ave")
                .city("Dallas")
                .state("TX")
                .zipCode("75201")
                .country("USA")
                .build();
        request.setAddress(address);

        SupplierTaxInfoDTO taxInfo = new SupplierTaxInfoDTO();
        taxInfo.setWithholding(false);
        request.setTaxInfo(taxInfo);

        SupplierPaymentTermsDTO paymentTerms = new SupplierPaymentTermsDTO();
        paymentTerms.setPaymentTermType("NET30");
        paymentTerms.setPaymentMethod("BANK_TRANSFER");
        paymentTerms.setCurrency("USD");
        request.setPaymentTerms(paymentTerms);

        SupplierResponseDTO response = supplierService.createSupplier(request);
        return response.getId();
    }

    private void seedProducts() {
        Long incomeAccountId = accountRepository.findByAccountId(OPERATING_REVENUE_ACCOUNT)
                .orElseThrow(() -> new IllegalStateException("Operating Revenue account not seeded")).getId();
        Long taxCategoryId = taxCategoryRepository.findByDeletedFalse().stream()
                .filter(tc -> tc.getName().contains("VAT (Standard)"))
                .findFirst()
                .map(tc -> tc.getId())
                .orElse(null);

        createProduct("Strategy Consulting Retainer", ProductItemType.SERVICE, new BigDecimal("2500.00"), incomeAccountId, taxCategoryId);
        createProduct("Brand Design Package", ProductItemType.SERVICE, new BigDecimal("1800.00"), incomeAccountId, taxCategoryId);
        createProduct("Monthly Support Plan", ProductItemType.SERVICE, new BigDecimal("450.00"), incomeAccountId, taxCategoryId);
        createProduct("Custom Software License", ProductItemType.NON_INVENTORY, new BigDecimal("3200.00"), incomeAccountId, taxCategoryId);
        createProduct("On-site Training Day", ProductItemType.SERVICE, new BigDecimal("1200.00"), incomeAccountId, taxCategoryId);
        createProduct("Hardware Bundle - Standard", ProductItemType.INVENTORY, new BigDecimal("950.00"), incomeAccountId, taxCategoryId);
        log.info("Seeded 6 demo products.");
    }

    private void createProduct(String name, ProductItemType itemType, BigDecimal price, Long incomeAccountId, Long taxCategoryId) {
        CreateProductRequest request = new CreateProductRequest();
        request.setName(name);
        request.setItemType(itemType);
        request.setPrice(price);
        request.setIncomeAccountId(incomeAccountId);
        request.setTaxCategoryId(taxCategoryId);
        productService.createProduct(null, request);
    }

    // ==========================================================================================
    // Opening Balance
    // ==========================================================================================

    private void seedOpeningBalance(Long financialYearId, LocalDate fyStart) {
        Long checkingId = resolveAccountPk(CHECKING_ACCOUNT);
        Long ownersEquityId = resolveAccountPk(OWNERS_EQUITY_ACCOUNT);

        OpeningBalanceLineRequest debit = new OpeningBalanceLineRequest();
        debit.setAccountId(checkingId);
        debit.setDescription("Initial capitalization");
        debit.setDebitAmount(new BigDecimal("75000.00"));
        debit.setCreditAmount(BigDecimal.ZERO);

        OpeningBalanceLineRequest credit = new OpeningBalanceLineRequest();
        credit.setAccountId(ownersEquityId);
        credit.setDescription("Initial capitalization");
        credit.setDebitAmount(BigDecimal.ZERO);
        credit.setCreditAmount(new BigDecimal("75000.00"));

        CreateOpeningBalanceRequest request = CreateOpeningBalanceRequest.builder()
                .financialYearId(financialYearId)
                .lines(List.of(debit, credit))
                .build();

        openingBalanceService.create(request);
        log.info("Posted opening balance for financial year starting {}", fyStart);
    }

    // ==========================================================================================
    // Invoices + AR Payments
    // ==========================================================================================

    private void seedInvoicesAndPayments(List<Long> customerIds, LocalDate from, LocalDate throughMonthStart) {
        int index = 0;
        LocalDate cursor = from;

        while (!cursor.isAfter(throughMonthStart)) {
            for (int slot = 0; slot < 2; slot++) {
                LocalDate issueDate = cursor.withDayOfMonth(slot == 0 ? 5 : 20);
                Long customerId = customerIds.get(index % customerIds.size());
                BigDecimal baseAmount = new BigDecimal(800 + (index % 7) * 350);

                InvoiceResponse invoice = createInvoice(customerId, issueDate, baseAmount);

                int outcome = index % 5;
                if (outcome == 4) {
                    // left entirely unpaid - ages into the AR aging report
                } else if (outcome == 3) {
                    BigDecimal partial = invoice.getTotalAmount().multiply(new BigDecimal("0.55")).setScale(2, java.math.RoundingMode.HALF_UP);
                    payAndAllocateInvoice(customerId, invoice.getId(), issueDate.plusDays(10), partial);
                } else {
                    payAndAllocateInvoice(customerId, invoice.getId(), issueDate.plusDays(12), invoice.getTotalAmount());
                }

                index++;
            }
            cursor = cursor.plusMonths(1);
        }
        log.info("Seeded {} demo invoices with realistic payment outcomes.", index);
    }

    private InvoiceResponse createInvoice(Long customerId, LocalDate issueDate, BigDecimal baseAmount) {
        InvoiceItemRequest item1 = new InvoiceItemRequest();
        item1.setDescription("Professional services rendered");
        item1.setQuantity(BigDecimal.ONE);
        item1.setUnitPrice(baseAmount);
        item1.setTaxRate(new BigDecimal("15"));

        InvoiceItemRequest item2 = new InvoiceItemRequest();
        item2.setDescription("Platform & support fee");
        item2.setQuantity(BigDecimal.ONE);
        item2.setUnitPrice(baseAmount.multiply(new BigDecimal("0.2")).setScale(2, java.math.RoundingMode.HALF_UP));
        item2.setTaxRate(new BigDecimal("15"));

        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setIssueDate(issueDate);
        request.setDueDate(issueDate.plusDays(30));
        request.setCustomerId(customerId);
        request.setDiscountType(DiscountType.NONE);
        request.setDiscountValue(BigDecimal.ZERO);
        request.setItems(List.of(item1, item2));

        InvoiceResponse invoice = invoiceService.createInvoice(null, request);
        invoiceService.sendInvoice(invoice.getId());
        return invoice;
    }

    private void payAndAllocateInvoice(Long customerId, Long invoiceId, LocalDate paymentDate, BigDecimal amount) {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setCustomerId(customerId);
        request.setPaymentDate(paymentDate);
        request.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        request.setCurrency(Currency.USD);
        request.setExchangeRate(BigDecimal.ONE);
        request.setAmountReceived(amount);

        CreatePaymentResponse payment = paymentService.createPayment(request);

        PaymentAllocationRequest allocation = new PaymentAllocationRequest();
        allocation.setInvoiceId(invoiceId);
        allocation.setAllocatedAmount(amount);

        AllocatePaymentRequest allocateRequest = new AllocatePaymentRequest();
        allocateRequest.setAllocations(List.of(allocation));

        paymentAllocationService.allocatePayment(payment.getPaymentId(), allocateRequest);
    }

    // ==========================================================================================
    // Bills + AP Payments
    // ==========================================================================================

    private void seedBillsAndPayments(List<Long> supplierIds, LocalDate from, LocalDate throughMonthStart) {
        int index = 0;
        LocalDate cursor = from;

        while (!cursor.isAfter(throughMonthStart)) {
            LocalDate billDate = cursor.withDayOfMonth(10);
            Long supplierId = supplierIds.get(index % supplierIds.size());
            BigDecimal baseAmount = new BigDecimal(400 + (index % 5) * 220);

            BillResponse bill = createBill(supplierId, billDate, baseAmount);
            billService.approveBill(bill.getId());

            if (index % 4 != 3) {
                payAndAllocateBill(supplierId, bill.getId(), billDate.plusDays(15), bill.getTotalAmount());
            }
            // else: left OPEN - ages into the AP aging report

            index++;
            cursor = cursor.plusMonths(1);
        }
        log.info("Seeded {} demo bills with realistic payment outcomes.", index);
    }

    private BillResponse createBill(Long supplierId, LocalDate billDate, BigDecimal baseAmount) {
        BillItemRequest item = new BillItemRequest();
        item.setDescription("Recurring vendor services");
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(baseAmount);
        item.setTaxRate(new BigDecimal("15"));

        CreateBillRequest request = new CreateBillRequest();
        request.setBillDate(billDate);
        request.setDueDate(billDate.plusDays(30));
        request.setSupplierId(supplierId);
        request.setDiscountType(DiscountType.NONE);
        request.setDiscountValue(BigDecimal.ZERO);
        request.setItems(List.of(item));

        return billService.createBill(request);
    }

    private void payAndAllocateBill(Long supplierId, Long billId, LocalDate paymentDate, BigDecimal amount) {
        CreateSupplierPaymentRequest request = new CreateSupplierPaymentRequest();
        request.setSupplierId(supplierId);
        request.setPaymentDate(paymentDate);
        request.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        request.setCurrency(Currency.USD);
        request.setExchangeRate(BigDecimal.ONE);
        request.setAmountPaid(amount);

        CreateSupplierPaymentResponse payment = supplierPaymentService.createPayment(request);

        SupplierPaymentAllocationRequest allocation = new SupplierPaymentAllocationRequest();
        allocation.setBillId(billId);
        allocation.setAllocatedAmount(amount);

        AllocateSupplierPaymentRequest allocateRequest = new AllocateSupplierPaymentRequest();
        allocateRequest.setAllocations(List.of(allocation));

        supplierPaymentAllocationService.allocatePayment(payment.getPaymentId(), allocateRequest);
    }

    // ==========================================================================================
    // Recurring Journals
    // ==========================================================================================

    private void seedRecurringJournals(LocalDate startDate) {
        createRecurringTemplate(
                "Monthly office rent", "1500.00",
                OPERATING_EXPENSE_ACCOUNT, CHECKING_ACCOUNT, startDate
        );
        createRecurringTemplate(
                "Monthly depreciation - fixed assets", "400.00",
                OPERATING_EXPENSE_ACCOUNT, ACCUMULATED_DEPRECIATION_ACCOUNT, startDate
        );

        // generateDueOccurrences() advances each template by exactly one occurrence per call;
        // loop it enough times to catch the whole historical backlog up to today, exactly as the
        // daily scheduled job would over time.
        for (int i = 0; i < 30; i++) {
            recurringJournalService.generateDueOccurrences();
        }
        log.info("Seeded 2 recurring journal templates and generated their historical occurrences.");
    }

    private void createRecurringTemplate(String description, String amount, String debitAccountCode, String creditAccountCode, LocalDate startDate) {
        RecurringJournalTemplateLineRequest debitLine = new RecurringJournalTemplateLineRequest();
        debitLine.setAccountId(resolveAccountPk(debitAccountCode));
        debitLine.setDescription(description);
        debitLine.setDebitAmount(new BigDecimal(amount));
        debitLine.setCreditAmount(BigDecimal.ZERO);

        RecurringJournalTemplateLineRequest creditLine = new RecurringJournalTemplateLineRequest();
        creditLine.setAccountId(resolveAccountPk(creditAccountCode));
        creditLine.setDescription(description);
        creditLine.setDebitAmount(BigDecimal.ZERO);
        creditLine.setCreditAmount(new BigDecimal(amount));

        CreateRecurringJournalTemplateRequest request = CreateRecurringJournalTemplateRequest.builder()
                .description(description)
                .journalType(JournalType.ADJUSTMENT)
                .frequency(RecurringJournalFrequency.MONTHLY)
                .startDate(startDate)
                .lines(List.of(debitLine, creditLine))
                .build();

        recurringJournalService.create(request);
    }

    // ==========================================================================================
    // Manual adjusting journal
    // ==========================================================================================

    private void seedManualAdjustingJournal() {
        CreateJournalLineRequest debit = CreateJournalLineRequest.builder()
                .accountId(Long.valueOf(OTHER_EXPENSE_ACCOUNT))
                .description("Write-off of stale supplier advance")
                .debitAmount(new BigDecimal("600.00"))
                .creditAmount(BigDecimal.ZERO)
                .build();

        CreateJournalLineRequest credit = CreateJournalLineRequest.builder()
                .accountId(Long.valueOf(SUPPLIER_ADVANCES_ACCOUNT))
                .description("Write-off of stale supplier advance")
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(new BigDecimal("600.00"))
                .build();

        CreateJournalRequest request = CreateJournalRequest.builder()
                .journalDate(LocalDate.now().minusMonths(1).withDayOfMonth(15))
                .reference("ADJ-SUPPLIER-ADV")
                .description("Write-off of stale supplier advance")
                .journalType(JournalType.ADJUSTMENT)
                .lines(List.of(debit, credit))
                .build();

        var created = journalService.create(request);
        journalService.post(created.getId());
        log.info("Posted one manual adjusting journal.");
    }

    // ==========================================================================================
    // Helpers
    // ==========================================================================================

    private void lockEarlyPeriods(Long financialYearId, int periodCount) {
        List<AccountingPeriodResponse> periods = accountingPeriodService.getByFinancialYear(financialYearId);
        periods.stream()
                .filter(p -> p.getPeriodNumber() <= periodCount)
                .forEach(p -> accountingPeriodService.lock(p.getId(), "Books finalized for investor reporting"));
        log.info("Locked the first {} periods of financial year {}", periodCount, financialYearId);
    }

    private CreateFinancialYearRequest newFinancialYearRequest(String name, LocalDate startDate, LocalDate endDate) {
        CreateFinancialYearRequest request = new CreateFinancialYearRequest();
        request.setName(name);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setGenerateMonthlyPeriods(true);
        return request;
    }

    private Long resolveAccountPk(String accountCode) {
        return accountRepository.findByAccountId(accountCode)
                .orElseThrow(() -> new IllegalStateException("Account not found: " + accountCode))
                .getId();
    }
}
