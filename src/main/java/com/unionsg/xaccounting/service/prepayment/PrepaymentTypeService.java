package com.unionsg.xaccounting.service.prepayment;

import com.unionsg.xaccounting.MapperLayer.PrepaymentMapper;
import com.unionsg.xaccounting.dto.prepayment.CreatePrepaymentTypeRequest;
import com.unionsg.xaccounting.dto.prepayment.PrepaymentTypeResponse;
import com.unionsg.xaccounting.entity.prepayment.PrepaymentType;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.prepayment.PrepaymentRepository;
import com.unionsg.xaccounting.repository.prepayment.PrepaymentTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrepaymentTypeService {

    private final PrepaymentTypeRepository repository;
    private final PrepaymentRepository prepaymentRepository;

    @Transactional
    public PrepaymentTypeResponse create(CreatePrepaymentTypeRequest request) {
        String name = validName(request.getName());
        if (repository.existsByNameIgnoreCaseAndDeletedFalse(name)) {
            throw new BusinessException("A prepayment type named \"" + name + "\" already exists");
        }

        PrepaymentType type = new PrepaymentType();
        apply(type, request, name);
        type.setActive(true);

        return PrepaymentMapper.toTypeResponse(repository.save(type));
    }

    @Transactional
    public PrepaymentTypeResponse update(Long id, CreatePrepaymentTypeRequest request) {
        PrepaymentType type = getEntity(id);
        String name = validName(request.getName());
        if (repository.existsByNameIgnoreCaseAndDeletedFalseAndIdNot(name, id)) {
            throw new BusinessException("A prepayment type named \"" + name + "\" already exists");
        }
        apply(type, request, name);
        return PrepaymentMapper.toTypeResponse(repository.save(type));
    }

    @Transactional(readOnly = true)
    public PrepaymentTypeResponse get(Long id) {
        return PrepaymentMapper.toTypeResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<PrepaymentTypeResponse> listActive() {
        return repository.findByActiveTrueAndDeletedFalse().stream().map(PrepaymentMapper::toTypeResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PrepaymentTypeResponse> listAll() {
        return repository.findByDeletedFalse().stream().map(PrepaymentMapper::toTypeResponse).toList();
    }

    @Transactional
    public PrepaymentTypeResponse setActive(Long id, boolean active) {
        PrepaymentType type = getEntity(id);
        type.setActive(active);
        return PrepaymentMapper.toTypeResponse(repository.save(type));
    }

    @Transactional
    public void deactivate(Long id) {
        setActive(id, false);
    }

    /** Soft-deletes a prepayment type that no prepayment uses yet; used types can only be deactivated. */
    @Transactional
    public void delete(Long id) {
        PrepaymentType type = getEntity(id);
        if (prepaymentRepository.existsByPrepaymentTypeId(id)) {
            throw new BusinessException("\"" + type.getName() + "\" is used by existing prepayments. Deactivate it instead.");
        }
        type.setDeleted(true);
        type.setDeletedAt(LocalDateTime.now());
        type.setActive(false);
        repository.save(type);
    }

    /** Loads a prepayment type that can be put on a new prepayment. */
    @Transactional(readOnly = true)
    public PrepaymentType getUsable(Long id) {
        PrepaymentType type = getEntity(id);
        if (!Boolean.TRUE.equals(type.getActive())) {
            throw new BusinessException("Prepayment type \"" + type.getName() + "\" is inactive");
        }
        return type;
    }

    private PrepaymentType getEntity(Long id) {
        return repository.findById(id)
                .filter(t -> !Boolean.TRUE.equals(t.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Prepayment type not found with ID: " + id));
    }

    private static String validName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("A prepayment type name is required");
        }
        if (name.trim().length() > 100) {
            throw new BusinessException("A prepayment type name can be at most 100 characters");
        }
        return name.trim();
    }

    private static void apply(PrepaymentType type, CreatePrepaymentTypeRequest request, String name) {
        if (request.getDescription() != null && request.getDescription().length() > 500) {
            throw new BusinessException("A prepayment type description can be at most 500 characters");
        }
        type.setName(name);
        type.setDescription(request.getDescription() == null || request.getDescription().isBlank() ? null : request.getDescription().trim());
    }
}
