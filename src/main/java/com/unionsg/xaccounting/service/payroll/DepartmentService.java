package com.unionsg.xaccounting.service.payroll;

import com.unionsg.xaccounting.dto.payroll.CreateDepartmentRequest;
import com.unionsg.xaccounting.dto.payroll.DepartmentResponse;
import com.unionsg.xaccounting.entity.payroll.Department;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.payroll.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Transactional
    public DepartmentResponse create(CreateDepartmentRequest request) {
        Department department = new Department();
        department.setName(request.getName());
        department.setCostCenterCode(request.getCostCenterCode());
        department.setDescription(request.getDescription());
        return toResponse(departmentRepository.save(department));
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAll() {
        return departmentRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Department getEntity(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));
    }

    public DepartmentResponse toResponse(Department department) {
        return DepartmentResponse.builder()
                .id(department.getId())
                .name(department.getName())
                .costCenterCode(department.getCostCenterCode())
                .description(department.getDescription())
                .active(department.isActive())
                .build();
    }
}
