package com.unionsg.xaccounting.service.expense;

import com.unionsg.xaccounting.dto.expense.ExpenseAccountingLine;
import com.unionsg.xaccounting.dto.expense.ExpenseActivityResponse;
import com.unionsg.xaccounting.dto.expense.ExpenseFilter;
import com.unionsg.xaccounting.dto.expense.ExpenseJournalResponse;
import com.unionsg.xaccounting.dto.expense.ExpenseResponse;
import com.unionsg.xaccounting.dto.expense.SaveExpenseLineRequest;
import com.unionsg.xaccounting.dto.expense.SaveExpenseRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.journal.JournalLineResponse;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.enums.PaymentMethod;
import com.unionsg.xaccounting.enums.expense.ExpenseAction;
import com.unionsg.xaccounting.enums.expense.ExpenseStatus;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.settings.BankAccountRepository;
import com.unionsg.xaccounting.service.banking.BaseCurrencyService;
import com.unionsg.xaccounting.service.journal.JournalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the expense lifecycle against the real database and the shared journal services
 * (same Postgres requirement as {@code XaccountingApplicationTests}). Each test rolls back.
 */
@SpringBootTest
@Transactional
class ExpenseIntegrationTest {

    @Autowired
    private ExpenseService service;
    @Autowired
    private BankAccountRepository bankAccountRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private JournalService journalService;
    @Autowired
    private BaseCurrencyService baseCurrencyService;

    private String base;
    private BankAccount savings;
    private Long operating;
    private Long freight;

    @BeforeEach
    void setUp() {
        base = baseCurrencyService.resolve();
        savings = bankAccount("Test Expense Savings", "1020", base, false);
        fund("1020", service.availableBalance(savings).negate().add(new BigDecimal("1000")));
        operating = accountId("5000");
        freight = accountId("5030");
    }

    @Test
    void createSavesADraftWithANumberLinesAndAPreviewOfTheJournal() {
        ExpenseResponse created = service.create(request(savings, line(operating, "Printer paper", "120.50"),
                line(freight, "Courier", "30")).reference("RCPT-1").build());

        assertThat(created.getStatus()).isEqualTo(ExpenseStatus.DRAFT);
        assertThat(created.getExpenseNumber()).isNotBlank();
        assertThat(created.getCurrency()).isEqualTo(base);
        assertThat(created.getTotalAmount()).isEqualByComparingTo("150.50");
        assertThat(created.getLines()).hasSize(2);
        assertThat(created.getLines().get(0).getExpenseDate()).isEqualTo(LocalDate.now());
        assertThat(created.getLines().get(0).getCategory()).isEqualTo("OFF");
        assertThat(created.getJournalId()).isNull();
        assertThat(created.getAccountingLines()).extracting(ExpenseAccountingLine::getAccountCode)
                .containsExactly("5000", "5030", "1020");
        assertThat(service.getActivity(created.getId())).extracting(ExpenseActivityResponse::getAction)
                .containsExactly(ExpenseAction.CREATED);
    }

    @Test
    void postingGeneratesABalancedJournalLinkedToTheExpense() {
        ExpenseResponse draft = service.create(request(savings, line(operating, "Rent", "400"),
                line(freight, "Delivery", "100")).build());

        ExpenseResponse posted = service.post(draft.getId());

        assertThat(posted.getStatus()).isEqualTo(ExpenseStatus.POSTED);
        ExpenseJournalResponse journal = service.getJournal(draft.getId());
        assertThat(journal.getJournal().getStatus()).isEqualTo(JournalStatus.POSTED);
        assertThat(journal.getJournal().getJournalType()).isEqualTo(JournalType.EXPENSE);
        assertThat(journal.getJournal().getReference()).isEqualTo(draft.getExpenseNumber());
        assertThat(journal.getJournal().getSourceModule()).isEqualTo(ExpenseJournalService.SOURCE_MODULE);
        assertThat(journal.getJournal().getSourceEntityId()).isEqualTo(draft.getId());
        assertThat(journal.getJournal().getTotalDebit()).isEqualByComparingTo("500");
        assertThat(journal.getJournal().getTotalCredit()).isEqualByComparingTo("500");
        assertThat(debitOn(journal.getJournal().getLines(), "5000")).isEqualByComparingTo("400");
        assertThat(debitOn(journal.getJournal().getLines(), "5030")).isEqualByComparingTo("100");
        assertThat(creditOn(journal.getJournal().getLines(), "1020")).isEqualByComparingTo("500");

        assertThat(service.availableBalance(savings)).isEqualByComparingTo("500");
    }

