package com.unionsg.xaccounting.service.deposit;

import com.unionsg.xaccounting.MapperLayer.DepositMapper;
import com.unionsg.xaccounting.dto.deposit.CreateDepositTypeRequest;
import com.unionsg.xaccounting.dto.deposit.DepositTypeResponse;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.deposit.DepositType;
import com.unionsg.xaccounting.enums.deposit.DepositDirection;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.deposit.DepositRepository;
import com.unionsg.xaccounting.repository.deposit.DepositTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DepositTypeService {

    private final DepositTypeRepository repository;
    private final DepositRepository depositRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public DepositTypeResponse create(CreateDepositTypeRequest request) {
        String name = validName(request.getName());
        if (repository.existsByNameIgnoreCaseAndDeletedFalse(name)) {
            throw new BusinessException("A deposit type named \"" + name + "\" already exists");
        }
        DepositType type = new DepositType();
        apply(type, request, name);
        type.setActive(true);
        return DepositMapper.toTypeResponse(repository.save(type));
    }

    @Transactional
    public DepositTypeResponse update(Long id, CreateDepositTypeRequest request) {
        DepositType type = getEntity(id);
        String name = validName(request.getName());
        if (repository.existsByNameIgnoreCaseAndDeletedFalseAndIdNot(name, id)) {
            throw new BusinessException("A deposit type named \"" + name + "\" already exists");
        }
        if (request.getDirection() != null && request.getDirection() != type.getDirection()
                && depositRepository.existsByDepositTypeId(id)) {
            throw new BusinessException("\"" + type.getName() + "\" is used by existing deposits, so its direction cannot change");
        }
        apply(type, request, name);
        return DepositMapper.toTypeResponse(repository.save(type));
    }

    @Transactional(readOnly = true)
    public DepositTypeResponse get(Long id) {
        return DepositMapper.toTypeResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<DepositTypeResponse> list(boolean activeOnly, DepositDirection direction) {
        List<DepositType> types = activeOnly ? repository.findByActiveTrueAndDeletedFalse() : repository.findByDeletedFalse();
        return types.stream()
                .filter(t -> direction == null || t.getDirection() == direction)
                .map(DepositMapper::toTypeResponse)
                .toList();
    }

    @Transactional
    public DepositTypeResponse setActive(Long id, boolean active) {
        DepositType type = getEntity(id);
        type.setActive(active);
        return DepositMapper.toTypeResponse(repository.save(type));
    }

    /** Soft-deletes a type no deposit uses yet; used types can only be deactivated. */
    @Transactional
    public void delete(Long id) {
        DepositType type = getEntity(id);
        if (depositRepository.existsByDepositTypeId(id)) {
            throw new BusinessException("\"" + type.getName() + "\" is used by existing deposits. Deactivate it instead.");
        }
        type.setDeleted(true);
        type.setDeletedAt(LocalDateTime.now());
        type.setActive(false);
        repository.save(type);
    }

    /** Loads a type that can be put on a new deposit of the given direction. */
    @Transactional(readOnly = true)
    public DepositType getUsable(Long id, DepositDirection direction) {
        if (id == null) {
            throw new BusinessException("A deposit type is required");
        }
        DepositType type = getEntity(id);
        if (!Boolean.TRUE.equals(type.getActive())) {
            throw new BusinessException("Deposit type \"" + type.getName() + "\" is inactive and cannot be used");
        }
        if (direction != null && type.getDirection() != direction) {
            throw new BusinessException("Deposit type \"" + type.getName() + "\" is for a "
                    + type.getDirection().getLabel().toLowerCase() + ", not a " + direction.getLabel().toLowerCase());
        }
        return type;
    }

    private DepositType getEntity(Long id) {
        return repository.findById(id)
                .filter(t -> !Boolean.TRUE.equals(t.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Deposit type not found with ID: " + id));
    }

    private void apply(DepositType type, CreateDepositTypeRequest request, String name) {
        if (request.getDirection() == null) {
            throw new BusinessException("Choose whether this type is for deposits paid or deposits received");
        }
        if (request.getDescription() != null && request.getDescription().length() > 500) {
            throw new BusinessException("A deposit type description can be at most 500 characters");
        }
        AccountEntity depositAccount = account(request.getDepositAccountId());
        AccountEntity forfeitureAccount = account(request.getForfeitureAccountId());
        DepositAccountRules.assertHoldingAccount(depositAccount, request.getDirection());
        DepositAccountRules.assertForfeitureAccount(forfeitureAccount, request.getDirection());

        type.setName(name);
        type.setDescription(request.getDescription() == null || request.getDescription().isBlank() ? null : request.getDescription().trim());
        type.setDirection(request.getDirection());
        type.setRefundableByDefault(request.getRefundableByDefault() == null || request.getRefundableByDefault());
        type.setInterestBearingByDefault(Boolean.TRUE.equals(request.getInterestBearingByDefault()));
        type.setDepositAccount(depositAccount);
        type.setForfeitureAccount(forfeitureAccount);
    }

    private AccountEntity account(Long id) {
        if (id == null) {
            return null;
        }
        return accountRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Account not found with ID: " + id));
    }

    private static String validName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("A deposit type name is required");
        }
        if (name.trim().length() > 100) {
            throw new BusinessException("A deposit type name can be at most 100 characters");
        }
        return name.trim();
    }
}
