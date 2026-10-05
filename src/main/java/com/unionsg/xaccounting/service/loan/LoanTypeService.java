package com.unionsg.xaccounting.service.loan;

import com.unionsg.xaccounting.MapperLayer.LoanMapper;
import com.unionsg.xaccounting.dto.loan.CreateLoanTypeRequest;
import com.unionsg.xaccounting.dto.loan.LoanTypeResponse;
import com.unionsg.xaccounting.entity.loan.LoanType;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.loan.LoanRepository;
import com.unionsg.xaccounting.repository.loan.LoanTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanTypeService {

    private final LoanTypeRepository repository;
    private final LoanRepository loanRepository;

    @Transactional
    public LoanTypeResponse create(CreateLoanTypeRequest request) {
        String name = validName(request.getName());
        if (repository.existsByNameIgnoreCaseAndDeletedFalse(name)) {
            throw new BusinessException("A loan type named \"" + name + "\" already exists");
        }

        LoanType type = new LoanType();
        apply(type, request, name);
        type.setActive(true);

        return LoanMapper.toTypeResponse(repository.save(type));
    }

    @Transactional
    public LoanTypeResponse update(Long id, CreateLoanTypeRequest request) {
        LoanType type = getEntity(id);
        String name = validName(request.getName());
        if (repository.existsByNameIgnoreCaseAndDeletedFalseAndIdNot(name, id)) {
            throw new BusinessException("A loan type named \"" + name + "\" already exists");
        }
        apply(type, request, name);
        return LoanMapper.toTypeResponse(repository.save(type));
    }

    @Transactional(readOnly = true)
    public LoanTypeResponse get(Long id) {
        return LoanMapper.toTypeResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<LoanTypeResponse> listActive() {
        return repository.findByActiveTrueAndDeletedFalse().stream().map(LoanMapper::toTypeResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<LoanTypeResponse> listAll() {
        return repository.findByDeletedFalse().stream().map(LoanMapper::toTypeResponse).toList();
    }

    @Transactional
    public LoanTypeResponse setActive(Long id, boolean active) {
        LoanType type = getEntity(id);
        type.setActive(active);
        return LoanMapper.toTypeResponse(repository.save(type));
    }

    @Transactional
    public void deactivate(Long id) {
        setActive(id, false);
    }

    /** Soft-deletes a loan type that no loan uses yet; used types can only be deactivated. */
    @Transactional
    public void delete(Long id) {
        LoanType type = getEntity(id);
        if (loanRepository.existsByLoanTypeId(id)) {
            throw new BusinessException("\"" + type.getName() + "\" is used by existing loans. Deactivate it instead.");
        }
        type.setDeleted(true);
        type.setDeletedAt(LocalDateTime.now());
        type.setActive(false);
        repository.save(type);
    }

    /** Loads a loan type that can be put on a new loan. */
    @Transactional(readOnly = true)
    public LoanType getUsable(Long id) {
        LoanType type = getEntity(id);
        if (!Boolean.TRUE.equals(type.getActive())) {
            throw new BusinessException("Loan type \"" + type.getName() + "\" is inactive");
        }
        return type;
    }

    private LoanType getEntity(Long id) {
        return repository.findById(id)
                .filter(t -> !Boolean.TRUE.equals(t.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Loan type not found with ID: " + id));
    }

    private static String validName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("A loan type name is required");
        }
        if (name.trim().length() > 100) {
            throw new BusinessException("A loan type name can be at most 100 characters");
        }
        return name.trim();
    }

    private static void apply(LoanType type, CreateLoanTypeRequest request, String name) {
        if (request.getDescription() != null && request.getDescription().length() > 500) {
            throw new BusinessException("A loan type description can be at most 500 characters");
        }
        type.setName(name);
        type.setDescription(request.getDescription() == null || request.getDescription().isBlank() ? null : request.getDescription().trim());
        type.setDefaultDirection(request.getDefaultDirection());
    }
}
