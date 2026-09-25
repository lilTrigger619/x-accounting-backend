package com.unionsg.xaccounting.service.loan;

import com.unionsg.xaccounting.MapperLayer.LoanMapper;
import com.unionsg.xaccounting.dto.loan.CreateLoanTypeRequest;
import com.unionsg.xaccounting.dto.loan.LoanTypeResponse;
import com.unionsg.xaccounting.entity.loan.LoanType;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.loan.LoanTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanTypeService {

    private final LoanTypeRepository repository;

    @Transactional
    public LoanTypeResponse create(CreateLoanTypeRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BusinessException("A loan type name is required");
        }
        if (repository.existsByName(request.getName())) {
            throw new BusinessException("A loan type named \"" + request.getName() + "\" already exists");
        }

        LoanType type = new LoanType();
        type.setName(request.getName());
        type.setDescription(request.getDescription());
        type.setDefaultDirection(request.getDefaultDirection());
        type.setActive(true);

        return LoanMapper.toTypeResponse(repository.save(type));
    }

    @Transactional(readOnly = true)
    public List<LoanTypeResponse> listActive() {
        return repository.findByActiveTrue().stream().map(LoanMapper::toTypeResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<LoanTypeResponse> listAll() {
        return repository.findAll().stream().map(LoanMapper::toTypeResponse).toList();
    }

    @Transactional
    public void deactivate(Long id) {
        LoanType type = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Loan type not found with ID: " + id));
        type.setActive(false);
        repository.save(type);
    }
}
