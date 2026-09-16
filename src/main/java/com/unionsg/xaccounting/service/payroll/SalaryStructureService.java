package com.unionsg.xaccounting.service.payroll;

import com.unionsg.xaccounting.dto.payroll.CreateSalaryStructureRequest;
import com.unionsg.xaccounting.dto.payroll.SalaryStructureLineRequest;
import com.unionsg.xaccounting.dto.payroll.SalaryStructureLineResponse;
import com.unionsg.xaccounting.dto.payroll.SalaryStructureResponse;
import com.unionsg.xaccounting.entity.payroll.PayComponent;
import com.unionsg.xaccounting.entity.payroll.SalaryStructure;
import com.unionsg.xaccounting.entity.payroll.SalaryStructureLine;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.payroll.SalaryStructureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalaryStructureService {

    private final SalaryStructureRepository salaryStructureRepository;
    private final PayComponentService payComponentService;

    @Transactional
    public SalaryStructureResponse create(CreateSalaryStructureRequest request) {
        SalaryStructure structure = new SalaryStructure();
        structure.setName(request.getName());
        structure.setDescription(request.getDescription());

        List<SalaryStructureLine> lines = new ArrayList<>();
        if (request.getLines() != null) {
            for (SalaryStructureLineRequest lineRequest : request.getLines()) {
                SalaryStructureLine line = new SalaryStructureLine();
                line.setSalaryStructure(structure);
                line.setPayComponent(payComponentService.getEntity(lineRequest.getPayComponentId()));
                line.setValue(lineRequest.getValue());
                lines.add(line);
            }
        }
        structure.setLines(lines);

        return toResponse(salaryStructureRepository.save(structure));
    }

    @Transactional(readOnly = true)
    public List<SalaryStructureResponse> getAll() {
        return salaryStructureRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SalaryStructureResponse getById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public SalaryStructure getEntity(Long id) {
        return salaryStructureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salary structure not found: " + id));
    }

    public SalaryStructureResponse toResponse(SalaryStructure structure) {
        List<SalaryStructureLineResponse> lines = structure.getLines().stream()
                .map(line -> {
                    PayComponent component = line.getPayComponent();
                    return SalaryStructureLineResponse.builder()
                            .id(line.getId())
                            .payComponentId(component.getId())
                            .payComponentName(component.getName())
                            .category(component.getCategory())
                            .side(component.getSide())
                            .value(line.getValue())
                            .build();
                })
                .toList();

        return SalaryStructureResponse.builder()
                .id(structure.getId())
                .name(structure.getName())
                .description(structure.getDescription())
                .active(structure.isActive())
                .lines(lines)
                .build();
    }
}
