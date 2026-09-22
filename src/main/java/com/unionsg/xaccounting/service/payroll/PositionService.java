package com.unionsg.xaccounting.service.payroll;

import com.unionsg.xaccounting.dto.payroll.CreatePositionRequest;
import com.unionsg.xaccounting.dto.payroll.PositionResponse;
import com.unionsg.xaccounting.entity.payroll.Department;
import com.unionsg.xaccounting.entity.payroll.Position;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.payroll.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PositionService {

    private final PositionRepository positionRepository;
    private final DepartmentService departmentService;

    @Transactional
    public PositionResponse create(CreatePositionRequest request) {
        Position position = new Position();
        position.setTitle(request.getTitle());
        position.setDescription(request.getDescription());
        if (request.getDepartmentId() != null) {
            position.setDepartment(departmentService.getEntity(request.getDepartmentId()));
        }
        return toResponse(positionRepository.save(position));
    }

    @Transactional
    public PositionResponse update(Long id, CreatePositionRequest request) {
        Position position = getEntity(id);
        position.setTitle(request.getTitle());
        position.setDescription(request.getDescription());
        position.setDepartment(request.getDepartmentId() != null
                ? departmentService.getEntity(request.getDepartmentId())
                : null);
        return toResponse(positionRepository.save(position));
    }

    @Transactional
    public PositionResponse setActive(Long id, boolean active) {
        Position position = getEntity(id);
        position.setActive(active);
        return toResponse(positionRepository.save(position));
    }

    @Transactional(readOnly = true)
    public List<PositionResponse> getAll() {
        return positionRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Position getEntity(Long id) {
        return positionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Position not found: " + id));
    }

    public PositionResponse toResponse(Position position) {
        Department department = position.getDepartment();
        return PositionResponse.builder()
                .id(position.getId())
                .title(position.getTitle())
                .departmentId(department != null ? department.getId() : null)
                .departmentName(department != null ? department.getName() : null)
                .description(position.getDescription())
                .active(position.isActive())
                .build();
    }
}
