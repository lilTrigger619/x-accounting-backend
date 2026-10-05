package com.unionsg.xaccounting.service.bankrec;

import com.unionsg.xaccounting.dto.bankrec.ImportRowResponse;
import com.unionsg.xaccounting.dto.bankrec.StatementImportOptions;
import com.unionsg.xaccounting.dto.bankrec.StatementImportResponse;
import com.unionsg.xaccounting.entity.bankrec.BankStatementImport;
import com.unionsg.xaccounting.entity.bankrec.BankStatementTransaction;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.bankrec.BankReconciliationAdjustmentRepository;
import com.unionsg.xaccounting.repository.bankrec.BankReconciliationMatchItemRepository;
import com.unionsg.xaccounting.repository.bankrec.BankReconciliationRepository;
import com.unionsg.xaccounting.repository.bankrec.BankStatementImportRepository;
import com.unionsg.xaccounting.repository.bankrec.BankStatementTransactionRepository;
import com.unionsg.xaccounting.repository.settings.BankAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BankStatementImportServiceTest {

    private static final String CSV = """
            Date,Description,Reference,Debit,Credit,Balance
            01/03/2026,Opening deposit,DEP-1,,500.00,500.00
            02/03/2026,SMS charge,,5.00,,495.00
            02/03/2026,SMS charge,,5.00,,490.00
            """;

    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private BankReconciliationRepository reconciliationRepository;
    @Mock private BankStatementImportRepository importRepository;
    @Mock private BankStatementTransactionRepository transactionRepository;
    @Mock private BankReconciliationMatchItemRepository matchItemRepository;
    @Mock private BankReconciliationAdjustmentRepository adjustmentRepository;
    @Mock private BankStatementImportProfileService profileService;
    @Mock private BankRecAuditService auditService;

    @InjectMocks
    private BankStatementImportService service;

    private final List<String> storedKeys = new ArrayList<>();

    @BeforeEach
    void setUp() {
        BankAccount bank = new BankAccount();
        bank.setId(1L);
        bank.setAccountName("Operating account");
        bank.setGlAccountCode("1010");
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(bank));
        when(importRepository.save(any(BankStatementImport.class))).thenAnswer(inv -> {
            BankStatementImport i = inv.getArgument(0);
            i.setId(3L);
            return i;
        });
        when(transactionRepository.findExistingDedupeKeys(eq(1L), anyCollection()))
                .thenAnswer(inv -> new ArrayList<>(inv.<java.util.Collection<String>>getArgument(1)).stream()
                        .filter(storedKeys::contains).toList());
        when(transactionRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<BankStatementTransaction> saved = inv.getArgument(0);
            saved.forEach(t -> storedKeys.add(t.getDedupeKey()));
            return saved;
        });
    }

    private static StatementImportOptions options() {
        StatementImportOptions o = new StatementImportOptions();
        o.setBankAccountId(1L);
        o.setDateFormat("dd/MM/yyyy");
        o.setTransactionDateColumn("Date");
        o.setDescriptionColumn("Description");
        o.setReferenceColumn("Reference");
        o.setDebitColumn("Debit");
        o.setCreditColumn("Credit");
        o.setBalanceColumn("Balance");
        return o;
    }

    @Test
    void importsEveryNewLineIncludingIdenticalOnesWithDifferentBalances() {
        StatementImportResponse result = service.importStatement(CSV, "march.csv", options());

        assertThat(result.getImportedRows()).isEqualTo(3);
        assertThat(result.getDuplicateRows()).isZero();
        assertThat(result.getFirstTransactionDate()).hasToString("2026-03-01");
        assertThat(storedKeys).doesNotHaveDuplicates().hasSize(3);
    }

    @Test
    void reimportingTheSameFileImportsNothing() {
        service.importStatement(CSV, "march.csv", options());

        StatementImportResponse again = service.importStatement(CSV, "march-again.csv", options());

        assertThat(again.getImportedRows()).isZero();
        assertThat(again.getDuplicateRows()).isEqualTo(3);
        assertThat(again.getRows()).extracting(ImportRowResponse::getOutcome).containsOnly("DUPLICATE");
        assertThat(storedKeys).hasSize(3);
    }

    @Test
    void anOverlappingFileOnlyAddsTheNewLines() {
        service.importStatement(CSV, "march.csv", options());
        String overlap = """
                Date,Description,Reference,Debit,Credit,Balance
                02/03/2026,SMS charge,,5.00,,490.00
                03/03/2026,Customer transfer,RCP-9,,120.00,610.00
                """;

        StatementImportResponse result = service.importStatement(overlap, "overlap.csv", options());

        assertThat(result.getImportedRows()).isEqualTo(1);
        assertThat(result.getDuplicateRows()).isEqualTo(1);
    }

    @Test
    void dryRunSavesNothing() {
        StatementImportOptions o = options();
        o.setDryRun(true);

        StatementImportResponse preview = service.importStatement(CSV, "march.csv", o);

        assertThat(preview.getDryRun()).isTrue();
        assertThat(preview.getImportedRows()).isEqualTo(3);
        verify(importRepository, never()).save(any());
        verify(transactionRepository, never()).saveAll(anyList());
    }

    @Test
    void repeatedBankTransactionIdsInOneFileAreDuplicates() {
        String csv = "Date,Amount,ID\n01/03/2026,10,TX1\n01/03/2026,10,TX1\n";
        StatementImportOptions o = new StatementImportOptions();
        o.setBankAccountId(1L);
        o.setDateFormat("dd/MM/yyyy");
        o.setTransactionDateColumn("Date");
        o.setAmountColumn("Amount");
        o.setExternalIdColumn("ID");

        StatementImportResponse result = service.importStatement(csv, "ids.csv", o);

        assertThat(result.getImportedRows()).isEqualTo(1);
        assertThat(result.getRows().get(1).getMessage()).isEqualTo("Repeated in this file");
    }

    @Test
    void importedLinesAreKeptApartFromTheLedgerAndStartUnmatched() {
        service.importStatement(CSV, "march.csv", options());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BankStatementTransaction>> captor = ArgumentCaptor.forClass(List.class);
        verify(transactionRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).allSatisfy(t -> {
            assertThat(t.getMatchedAmount()).isZero();
            assertThat(t.getStatementImport().getId()).isEqualTo(3L);
        });
    }

    @Test
    void anImportWithMatchHistoryCannotBeDeleted() {
        BankStatementImport batch = new BankStatementImport();
        batch.setId(3L);
        when(importRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(batch));
        when(matchItemRepository.findAllForImport(3L)).thenReturn(List.of(new com.unionsg.xaccounting.entity.bankrec.BankReconciliationMatchItem()));

        assertThatThrownBy(() -> service.deleteImport(3L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("matched or adjusted");
    }
}
