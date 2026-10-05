package com.unionsg.xaccounting.service.banking;

import com.unionsg.xaccounting.dto.banking.BankTransferAccountingLine;
import com.unionsg.xaccounting.dto.banking.BankTransferActivityResponse;
import com.unionsg.xaccounting.dto.banking.BankTransferFilter;
import com.unionsg.xaccounting.dto.banking.BankTransferJournalResponse;
import com.unionsg.xaccounting.dto.banking.BankTransferResponse;
import com.unionsg.xaccounting.dto.banking.SaveBankTransferRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.journal.JournalLineResponse;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.enums.banking.BankTransferAction;
import com.unionsg.xaccounting.enums.banking.BankTransferStatus;
import com.unionsg.xaccounting.enums.settings.BankAccountStatus;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.settings.BankAccountRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the transfer lifecycle against the real database and the shared journal services
 * (same Postgres requirement as {@code XaccountingApplicationTests}). Each test rolls back.
 */
@SpringBootTest
@Transactional
class BankTransferIntegrationTest {

    @Autowired
    private BankTransferService service;
    @Autowired
    private BankAccountRepository bankAccountRepository;
    @Autowired
    private JournalService journalService;
    @Autowired
    private BaseCurrencyService baseCurrencyService;

    private String base;
    private BankAccount savings;
    private BankAccount moneyMarket;

    @BeforeEach
    void setUp() {
        base = baseCurrencyService.resolve();
        savings = bankAccount("Test Savings", "1020", base, false);
        moneyMarket = bankAccount("Test Money Market", "1030", base, false);
        fund("1020", service.availableBalance(savings).negate().add(new BigDecimal("10000")));
    }

    @Test
    void createSavesADraftWithANumberAndAPreviewOfTheJournal() {
        BankTransferResponse created = service.create(request(savings, moneyMarket, "2500").feeAmount(new BigDecimal("10")).build());

        assertThat(created.getStatus()).isEqualTo(BankTransferStatus.DRAFT);
        assertThat(created.getTransferNumber()).isNotBlank();
        assertThat(created.getJournalId()).isNull();
        assertThat(created.getAccountingLines()).extracting(BankTransferAccountingLine::getAccountCode)
                .containsExactly("1030", "1020", "5100", "1020");
        assertThat(service.getActivity(created.getId())).extracting(BankTransferActivityResponse::getAction)
                .containsExactly(BankTransferAction.CREATED);
    }

    @Test
    void postingGeneratesABalancedJournalLinkedToTheTransfer() {
        BankTransferResponse draft = service.create(request(savings, moneyMarket, "2500").feeAmount(new BigDecimal("10")).build());

        BankTransferResponse posted = service.post(draft.getId());

        assertThat(posted.getStatus()).isEqualTo(BankTransferStatus.POSTED);
        assertThat(posted.getJournalId()).isNotNull();
        BankTransferJournalResponse journal = service.getJournal(draft.getId());
        assertThat(journal.getJournal().getStatus()).isEqualTo(JournalStatus.POSTED);
        assertThat(journal.getJournal().getJournalType()).isEqualTo(JournalType.BANK_TRANSFER);
        assertThat(journal.getJournal().getReference()).isEqualTo(draft.getTransferNumber());
        assertThat(journal.getJournal().getSourceModule()).isEqualTo(BankTransferJournalService.SOURCE_MODULE);
        assertThat(journal.getJournal().getSourceEntityId()).isEqualTo(draft.getId());
        assertThat(journal.getJournal().getTotalDebit()).isEqualByComparingTo("2510");
        assertThat(journal.getJournal().getTotalCredit()).isEqualByComparingTo("2510");
        assertThat(debitOn(journal.getJournal().getLines(), "1030")).isEqualByComparingTo("2500");
        assertThat(debitOn(journal.getJournal().getLines(), "5100")).isEqualByComparingTo("10");
        assertThat(creditOn(journal.getJournal().getLines(), "1020")).isEqualByComparingTo("2510");

        assertThat(service.availableBalance(savings)).isEqualByComparingTo("7490");
    }

