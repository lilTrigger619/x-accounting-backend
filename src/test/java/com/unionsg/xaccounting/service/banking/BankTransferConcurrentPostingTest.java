package com.unionsg.xaccounting.service.banking;

import com.unionsg.xaccounting.dto.banking.BankTransferResponse;
import com.unionsg.xaccounting.dto.banking.SaveBankTransferRequest;
import com.unionsg.xaccounting.entity.banking.BankTransfer;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.enums.banking.BankTransferStatus;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.banking.BankTransferActivityRepository;
import com.unionsg.xaccounting.repository.banking.BankTransferRepository;
import com.unionsg.xaccounting.repository.journal.JournalEntryRepository;
import com.unionsg.xaccounting.repository.settings.BankAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fires several post requests for the same transfer at once, each in its own committed
 * transaction, and checks exactly one journal comes out. Cleans up what it committed.
 */
@SpringBootTest
class BankTransferConcurrentPostingTest {

    @Autowired
    private BankTransferService service;
    @Autowired
    private BankTransferRepository transferRepository;
    @Autowired
    private BankTransferActivityRepository activityRepository;
    @Autowired
    private BankAccountRepository bankAccountRepository;
    @Autowired
    private JournalEntryRepository journalEntryRepository;
    @Autowired
    private BaseCurrencyService baseCurrencyService;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private final List<Long> bankAccountIds = new ArrayList<>();
    private Long transferId;

    @Test
    void concurrentPostsProduceExactlyOneJournal() throws Exception {
        String base = baseCurrencyService.resolve();
        BankAccount source = account("Concurrency Source", "1020", base);
        BankAccount destination = account("Concurrency Destination", "1030", base);

        BankTransferResponse draft = service.create(SaveBankTransferRequest.builder()
                .sourceBankAccountId(source.getId())
                .destinationBankAccountId(destination.getId())
                .transferDate(LocalDate.now())
                .amount(new BigDecimal("12.34"))
                .build());
        transferId = draft.getId();

        int attempts = 4;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        Callable<Boolean> post = () -> {
            start.await();
            try {
                service.post(draft.getId());
                return true;
            } catch (BusinessException e) {
                return false;
            }
        };
        for (int i = 0; i < attempts; i++) {
            results.add(pool.submit(post));
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        long successes = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) {
                successes++;
            }
        }
        assertThat(successes).isEqualTo(1);

        BankTransferResponse after = service.get(draft.getId());
        assertThat(after.getStatus()).isEqualTo(BankTransferStatus.POSTED);
        long journals = journalEntryRepository.findAll().stream()
                .filter(j -> draft.getTransferNumber().equals(j.getReference()))
                .count();
        assertThat(journals).isEqualTo(1);
    }

    @AfterEach
    void cleanUp() {
        transactionTemplate.executeWithoutResult(status -> {
            if (transferId != null) {
                BankTransfer transfer = transferRepository.findById(transferId).orElse(null);
                if (transfer != null) {
                    activityRepository.deleteAll(activityRepository.findByBankTransferIdOrderByCreatedAtDescIdDesc(transferId));
                    Long journalId = transfer.getJournal() != null ? transfer.getJournal().getId() : null;
                    transferRepository.delete(transfer);
                    transferRepository.flush();
                    if (journalId != null) {
                        journalEntryRepository.deleteById(journalId);
                    }
                }
            }
            bankAccountRepository.deleteAllById(bankAccountIds);
        });
    }

    private BankAccount account(String name, String glCode, String currency) {
        BankAccount account = new BankAccount();
        account.setAccountName(name);
        account.setGlAccountCode(glCode);
        account.setCurrency(currency);
        account.setAllowOverdraft(true);
        BankAccount saved = bankAccountRepository.saveAndFlush(account);
        bankAccountIds.add(saved.getId());
        return saved;
    }
}
