package com.unionsg.xaccounting.service.branch;

import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.entity.branch.Branch;
import com.unionsg.xaccounting.entity.branch.BranchAccountingSettings;
import com.unionsg.xaccounting.entity.branch.CostCenter;
import com.unionsg.xaccounting.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InterBranchBalancerTest {

    @Mock private BranchAccountingSettingsService settingsService;
    @Mock private BranchService branchService;

    @InjectMocks
    private InterBranchBalancer balancer;

    private BranchAccountingSettings settings;
    private Branch accra;
    private Branch kumasi;
    private Branch tamale;
    private AccountEntity interBranch;

    @BeforeEach
    void setUp() {
        accra = branch(1L, "Accra");
        kumasi = branch(2L, "Kumasi");
        tamale = branch(3L, "Tamale");
        interBranch = new AccountEntity();
        interBranch.setId(99L);
        interBranch.setAccountId("1790");
        settings = new BranchAccountingSettings();
        settings.setSelfBalancingBranches(true);
        settings.setInterBranchAccount(interBranch);
        when(settingsService.settings()).thenReturn(settings);
        when(branchService.defaultCostCenterOf(any())).thenAnswer(inv -> costCenter(inv.getArgument(0)));
    }

    @Test
    void addsDueToAndDueFromWhenAccraPaysKumasisBill() {
        JournalEntry journal = journal(accra);
        journal.addLine(line(kumasi, "10000", "0"));   // Kumasi expense
        journal.addLine(line(accra, "0", "10000"));    // Accra bank

        balancer.balance(journal);

        List<JournalLine> ib = interBranchLines(journal);
        assertThat(ib).hasSize(2);
        JournalLine kumasiSide = ib.stream().filter(l -> l.getBranch() == kumasi).findFirst().orElseThrow();
        JournalLine accraSide = ib.stream().filter(l -> l.getBranch() == accra).findFirst().orElseThrow();
        assertThat(kumasiSide.getCreditAmount()).isEqualByComparingTo("10000");
        assertThat(kumasiSide.getCounterpartyBranch()).isSameAs(accra);
        assertThat(kumasiSide.getDescription()).isEqualTo("Due to Accra");
        assertThat(accraSide.getDebitAmount()).isEqualByComparingTo("10000");
        assertThat(accraSide.getCounterpartyBranch()).isSameAs(kumasi);
        assertThat(accraSide.getDescription()).isEqualTo("Due from Kumasi");
        assertThat(ib).allMatch(l -> l.getAccount() == interBranch);

        assertBalancedPerBranch(journal);
        assertThatCode(() -> balancer.assertBalancedPerBranch(journal)).doesNotThrowAnyException();
    }

    @Test
    void balancesThreeBranchesAgainstTheJournalsOwnBranch() {
        JournalEntry journal = journal(accra);
        journal.addLine(line(kumasi, "300", "0"));
        journal.addLine(line(tamale, "0", "100"));
        journal.addLine(line(accra, "0", "200"));

        balancer.balance(journal);

        assertThat(interBranchLines(journal)).hasSize(4);
        assertThat(interBranchLines(journal)).allMatch(l -> l.getBranch() == accra || l.getCounterpartyBranch() == accra);
        assertBalancedPerBranch(journal);
    }

    @Test
    void leavesSingleBranchJournalsAlone() {
        JournalEntry journal = journal(accra);
        journal.addLine(line(accra, "50", "0"));
        journal.addLine(line(accra, "0", "50"));

        balancer.balance(journal);

        assertThat(journal.getLines()).hasSize(2);
    }

    @Test
    void leavesBranchesThatAlreadyBalanceAlone() {
        JournalEntry journal = journal(accra);
        journal.addLine(line(accra, "50", "0"));
        journal.addLine(line(accra, "0", "50"));
        journal.addLine(line(kumasi, "20", "0"));
        journal.addLine(line(kumasi, "0", "20"));

        balancer.balance(journal);

        assertThat(interBranchLines(journal)).isEmpty();
    }

    @Test
    void reRunReplacesEarlierInterBranchLines() {
        JournalEntry journal = journal(accra);
        journal.addLine(line(kumasi, "10", "0"));
        journal.addLine(line(accra, "0", "10"));
        balancer.balance(journal);
        balancer.balance(journal);

        assertThat(interBranchLines(journal)).hasSize(2);
    }

    @Test
    void refusesCrossBranchJournalWithoutAnInterBranchAccount() {
        settings.setInterBranchAccount(null);
        JournalEntry journal = journal(accra);
        journal.addLine(line(kumasi, "10", "0"));
        journal.addLine(line(accra, "0", "10"));

        assertThatThrownBy(() -> balancer.balance(journal))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inter-branch account");
    }

    @Test
    void doesNothingWhenSelfBalancingIsOff() {
        settings.setSelfBalancingBranches(false);
        JournalEntry journal = journal(accra);
        journal.addLine(line(kumasi, "10", "0"));
        journal.addLine(line(accra, "0", "10"));

        balancer.balance(journal);

        assertThat(interBranchLines(journal)).isEmpty();
        assertThatCode(() -> balancer.assertBalancedPerBranch(journal)).doesNotThrowAnyException();
    }

    @Test
    void postingCheckRejectsABranchThatDoesNotBalance() {
        JournalEntry journal = journal(accra);
        journal.addLine(line(kumasi, "10", "0"));
        journal.addLine(line(accra, "0", "10"));

        assertThatThrownBy(() -> balancer.assertBalancedPerBranch(journal))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Kumasi");
    }

    private static void assertBalancedPerBranch(JournalEntry journal) {
        Map<Branch, BigDecimal> net = journal.getLines().stream().collect(Collectors.groupingBy(JournalLine::getBranch,
                Collectors.reducing(BigDecimal.ZERO, l -> l.getDebitAmount().subtract(l.getCreditAmount()), BigDecimal::add)));
        assertThat(net.values()).allMatch(v -> v.signum() == 0);
    }

    private static List<JournalLine> interBranchLines(JournalEntry journal) {
        return journal.getLines().stream().filter(l -> Boolean.TRUE.equals(l.getInterBranch())).toList();
    }

    private static JournalEntry journal(Branch branch) {
        JournalEntry journal = new JournalEntry();
        journal.setBranch(branch);
        journal.setCurrencyCode("GHS");
        return journal;
    }

    private static JournalLine line(Branch branch, String debit, String credit) {
        JournalLine line = new JournalLine();
        line.setBranch(branch);
        line.setCostCenter(costCenter(branch));
        line.setDebitAmount(new BigDecimal(debit));
        line.setCreditAmount(new BigDecimal(credit));
        line.setLineNumber(1);
        return line;
    }

    private static Branch branch(Long id, String name) {
        Branch b = new Branch();
        b.setId(id);
        b.setName(name);
        b.setCode(name.substring(0, 3).toUpperCase());
        b.setActive(true);
        return b;
    }

    private static CostCenter costCenter(Branch branch) {
        CostCenter cc = new CostCenter();
        cc.setId(branch.getId() * 10);
        cc.setBranch(branch);
        cc.setCode(branch.getCode() + "-GEN");
        cc.setBranchDefault(true);
        return cc;
    }
}