    @Test
    void postingTwiceIsRefusedAndCreatesNoSecondJournal() {
        BankTransferResponse draft = service.create(request(savings, moneyMarket, "100").build());
        BankTransferResponse posted = service.post(draft.getId());

        assertThatThrownBy(() -> service.post(draft.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already been posted");
        assertThat(service.get(draft.getId()).getJournalId()).isEqualTo(posted.getJournalId());
        assertThat(service.availableBalance(savings)).isEqualByComparingTo("9900");
    }

    @Test
    void postedTransfersAreReadOnlyAndCannotBeCancelled() {
        BankTransferResponse draft = service.create(request(savings, moneyMarket, "100").build());
        service.post(draft.getId());

        assertThatThrownBy(() -> service.update(draft.getId(), request(savings, moneyMarket, "200").build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("can no longer be edited");
        assertThatThrownBy(() -> service.cancel(draft.getId(), "mistake"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Reverse it instead");
    }

    @Test
    void reversingPostsAReversalJournalAndRestoresBalances() {
        BankTransferResponse draft = service.create(request(savings, moneyMarket, "3000").feeAmount(new BigDecimal("20")).build());
        BankTransferResponse posted = service.post(draft.getId());
        BigDecimal destinationAfterPost = service.availableBalance(moneyMarket);

        BankTransferResponse reversed = service.reverse(draft.getId(), "Sent to the wrong account");

        assertThat(reversed.getStatus()).isEqualTo(BankTransferStatus.REVERSED);
        assertThat(reversed.getReversalJournalId()).isNotNull().isNotEqualTo(posted.getJournalId());
        assertThat(reversed.getReversalReason()).isEqualTo("Sent to the wrong account");

        BankTransferJournalResponse journals = service.getJournal(draft.getId());
        assertThat(journals.getJournal().getId()).isEqualTo(posted.getJournalId());
        assertThat(journals.getJournal().getLines()).hasSize(4);
        assertThat(journals.getReversalJournal().getStatus()).isEqualTo(JournalStatus.POSTED);
        assertThat(creditOn(journals.getReversalJournal().getLines(), "1030")).isEqualByComparingTo("3000");
        assertThat(debitOn(journals.getReversalJournal().getLines(), "1020")).isEqualByComparingTo("3020");

        assertThat(service.availableBalance(savings)).isEqualByComparingTo("10000");
        assertThat(service.availableBalance(moneyMarket)).isEqualByComparingTo(destinationAfterPost.subtract(new BigDecimal("3000")));

        assertThatThrownBy(() -> service.reverse(draft.getId(), "again"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already been reversed");
        assertThat(service.getActivity(draft.getId())).extracting(BankTransferActivityResponse::getAction)
                .containsExactly(BankTransferAction.REVERSED, BankTransferAction.POSTED, BankTransferAction.CREATED);
    }

    @Test
    void insufficientBalanceBlocksPostingUnlessTheAccountAllowsOverdraft() {
        BankTransferResponse draft = service.create(request(savings, moneyMarket, "9995").feeAmount(new BigDecimal("10")).build());

        assertThatThrownBy(() -> service.post(draft.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient balance");
        assertThat(service.get(draft.getId()).getStatus()).isEqualTo(BankTransferStatus.DRAFT);

        savings.setAllowOverdraft(true);
        bankAccountRepository.saveAndFlush(savings);
        assertThat(service.post(draft.getId()).getStatus()).isEqualTo(BankTransferStatus.POSTED);
        assertThat(service.availableBalance(savings)).isEqualByComparingTo("-5");
    }

    @Test
    void invalidAccountsAreRejected() {
        assertThatThrownBy(() -> service.create(request(savings, savings, "10").build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("must be different");

        BankAccount sameGl = bankAccount("Second Savings", "1020", base, false);
        assertThatThrownBy(() -> service.create(request(savings, sameGl, "10").build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("same GL account");

        assertThatThrownBy(() -> service.create(SaveBankTransferRequest.builder()
                .sourceBankAccountId(savings.getId()).destinationBankAccountId(-1L)
                .transferDate(LocalDate.now()).amount(BigDecimal.TEN).build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Destination account not found");

        BankTransferResponse draft = service.create(request(savings, moneyMarket, "10").build());
        moneyMarket.setStatus(BankAccountStatus.INACTIVE);
        bankAccountRepository.saveAndFlush(moneyMarket);
        assertThatThrownBy(() -> service.post(draft.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    void foreignCurrencyTransferPostsTheExchangeLossInBaseCurrency() {
        String foreign = base.equals("USD") ? "EUR" : "USD";
        BankAccount usd = bankAccount("Test Dollar Account", "1000", foreign, false);
        BigDecimal usdBefore = service.availableBalance(usd);

        // 10,000 base buys 75.00 foreign; foreign is carried at 130 → 9,750 base. Loss 250.
        BankTransferResponse draft = service.create(request(savings, usd, "10000")
                .exchangeRate(new BigDecimal("0.0075"))
                .destinationBaseRate(new BigDecimal("130"))
                .build());
        assertThat(draft.getConvertedAmount()).isEqualByComparingTo("75.00");
        assertThat(draft.getDestinationCurrency()).isEqualTo(foreign);
        assertThat(draft.getExchangeGainLoss()).isEqualByComparingTo("-250.00");

        service.post(draft.getId());

        BankTransferJournalResponse journal = service.getJournal(draft.getId());
        assertThat(journal.getJournal().getCurrencyCode()).isEqualTo(base);
        assertThat(journal.getJournal().getTotalDebit()).isEqualByComparingTo(journal.getJournal().getTotalCredit());
        assertThat(debitOn(journal.getJournal().getLines(), "1000")).isEqualByComparingTo("9750");
        assertThat(debitOn(journal.getJournal().getLines(), "5110")).isEqualByComparingTo("250");
        assertThat(creditOn(journal.getJournal().getLines(), "1020")).isEqualByComparingTo("10000");
        assertThat(service.availableBalance(usd).subtract(usdBefore)).isEqualByComparingTo("9750");
    }

    @Test
    void cancellingADraftIsFinalAndStaleEditsAreRefused() {
        BankTransferResponse draft = service.create(request(savings, moneyMarket, "50").build());

        assertThatThrownBy(() -> service.update(draft.getId(),
                request(savings, moneyMarket, "60").version(draft.getVersion() + 5).build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("changed by someone else");

        BankTransferResponse updated = service.update(draft.getId(),
                request(savings, moneyMarket, "60").version(draft.getVersion()).build());
        assertThat(updated.getAmount()).isEqualByComparingTo("60");

        BankTransferResponse cancelled = service.cancel(draft.getId(), "Duplicate request");
        assertThat(cancelled.getStatus()).isEqualTo(BankTransferStatus.CANCELLED);
        assertThatThrownBy(() -> service.post(draft.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cancelled");
    }

    @Test
    void listFiltersByAccountStatusAmountAndNumber() {
        BankTransferResponse small = service.create(request(savings, moneyMarket, "40").reference("REF-" + UUID.randomUUID()).build());
        BankTransferResponse large = service.create(request(savings, moneyMarket, "900").build());
        service.post(large.getId());

        List<Long> bySource = ids(BankTransferFilter.builder().sourceBankAccountId(savings.getId()).build());
        assertThat(bySource).contains(small.getId(), large.getId());

        assertThat(ids(BankTransferFilter.builder().sourceBankAccountId(savings.getId())
                .status(BankTransferStatus.POSTED).build())).containsExactly(large.getId());
        assertThat(ids(BankTransferFilter.builder().sourceBankAccountId(savings.getId())
                .minAmount(new BigDecimal("100")).build())).containsExactly(large.getId());
        assertThat(ids(BankTransferFilter.builder().reference(small.getReference()).build())).containsExactly(small.getId());
        assertThat(ids(BankTransferFilter.builder().transferNumber(large.getTransferNumber()).build())).containsExactly(large.getId());
    }

    private List<Long> ids(BankTransferFilter filter) {
        return service.list(filter, PageRequest.of(0, 50)).getContent().stream().map(t -> t.getId()).toList();
    }

    private SaveBankTransferRequest.SaveBankTransferRequestBuilder request(BankAccount from, BankAccount to, String amount) {
        return SaveBankTransferRequest.builder()
                .sourceBankAccountId(from.getId())
                .destinationBankAccountId(to.getId())
                .transferDate(LocalDate.now())
                .amount(new BigDecimal(amount));
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
