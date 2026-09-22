package com.unionsg.xaccounting.service.payroll;

import com.unionsg.xaccounting.dto.payroll.CreatePayrollGroupRequest;
import com.unionsg.xaccounting.dto.payroll.PayrollGroupResponse;
import com.unionsg.xaccounting.entity.payroll.PayrollGroup;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.payroll.PayrollGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PayrollGroupService {

    private final PayrollGroupRepository payrollGroupRepository;

    @Transactional
    public PayrollGroupResponse create(CreatePayrollGroupRequest request) {
        PayrollGroup group = new PayrollGroup();
        group.setName(request.getName());
        group.setPayFrequency(request.getPayFrequency());
        group.setDescription(request.getDescription());
        return toResponse(payrollGroupRepository.save(group));
    }

    @Transactional
    public PayrollGroupResponse update(Long id, CreatePayrollGroupRequest request) {
        PayrollGroup group = getEntity(id);
        group.setName(request.getName());
        group.setPayFrequency(request.getPayFrequency());
        group.setDescription(request.getDescription());
        return toResponse(payrollGroupRepository.save(group));
    }

    @Transactional
    public PayrollGroupResponse setActive(Long id, boolean active) {
        PayrollGroup group = getEntity(id);
        group.setActive(active);
        return toResponse(payrollGroupRepository.save(group));
    }

    @Transactional(readOnly = true)
    public List<PayrollGroupResponse> getAll() {
        return payrollGroupRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PayrollGroup getEntity(Long id) {
        return payrollGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll group not found: " + id));
    }

    public PayrollGroupResponse toResponse(PayrollGroup group) {
        return PayrollGroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .payFrequency(group.getPayFrequency())
                .description(group.getDescription())
                .active(group.isActive())
                .build();
    }
}