    @Test
    void postingTwiceIsRefusedAndPostedExpensesAreReadOnly() {
        ExpenseResponse draft = service.create(request(savings, line(operating, "Fuel", "50")).build());
        ExpenseResponse posted = service.post(draft.getId());

        assertThatThrownBy(() -> service.post(draft.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already been posted");
        assertThatThrownBy(() -> service.update(draft.getId(), request(savings, line(operating, "Fuel", "60")).build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("can no longer be edited");
        assertThatThrownBy(() -> service.delete(draft.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Reverse it instead");
        assertThat(service.get(draft.getId()).getJournalId()).isEqualTo(posted.getJournalId());
        assertThat(service.availableBalance(savings)).isEqualByComparingTo("950");
    }

    @Test
    void reversingPostsAReversalJournalAndRestoresTheBalance() {
        ExpenseResponse draft = service.create(request(savings, line(operating, "Training", "300")).build());
        ExpenseResponse posted = service.post(draft.getId());

        ExpenseResponse reversed = service.reverse(draft.getId(), "Paid twice");

        assertThat(reversed.getStatus()).isEqualTo(ExpenseStatus.REVERSED);
        assertThat(reversed.getReversalJournalId()).isNotNull().isNotEqualTo(posted.getJournalId());
        assertThat(reversed.getReversalReason()).isEqualTo("Paid twice");
        assertThat(service.getJournal(draft.getId()).getReversalJournal()).isNotNull();
        assertThat(service.availableBalance(savings)).isEqualByComparingTo("1000");
        assertThatThrownBy(() -> service.reverse(draft.getId(), "again"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already been reversed");
        assertThat(service.getActivity(draft.getId())).extracting(ExpenseActivityResponse::getAction)
                .containsExactly(ExpenseAction.REVERSED, ExpenseAction.POSTED, ExpenseAction.CREATED);
    }

    @Test
    void updatingADraftReplacesItsLines() {
        ExpenseResponse draft = service.create(request(savings, line(operating, "Paper", "10"), line(freight, "Post", "5")).build());

        ExpenseResponse updated = service.update(draft.getId(),
                request(savings, line(freight, "Courier", "25")).version(draft.getVersion()).build());

        assertThat(updated.getLines()).hasSize(1);
        assertThat(updated.getLines().get(0).getAccountCode()).isEqualTo("5030");
        assertThat(updated.getTotalAmount()).isEqualByComparingTo("25");
        assertThatThrownBy(() -> service.update(draft.getId(),
                request(savings, line(freight, "Courier", "30")).version(draft.getVersion()).build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("changed by someone else");
    }

    @Test
    void deletingADraftHidesIt() {
        ExpenseResponse draft = service.create(request(savings, line(operating, "Snacks", "12")).build());

        service.delete(draft.getId());

        assertThatThrownBy(() -> service.get(draft.getId())).isInstanceOf(ResourceNotFoundException.class);
        assertThat(ids(ExpenseFilter.builder().build())).doesNotContain(draft.getId());
    }

    @Test
    void insufficientBalanceBlocksPostingUnlessTheAccountAllowsOverdraft() {
        ExpenseResponse draft = service.create(request(savings, line(operating, "Laptop", "1500")).build());
        assertThatThrownBy(() -> service.post(draft.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient balance");

        savings.setAllowOverdraft(true);
        bankAccountRepository.saveAndFlush(savings);
        assertThat(service.post(draft.getId()).getStatus()).isEqualTo(ExpenseStatus.POSTED);
    }

    @Test
    void linesNeedAConfiguredCategoryAndAnExpenseAccount() {
        assertThatThrownBy(() -> service.create(request(savings, line(accountId("1020"), "Wrong", "10")).build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not an expense account");
        assertThatThrownBy(() -> service.create(request(savings, line(-1L, "Missing", "10")).build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Expense account not found");
        SaveExpenseLineRequest unknownCategory = line(operating, "Lunch", "10");
        unknownCategory.setCategory("NOT-A-CATEGORY");
        assertThatThrownBy(() -> service.create(request(savings, unknownCategory).build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not a valid line 1 category");
    }

    @Test
    void foreignCurrencyAccountsNeedARateAndPostInBaseCurrency() {
        String foreign = base.equals("USD") ? "EUR" : "USD";
        BankAccount dollars = bankAccount("Test Expense Dollars", "1030", foreign, true);

        assertThatThrownBy(() -> service.create(request(dollars, line(operating, "Software", "10")).build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exchange rate");

        ExpenseResponse draft = service.create(request(dollars, line(operating, "Software", "10.00"),
                line(freight, "Shipping", "0.33")).exchangeRate(new BigDecimal("129.5")).build());
        assertThat(draft.getCurrency()).isEqualTo(foreign);
        assertThat(draft.getTotalAmount()).isEqualByComparingTo("10.33");
        // 1295.00 + 42.735 -> 42.74; the credit is the sum of the rounded debits.
        assertThat(draft.getBaseTotalAmount()).isEqualByComparingTo("1337.74");

        service.post(draft.getId());
        ExpenseJournalResponse journal = service.getJournal(draft.getId());
        assertThat(journal.getJournal().getCurrencyCode()).isEqualTo(base);
        assertThat(creditOn(journal.getJournal().getLines(), "1030")).isEqualByComparingTo("1337.74");
    }

    @Test
    void listFiltersByStatusAndCategory() {
        ExpenseResponse a = service.create(request(savings, line(operating, "A", "10")).build());
        ExpenseResponse b = service.create(request(savings, line(freight, "B", "20")).build());
        service.post(b.getId());

        assertThat(ids(ExpenseFilter.builder().status(ExpenseStatus.POSTED).build())).contains(b.getId()).doesNotContain(a.getId());
        assertThat(ids(ExpenseFilter.builder().accountId(operating).build())).contains(a.getId()).doesNotContain(b.getId());
        assertThat(ids(ExpenseFilter.builder().search(a.getExpenseNumber()).build())).containsExactly(a.getId());
        assertThat(ids(ExpenseFilter.builder().category("OFF").build())).contains(a.getId(), b.getId());
        assertThat(ids(ExpenseFilter.builder().category("TRV").build())).doesNotContain(a.getId(), b.getId());
    }

    private List<Long> ids(ExpenseFilter filter) {
        return service.list(filter, PageRequest.of(0, 200)).getContent().stream().map(e -> e.getId()).toList();
    }

    private SaveExpenseRequest.SaveExpenseRequestBuilder request(BankAccount from, SaveExpenseLineRequest... lines) {
        return SaveExpenseRequest.builder()
                .paymentAccountId(from.getId())
                .paymentDate(LocalDate.now())
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .lines(List.of(lines));
    }

    private static SaveExpenseLineRequest line(Long accountId, String description, String amount) {
        return SaveExpenseLineRequest.builder().category("OFF").accountId(accountId).description(description)
                .amount(new BigDecimal(amount)).build();
    }

    private Long accountId(String code) {
        return accountRepository.findByAccountId(code).orElseThrow().getId();
    }

    private BankAccount bankAccount(String name, String glCode, String currency, boolean allowOverdraft) {
        BankAccount account = new BankAccount();
        account.setAccountName(name);
        account.setBankName("Test Bank");
        account.setGlAccountCode(glCode);
        account.setCurrency(currency);
        account.setAllowOverdraft(allowOverdraft);
        return bankAccountRepository.saveAndFlush(account);
    }

    /** Moves {@code amount} into a GL account against Owners Equity, so balances are known. */
    private void fund(String glCode, BigDecimal amount) {
        if (amount.signum() == 0) {
            return;
        }
        boolean debit = amount.signum() > 0;
        BigDecimal value = amount.abs();
        var created = journalService.create(CreateJournalRequest.builder()
                .journalDate(LocalDate.now())
                .description("Test funding")
                .journalType(JournalType.ADJUSTMENT)
                .currencyCode(base)
                .lines(List.of(
                        CreateJournalLineRequest.builder().accountId(Long.valueOf(glCode))
                                .debitAmount(debit ? value : BigDecimal.ZERO).creditAmount(debit ? BigDecimal.ZERO : value).build(),
                        CreateJournalLineRequest.builder().accountId(3000L)
                                .debitAmount(debit ? BigDecimal.ZERO : value).creditAmount(debit ? value : BigDecimal.ZERO).build()))
                .build());
        journalService.post(created.getId());
    }

    private static BigDecimal debitOn(List<JournalLineResponse> lines, String code) {
        return lines.stream().filter(l -> code.equals(l.getAccountCode()))
                .map(JournalLineResponse::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal creditOn(List<JournalLineResponse> lines, String code) {
        return lines.stream().filter(l -> code.equals(l.getAccountCode()))
                .map(JournalLineResponse::getCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
