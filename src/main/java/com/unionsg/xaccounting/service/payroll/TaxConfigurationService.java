package com.unionsg.xaccounting.service.payroll;

import com.unionsg.xaccounting.dto.payroll.CreateTaxConfigurationRequest;
import com.unionsg.xaccounting.dto.payroll.TaxBracketRequest;
import com.unionsg.xaccounting.dto.payroll.TaxBracketResponse;
import com.unionsg.xaccounting.dto.payroll.TaxConfigurationResponse;
import com.unionsg.xaccounting.entity.payroll.TaxBracket;
import com.unionsg.xaccounting.entity.payroll.TaxConfiguration;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.payroll.TaxConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages versioned income-tax configurations and computes tax on a given taxable income using
 * whichever configuration was effective on a given date (§10). Because the lookup is always
 * "effective as of period X", a tax law change that takes effect next month never touches the
 * calculation this service already performed for last month's posted payroll run.
 */
@Service
@RequiredArgsConstructor
public class TaxConfigurationService {

    private final TaxConfigurationRepository taxConfigurationRepository;

    @Transactional
    public TaxConfigurationResponse create(CreateTaxConfigurationRequest request) {
        TaxConfiguration config = new TaxConfiguration();
        config.setName(request.getName());
        config.setJurisdiction(request.getJurisdiction());
        config.setEffectiveFrom(request.getEffectiveFrom());
        config.setEffectiveTo(request.getEffectiveTo());
        config.setTaxPayableAccountCode(request.getTaxPayableAccountCode());

        List<TaxBracket> brackets = new ArrayList<>();
        if (request.getBrackets() != null) {
            for (TaxBracketRequest br : request.getBrackets()) {
                TaxBracket bracket = new TaxBracket();
                bracket.setTaxConfiguration(config);
                bracket.setLineNumber(br.getLineNumber());
                bracket.setMinIncome(br.getMinIncome());
                bracket.setMaxIncome(br.getMaxIncome());
                bracket.setRatePercent(br.getRatePercent());
                brackets.add(bracket);
            }
        }
        config.setBrackets(brackets);

        return toResponse(taxConfigurationRepository.save(config));
    }

    @Transactional(readOnly = true)
    public List<TaxConfigurationResponse> getAll() {
        return taxConfigurationRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TaxConfiguration getEffectiveAsOf(LocalDate asOf) {
        return taxConfigurationRepository.findEffectiveAsOf(asOf)
                .orElseThrow(() -> new BusinessException(
                        "No tax configuration is effective as of " + asOf + " - configure one before running payroll"));
    }

    @Transactional(readOnly = true)
    public TaxConfiguration getEntity(Long id) {
        return taxConfigurationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tax configuration not found: " + id));
    }

    /** Progressive-bracket tax computation: each bracket's rate applies only to the slice of income within it. */
    public BigDecimal computeTax(TaxConfiguration config, BigDecimal taxableIncome) {
        if (taxableIncome == null || taxableIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal tax = BigDecimal.ZERO;
        for (TaxBracket bracket : config.getBrackets()) {
            BigDecimal min = bracket.getMinIncome() != null ? bracket.getMinIncome() : BigDecimal.ZERO;
            BigDecimal max = bracket.getMaxIncome();
            if (taxableIncome.compareTo(min) <= 0) {
                continue;
            }
            BigDecimal upper = max != null ? max.min(taxableIncome) : taxableIncome;
            BigDecimal slice = upper.subtract(min);
            if (slice.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            tax = tax.add(slice.multiply(bracket.getRatePercent()).divide(BigDecimal.valueOf(100)));
        }
        return tax.setScale(2, RoundingMode.HALF_UP);
    }

    public TaxConfigurationResponse toResponse(TaxConfiguration config) {
        List<TaxBracketResponse> brackets = config.getBrackets().stream()
                .map(b -> TaxBracketResponse.builder()
                        .id(b.getId())
                        .lineNumber(b.getLineNumber())
                        .minIncome(b.getMinIncome())
                        .maxIncome(b.getMaxIncome())
                        .ratePercent(b.getRatePercent())
                        .build())
                .toList();

        return TaxConfigurationResponse.builder()
                .id(config.getId())
                .name(config.getName())
                .jurisdiction(config.getJurisdiction())
                .effectiveFrom(config.getEffectiveFrom())
                .effectiveTo(config.getEffectiveTo())
                .active(config.isActive())
                .taxPayableAccountCode(config.getTaxPayableAccountCode())
                .brackets(brackets)
                .build();
    }
}
