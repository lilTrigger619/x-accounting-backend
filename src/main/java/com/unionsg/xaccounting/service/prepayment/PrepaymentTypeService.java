package com.unionsg.xaccounting.service.prepayment;

import com.unionsg.xaccounting.MapperLayer.PrepaymentMapper;
import com.unionsg.xaccounting.dto.prepayment.CreatePrepaymentTypeRequest;
import com.unionsg.xaccounting.dto.prepayment.PrepaymentTypeResponse;
import com.unionsg.xaccounting.entity.prepayment.PrepaymentType;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.prepayment.PrepaymentTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PrepaymentTypeService {

    private final PrepaymentTypeRepository repository;

    @Transactional
    public PrepaymentTypeResponse create(CreatePrepaymentTypeRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BusinessException("A prepayment type name is required");
        }
        if (repository.existsByName(request.getName())) {
            throw new BusinessException("A prepayment type named \"" + request.getName() + "\" already exists");
        }

        PrepaymentType type = new PrepaymentType();
        type.setName(request.getName());
        type.setDescription(request.getDescription());
        type.setActive(true);

        return PrepaymentMapper.toTypeResponse(repository.save(type));
    }

    @Transactional(readOnly = true)
    public List<PrepaymentTypeResponse> listActive() {
        return repository.findByActiveTrue().stream().map(PrepaymentMapper::toTypeResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PrepaymentTypeResponse> listAll() {
        return repository.findAll().stream().map(PrepaymentMapper::toTypeResponse).toList();
    }

    @Transactional
    public void deactivate(Long id) {
        PrepaymentType type = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Prepayment type not found with ID: " + id));
        type.setActive(false);
        repository.save(type);
    }
}
