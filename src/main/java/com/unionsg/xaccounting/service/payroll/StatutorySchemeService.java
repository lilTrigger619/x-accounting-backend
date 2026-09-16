package com.unionsg.xaccounting.service.payroll;

import com.unionsg.xaccounting.dto.payroll.CreateStatutorySchemeRequest;
import com.unionsg.xaccounting.dto.payroll.StatutorySchemeResponse;
import com.unionsg.xaccounting.entity.payroll.StatutoryScheme;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.payroll.StatutorySchemeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatutorySchemeService {

    private final StatutorySchemeRepository statutorySchemeRepository;

    @Transactional
    public StatutorySchemeResponse create(CreateStatutorySchemeRequest request) {
        if (statutorySchemeRepository.existsByCode(request.getCode())) {
            throw new BusinessException("A statutory scheme with code " + request.getCode() + " already exists");
        }
        StatutoryScheme scheme = new StatutoryScheme();
        scheme.setCode(request.getCode());
        scheme.setName(request.getName());
        scheme.setCalculationBasis(request.getCalculationBasis());
        scheme.setEmployeeRatePercent(request.getEmployeeRatePercent());
        scheme.setEmployerRatePercent(request.getEmployerRatePercent());
        scheme.setEmployeeLiabilityAccountCode(request.getEmployeeLiabilityAccountCode());
        scheme.setEmployerExpenseAccountCode(request.getEmployerExpenseAccountCode());
        scheme.setEmployerLiabilityAccountCode(request.getEmployerLiabilityAccountCode());
        scheme.setEffectiveFrom(request.getEffectiveFrom());
        scheme.setEffectiveTo(request.getEffectiveTo());
        scheme.setDescription(request.getDescription());
        return toResponse(statutorySchemeRepository.save(scheme));
    }

    @Transactional(readOnly = true)
    public List<StatutorySchemeResponse> getAll() {
        return statutorySchemeRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<StatutoryScheme> getEffectiveSchemes(LocalDate asOf) {
        return statutorySchemeRepository.findEffectiveAsOf(asOf);
    }

    @Transactional(readOnly = true)
    public StatutoryScheme getEntity(Long id) {
        return statutorySchemeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Statutory scheme not found: " + id));
    }

    public StatutorySchemeResponse toResponse(StatutoryScheme scheme) {
        return StatutorySchemeResponse.builder()
                .id(scheme.getId())
                .code(scheme.getCode())
                .name(scheme.getName())
                .calculationBasis(scheme.getCalculationBasis())
                .employeeRatePercent(scheme.getEmployeeRatePercent())
                .employerRatePercent(scheme.getEmployerRatePercent())
                .employeeLiabilityAccountCode(scheme.getEmployeeLiabilityAccountCode())
                .employerExpenseAccountCode(scheme.getEmployerExpenseAccountCode())
                .employerLiabilityAccountCode(scheme.getEmployerLiabilityAccountCode())
                .effectiveFrom(scheme.getEffectiveFrom())
                .effectiveTo(scheme.getEffectiveTo())
                .active(scheme.isActive())
                .description(scheme.getDescription())
                .build();
    }
}
