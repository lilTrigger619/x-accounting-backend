package com.unionsg.xaccounting.service.payroll;

import com.unionsg.xaccounting.dto.payroll.CreateWorkLocationRequest;
import com.unionsg.xaccounting.dto.payroll.WorkLocationResponse;
import com.unionsg.xaccounting.entity.payroll.WorkLocation;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.payroll.WorkLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkLocationService {

    private final WorkLocationRepository workLocationRepository;

    @Transactional
    public WorkLocationResponse create(CreateWorkLocationRequest request) {
        WorkLocation location = new WorkLocation();
        location.setName(request.getName());
        location.setAddress(request.getAddress());
        return toResponse(workLocationRepository.save(location));
    }

    @Transactional(readOnly = true)
    public List<WorkLocationResponse> getAll() {
        return workLocationRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public WorkLocation getEntity(Long id) {
        return workLocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work location not found: " + id));
    }

    public WorkLocationResponse toResponse(WorkLocation location) {
        return WorkLocationResponse.builder()
                .id(location.getId())
                .name(location.getName())
                .address(location.getAddress())
                .active(location.isActive())
                .build();
    }
}
