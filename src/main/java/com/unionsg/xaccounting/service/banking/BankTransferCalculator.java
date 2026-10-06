package com.unionsg.xaccounting.service.banking;

import com.unionsg.xaccounting.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Works out a bank transfer's converted and base-currency amounts. Pure arithmetic so it can be
 * shared by the form preview, saving a draft and posting.
 *
 * <p>Rates: {@code exchangeRate} is source to destination (1 source = rate destination).
 * {@code sourceBaseRate}/{@code destinationBaseRate} convert each side to the base (accounting)
 * currency. A side already in base currency always has a base rate of 1. When the destination
 * is foreign and its base rate is left blank, it is implied from the other two rates, which
 * means no exchange gain or loss; entering the destination's own book rate is what produces
 * one. When both sides are foreign, the source base rate is required.</p>
 *
 * <p>Exchange gain/loss = base value received by the destination minus base value taken from
 * the source (excluding charges). Positive is a gain.</p>
 */
public final class BankTransferCalculator {

    private static final int RATE_SCALE = 10;

    private BankTransferCalculator() {
    }

    public record Input(
            BigDecimal amount,
            BigDecimal feeAmount,
            String sourceCurrency,
            String destinationCurrency,
            String baseCurrency,
            BigDecimal exchangeRate,
            BigDecimal sourceBaseRate,
            BigDecimal destinationBaseRate
    ) {
    }

    public record Result(
            BigDecimal amount,
            BigDecimal feeAmount,
            BigDecimal exchangeRate,
            BigDecimal convertedAmount,
            BigDecimal sourceBaseRate,
            BigDecimal destinationBaseRate,
            BigDecimal baseAmount,
            BigDecimal baseConvertedAmount,
            BigDecimal baseFeeAmount,
            BigDecimal exchangeGainLoss
    ) {
        /** Total taken out of the source account in base currency (transfer plus charges). */
        public BigDecimal baseTotalDebitedFromSource() {
            return baseAmount.add(baseFeeAmount);
        }
    }

    public static Result calculate(Input in) {
        BigDecimal amount = money(in.amount());
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("Amount must be greater than zero");
        }
        BigDecimal fee = in.feeAmount() == null ? BigDecimal.ZERO.setScale(2) : money(in.feeAmount());
        if (fee.signum() < 0) {
            throw new BusinessException("Transfer charges cannot be negative");
        }

        String source = normalize(in.sourceCurrency());
        String destination = normalize(in.destinationCurrency());
        String base = normalize(in.baseCurrency());
        boolean sameCurrency = source.equals(destination);
        boolean sourceIsBase = source.equals(base);
        boolean destinationIsBase = destination.equals(base);

        BigDecimal exchangeRate;
        if (sameCurrency) {
            exchangeRate = BigDecimal.ONE;
        } else {
            exchangeRate = positiveOrNull(in.exchangeRate(), "Exchange rate");
            if (exchangeRate == null) {
                throw new BusinessException("Exchange rate is required when transferring from "
                        + source + " to " + destination);
            }
        }
        BigDecimal converted = money(amount.multiply(exchangeRate));
        if (converted.signum() <= 0) {
            throw new BusinessException("The converted amount rounds to zero; check the exchange rate");
        }

        BigDecimal sourceBaseRate;
        if (sourceIsBase) {
            sourceBaseRate = BigDecimal.ONE;
        } else if (destinationIsBase) {
            // 1 source = exchangeRate destination = exchangeRate base.
            sourceBaseRate = exchangeRate;
        } else {
            sourceBaseRate = positiveOrNull(in.sourceBaseRate(), "Source currency rate");
            if (sourceBaseRate == null) {
                throw new BusinessException("Enter the " + source + " to " + base
                        + " rate so the transfer can be posted in " + base);
            }
        }

        BigDecimal baseAmount = money(amount.multiply(sourceBaseRate));
        BigDecimal baseFee = money(fee.multiply(sourceBaseRate));

        BigDecimal destinationBaseRate;
        BigDecimal baseConverted;
        if (destinationIsBase) {
            destinationBaseRate = BigDecimal.ONE;
            baseConverted = converted;
        } else if (sameCurrency) {
            destinationBaseRate = sourceBaseRate;
            baseConverted = baseAmount;
        } else {
            BigDecimal entered = positiveOrNull(in.destinationBaseRate(), "Destination currency rate");
            if (entered == null) {
                destinationBaseRate = sourceBaseRate.divide(exchangeRate, RATE_SCALE, RoundingMode.HALF_UP);
                baseConverted = baseAmount;
            } else {
                destinationBaseRate = entered;
                baseConverted = money(converted.multiply(entered));
            }
        }

        return new Result(
                amount,
                fee,
                exchangeRate,
                converted,
                sourceBaseRate,
                destinationBaseRate,
                baseAmount,
                baseConverted,
                baseFee,
                baseConverted.subtract(baseAmount)
        );
    }

    private static BigDecimal positiveOrNull(BigDecimal value, String label) {
        if (value == null) {
            return null;
        }
        if (value.signum() <= 0) {
            throw new BusinessException(label + " must be greater than zero");
        }
        return value;
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String normalize(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new BusinessException("Currency is required");
        }
        return currency.trim().toUpperCase();
    }
}
