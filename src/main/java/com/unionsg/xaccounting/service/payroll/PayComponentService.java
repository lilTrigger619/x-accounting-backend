package com.unionsg.xaccounting.service.payroll;

import com.unionsg.xaccounting.dto.payroll.CreatePayComponentRequest;
import com.unionsg.xaccounting.dto.payroll.PayComponentResponse;
import com.unionsg.xaccounting.entity.payroll.PayComponent;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.payroll.PayComponentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages the configurable pay components (§7-§9) that carry payroll's Chart-of-Accounts mapping
 * (§33, §53). This is the mechanism, not the Chart of Accounts itself: creating a component here
 * only records which account codes it should hit - {@code PayrollJournalService} is what resolves
 * those codes against the real {@code AccountRepository} at posting time, so a typo or a
 * not-yet-created account surfaces as a clear error before payroll posts, not as a silent
 * misclassification (§56).
 */
@Service
@RequiredArgsConstructor
public class PayComponentService {

    private final PayComponentRepository payComponentRepository;

    @Transactional
    public PayComponentResponse create(CreatePayComponentRequest request) {
        if (payComponentRepository.existsByCode(request.getCode())) {
            throw new BusinessException("A pay component with code " + request.getCode() + " already exists");
        }
        PayComponent component = new PayComponent();
        applyRequest(component, request);
        return toResponse(payComponentRepository.save(component));
    }

    @Transactional
    public PayComponentResponse update(Long id, CreatePayComponentRequest request) {
        PayComponent component = getEntity(id);
        applyRequest(component, request);
        return toResponse(payComponentRepository.save(component));
    }

    private void applyRequest(PayComponent component, CreatePayComponentRequest request) {
        component.setCode(request.getCode());
        component.setName(request.getName());
        component.setCategory(request.getCategory());
        component.setSide(request.getSide());
        component.setCalculationMethod(request.getCalculationMethod());
        component.setDefaultValue(request.getDefaultValue());
        component.setTaxable(request.isTaxable());
        component.setPensionable(request.isPensionable());
        component.setStatutory(request.isStatutory());
        component.setRecurring(request.isRecurring());
        component.setGlDebitAccountCode(request.getGlDebitAccountCode());
        component.setGlCreditAccountCode(request.getGlCreditAccountCode());
        component.setDescription(request.getDescription());
    }

    @Transactional(readOnly = true)
    public List<PayComponentResponse> getAll() {
        return payComponentRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PayComponent getEntity(Long id) {
        return payComponentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pay component not found: " + id));
    }

    @Transactional(readOnly = true)
    public PayComponent getByCode(String code) {
        return payComponentRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Pay component not found: " + code));
    }

    public PayComponentResponse toResponse(PayComponent component) {
        return PayComponentResponse.builder()
                .id(component.getId())
                .code(component.getCode())
                .name(component.getName())
                .category(component.getCategory())
                .side(component.getSide())
                .calculationMethod(component.getCalculationMethod())
                .defaultValue(component.getDefaultValue())
                .taxable(component.isTaxable())
                .pensionable(component.isPensionable())
                .statutory(component.isStatutory())
                .recurring(component.isRecurring())
                .active(component.isActive())
                .glDebitAccountCode(component.getGlDebitAccountCode())
                .glCreditAccountCode(component.getGlCreditAccountCode())
                .description(component.getDescription())
                .build();
    }
}
