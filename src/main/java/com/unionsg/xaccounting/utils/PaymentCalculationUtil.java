package com.unionsg.xaccounting.utils;

import com.unionsg.xaccounting.entity.customer.Customer;
import com.unionsg.xaccounting.entity.payment.PaymentAllocationEntity;
import com.unionsg.xaccounting.entity.payment.PaymentEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class PaymentCalculationUtil {

    private PaymentCalculationUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static BigDecimal calculateAllocatedAmount(List<PaymentAllocationEntity> allocations) {
        if (allocations == null || allocations.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return allocations.stream()
                .map(PaymentAllocationEntity::getAllocatedAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateUnallocatedAmount(
            BigDecimal amountReceived,
            BigDecimal allocatedAmount
    ) {
        if (amountReceived == null) {
            return BigDecimal.ZERO;
        }
        if (allocatedAmount == null) {
            return amountReceived;
        }
        return amountReceived.subtract(allocatedAmount)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static boolean isFullyAllocated(
            BigDecimal amountReceived,
            BigDecimal allocatedAmount
    ) {
        if (amountReceived == null || allocatedAmount == null) {
            return false;
        }
        return allocatedAmount.compareTo(amountReceived) == 0;
    }

    public static void updateAllocationTotals(PaymentEntity payment) {
        if (payment == null) {
            return;
        }

        BigDecimal allocated = calculateAllocatedAmount(payment.getAllocations());
        BigDecimal unallocated = calculateUnallocatedAmount(
                payment.getAmountReceived(),
                allocated
        );
        boolean fullyAllocated = isFullyAllocated(
                payment.getAmountReceived(),
                allocated
        );

        payment.setAllocatedAmount(allocated);
        payment.setUnallocatedAmount(unallocated);
        payment.setFullyAllocated(fullyAllocated);
    }

    public static BigDecimal calculateRemainingBalance(
            BigDecimal totalDue,
            BigDecimal allocatedSoFar
    ) {
        if (totalDue == null) {
            return BigDecimal.ZERO;
        }
        if (allocatedSoFar == null) {
            return totalDue;
        }
        return totalDue.subtract(allocatedSoFar)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateCustomerCredit(
            Customer customer,
            BigDecimal amount
    ) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
