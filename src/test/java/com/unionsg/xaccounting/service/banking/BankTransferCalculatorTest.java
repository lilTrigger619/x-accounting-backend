package com.unionsg.xaccounting.service.banking;

import com.unionsg.xaccounting.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BankTransferCalculatorTest {

    private static BigDecimal d(String value) {
        return new BigDecimal(value);
    }

    private static BankTransferCalculator.Result calc(String amount, String fee, String source, String destination,
                                                      String rate, String sourceBase, String destinationBase) {
        return BankTransferCalculator.calculate(new BankTransferCalculator.Input(
                amount == null ? null : d(amount),
                fee == null ? null : d(fee),
                source, destination, "GHS",
                rate == null ? null : d(rate),
                sourceBase == null ? null : d(sourceBase),
                destinationBase == null ? null : d(destinationBase)));
    }

    @Test
    void sameCurrencyTransferHasNoConversionOrGainLoss() {
        BankTransferCalculator.Result r = calc("1000", "15", "GHS", "GHS", "3", null, null);

        assertThat(r.exchangeRate()).isEqualByComparingTo("1");
        assertThat(r.convertedAmount()).isEqualByComparingTo("1000");
        assertThat(r.baseAmount()).isEqualByComparingTo("1000");
        assertThat(r.baseConvertedAmount()).isEqualByComparingTo("1000");
        assertThat(r.baseFeeAmount()).isEqualByComparingTo("15");
        assertThat(r.exchangeGainLoss()).isEqualByComparingTo("0");
        assertThat(r.baseTotalDebitedFromSource()).isEqualByComparingTo("1015");
    }

    @Test
    void baseToForeignWithoutBookRateImpliesNoGainLoss() {
        BankTransferCalculator.Result r = calc("1200", null, "GHS", "USD", "0.08", null, null);

        assertThat(r.convertedAmount()).isEqualByComparingTo("96.00");
        assertThat(r.baseAmount()).isEqualByComparingTo("1200");
        assertThat(r.baseConvertedAmount()).isEqualByComparingTo("1200");
        assertThat(r.exchangeGainLoss()).isEqualByComparingTo("0");
    }

    @Test
    void baseToForeignAtBookRateBelowTheDealRateIsALoss() {
        // Paid GHS 1,200 for USD 96, but USD is carried at 12.00 → worth GHS 1,152.
        BankTransferCalculator.Result r = calc("1200", null, "GHS", "USD", "0.08", null, "12");

        assertThat(r.baseConvertedAmount()).isEqualByComparingTo("1152.00");
        assertThat(r.exchangeGainLoss()).isEqualByComparingTo("-48.00");
    }

    @Test
    void foreignToBaseValuesTheSourceAtTheDealRate() {
        BankTransferCalculator.Result r = calc("100", "2", "USD", "GHS", "12.5", null, null);

        assertThat(r.sourceBaseRate()).isEqualByComparingTo("12.5");
        assertThat(r.convertedAmount()).isEqualByComparingTo("1250.00");
        assertThat(r.baseAmount()).isEqualByComparingTo("1250.00");
        assertThat(r.baseFeeAmount()).isEqualByComparingTo("25.00");
        assertThat(r.exchangeGainLoss()).isEqualByComparingTo("0");
    }

    @Test
    void foreignToForeignNeedsTheSourceBaseRateAndCanRealiseAGain() {
        assertThatThrownBy(() -> calc("100", null, "USD", "EUR", "0.9", null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("USD to GHS");

        // USD 100 at 12 = GHS 1,200 sent; EUR 90 at 14 = GHS 1,260 received → gain 60.
        BankTransferCalculator.Result r = calc("100", null, "USD", "EUR", "0.9", "12", "14");
        assertThat(r.convertedAmount()).isEqualByComparingTo("90.00");
        assertThat(r.baseAmount()).isEqualByComparingTo("1200.00");
        assertThat(r.baseConvertedAmount()).isEqualByComparingTo("1260.00");
        assertThat(r.exchangeGainLoss()).isEqualByComparingTo("60.00");
    }

    @Test
    void crossCurrencyRequiresAnExchangeRate() {
        assertThatThrownBy(() -> calc("100", null, "GHS", "USD", null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Exchange rate is required");
    }

    @Test
    void rejectsNonPositiveAmountsAndNegativeCharges() {
        assertThatThrownBy(() -> calc("0", null, "GHS", "GHS", null, null, null))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> calc("10", "-1", "GHS", "GHS", null, null, null))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> calc("10", null, "GHS", "USD", "-2", null, null))
                .isInstanceOf(BusinessException.class);
    }
}
