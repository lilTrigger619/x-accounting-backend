package com.unionsg.xaccounting.service.bankrec.engine;

import com.unionsg.xaccounting.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchAllocatorTest {

    private static AllocationItem item(long id, String remaining) {
        return new AllocationItem(id, new BigDecimal(remaining), null);
    }

    @Test
    void oneToOneFullMatch() {
        MatchAllocator.Allocation a = MatchAllocator.allocate(List.of(item(1, "100.00")), List.of(item(10, "100.00")));

        assertThat(a.amount()).isEqualByComparingTo("100.00");
        assertThat(a.statement().get(1L)).isEqualByComparingTo("100.00");
        assertThat(a.book().get(10L)).isEqualByComparingTo("100.00");
    }

    @Test
    void partialMatchLeavesTheRemainderOnTheLargerSide() {
        MatchAllocator.Allocation a = MatchAllocator.allocate(List.of(item(1, "1000.00")), List.of(item(10, "600.00")));

        assertThat(a.amount()).isEqualByComparingTo("600.00");
        assertThat(a.statement().get(1L)).isEqualByComparingTo("600.00");
    }

    @Test
    void oneStatementLineToSeveralBookLines() {
        MatchAllocator.Allocation a = MatchAllocator.allocate(
                List.of(item(1, "-900.00")), List.of(item(10, "-600.00"), item(11, "-300.00")));

        assertThat(a.amount()).isEqualByComparingTo("900.00");
        assertThat(a.book().get(10L)).isEqualByComparingTo("600.00");
        assertThat(a.book().get(11L)).isEqualByComparingTo("300.00");
    }

    @Test
    void severalStatementLinesToOneBookLineCanBePartialOnTheLastItem() {
        MatchAllocator.Allocation a = MatchAllocator.allocate(
                List.of(item(1, "400.00"), item(2, "700.00")), List.of(item(10, "1000.00")));

        assertThat(a.amount()).isEqualByComparingTo("1000.00");
        assertThat(a.statement().get(1L)).isEqualByComparingTo("400.00");
        assertThat(a.statement().get(2L)).isEqualByComparingTo("600.00");
    }

    @Test
    void manyToManyIsRefused() {
        assertThatThrownBy(() -> MatchAllocator.allocate(
                List.of(item(1, "1"), item(2, "1")), List.of(item(10, "1"), item(11, "1"))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("not many to many");
    }

    @Test
    void depositsCannotMatchPayments() {
        assertThatThrownBy(() -> MatchAllocator.allocate(List.of(item(1, "50")), List.of(item(10, "-50"))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("deposit");
    }

    @Test
    void fullyMatchedItemsCannotBeSelected() {
        assertThatThrownBy(() -> MatchAllocator.allocate(List.of(item(1, "0")), List.of(item(10, "5"))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("already fully matched");
    }

    @Test
    void extraSelectionsThatAddNothingAreRefused() {
        assertThatThrownBy(() -> MatchAllocator.allocate(
                List.of(item(1, "100")), List.of(item(10, "100"), item(11, "20"))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("extra selections");
    }

    @Test
    void explicitAllocationsMustBalanceAndFit() {
        MatchAllocator.Allocation a = MatchAllocator.allocate(
                List.of(new AllocationItem(1L, new BigDecimal("500"), new BigDecimal("200"))),
                List.of(new AllocationItem(10L, new BigDecimal("300"), new BigDecimal("200"))));
        assertThat(a.amount()).isEqualByComparingTo("200");

        assertThatThrownBy(() -> MatchAllocator.allocate(
                List.of(new AllocationItem(1L, new BigDecimal("500"), new BigDecimal("200"))),
                List.of(new AllocationItem(10L, new BigDecimal("300"), new BigDecimal("150")))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("must be equal");

        assertThatThrownBy(() -> MatchAllocator.allocate(
                List.of(new AllocationItem(1L, new BigDecimal("100"), new BigDecimal("200"))),
                List.of(new AllocationItem(10L, new BigDecimal("300"), new BigDecimal("200")))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("larger than");
    }
}
