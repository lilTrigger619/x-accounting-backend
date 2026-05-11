package com.unionsg.xaccounting.service.invoice;

import com.unionsg.xaccounting.entity.invoice.Invoice;
import com.unionsg.xaccounting.entity.invoice.InvoiceItem;
import com.unionsg.xaccounting.enums.DiscountType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class InvoiceCalculationService {

    public void calculateInvoice(Invoice invoice) {

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (InvoiceItem item : invoice.getItems()) {

            BigDecimal quantity = item.getQuantity();
            BigDecimal unitPrice = item.getUnitPrice();
            BigDecimal taxRate = item.getTaxRate();

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

        invoice.setSubtotal(scale(subtotal));
        invoice.setTotalTax(scale(totalTax));

        calculateDiscount(invoice);

        BigDecimal total =
                subtotal
                        .add(totalTax)
                        .subtract(invoice.getDiscountAmount());

        invoice.setTotalAmount(scale(total));
        invoice.setTotalDue(scale(total));
    }


    private void calculateDiscount(Invoice invoice) {

        BigDecimal discountAmount = BigDecimal.ZERO;

        if (invoice.getDiscountType() == DiscountType.PERCENTAGE) {

            discountAmount =
                    invoice.getSubtotal()
                            .multiply(
                                    invoice.getDiscountValue()
                                            .divide(
                                                    BigDecimal.valueOf(100),
                                                    4,
                                                    RoundingMode.HALF_UP
                                            )
                            );
        }

        if (invoice.getDiscountType() == DiscountType.FIXED) {
            discountAmount = invoice.getDiscountValue();
        }

        invoice.setDiscountAmount(scale(discountAmount));
    }


    private BigDecimal scale(BigDecimal value) {

        return value.setScale(2, RoundingMode.HALF_UP);
    }

}
