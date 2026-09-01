package com.unionsg.xaccounting.service.bill;

import com.unionsg.xaccounting.entity.bill.Bill;
import com.unionsg.xaccounting.entity.bill.BillItem;
import com.unionsg.xaccounting.enums.DiscountType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class BillCalculationService {

    public void calculateBill(Bill bill) {

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (BillItem item : bill.getItems()) {

            BigDecimal quantity = item.getQuantity();
            BigDecimal unitPrice = item.getUnitPrice();
            BigDecimal taxRate = item.getTaxRate() != null ? item.getTaxRate() : BigDecimal.ZERO;

            BigDecimal lineSubtotal =
                    quantity.multiply(unitPrice);

            BigDecimal lineTax =
                    lineSubtotal.multiply(
                            taxRate.divide(
                                    BigDecimal.valueOf(100),
                                    4,
                                    RoundingMode.HALF_UP
                            )
                    );

            BigDecimal lineTotal =
                    lineSubtotal.add(lineTax);

            item.setLineSubtotal(scale(lineSubtotal));
            item.setLineTax(scale(lineTax));
            item.setLineTotal(scale(lineTotal));

            subtotal = subtotal.add(lineSubtotal);
            totalTax = totalTax.add(lineTax);
        }

        bill.setSubtotal(scale(subtotal));
        bill.setTotalTax(scale(totalTax));

        calculateDiscount(bill);

        BigDecimal total =
                subtotal
                        .add(totalTax)
                        .subtract(bill.getDiscountAmount());

        bill.setTotalAmount(scale(total));

        BigDecimal alreadyPaid = bill.getAmountPaid() != null ? bill.getAmountPaid() : BigDecimal.ZERO;
        BigDecimal outstanding = scale(total).subtract(alreadyPaid);

        bill.setTotalDue(scale(total));
        bill.setBalance(outstanding.max(BigDecimal.ZERO));
    }


    private void calculateDiscount(Bill bill) {

        BigDecimal discountAmount = BigDecimal.ZERO;

        if (bill.getDiscountType() == DiscountType.PERCENTAGE && bill.getDiscountValue() != null) {

            discountAmount =
                    bill.getSubtotal()
                            .multiply(
                                    bill.getDiscountValue()
                                            .divide(
                                                    BigDecimal.valueOf(100),
                                                    4,
                                                    RoundingMode.HALF_UP
                                            )
                            );
        }

        if (bill.getDiscountType() == DiscountType.FIXED && bill.getDiscountValue() != null) {
            discountAmount = bill.getDiscountValue();
        }

        bill.setDiscountAmount(scale(discountAmount));
    }


    private BigDecimal scale(BigDecimal value) {

        return value.setScale(2, RoundingMode.HALF_UP);
    }

}
