package com.unionsg.xaccounting.service.bankrec;

import com.unionsg.xaccounting.dto.bankrec.MatchingRuleRequest;
import com.unionsg.xaccounting.dto.bankrec.MatchingRuleResponse;
import com.unionsg.xaccounting.entity.bankrec.BankMatchingRule;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.bankrec.BankMatchingRuleRepository;
import com.unionsg.xaccounting.repository.settings.BankAccountRepository;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.security.util.SecurityUtils;
import com.unionsg.xaccounting.service.bankrec.engine.MatchingCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Setup for the automatic matching rules (create, enquiry, update, delete). */
@Service
@RequiredArgsConstructor
public class BankMatchingRuleService {

    private final BankMatchingRuleRepository repository;
    private final BankAccountRepository bankAccountRepository;

    @Transactional(readOnly = true)
    public List<MatchingRuleResponse> list() {
        return repository.findByDeletedFalseOrderByPriorityAscIdAsc().stream().map(BankRecMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MatchingRuleResponse get(Long id) {
        return BankRecMapper.toResponse(find(id));
    }

    @Transactional
    public MatchingRuleResponse create(MatchingRuleRequest request) {
        String name = requireName(request);
        if (repository.existsByNameIgnoreCaseAndDeletedFalse(name)) {
            throw new BusinessException("A matching rule named \"" + name + "\" already exists");
        }
        BankMatchingRule rule = new BankMatchingRule();
        apply(rule, request, name);
        return BankRecMapper.toResponse(repository.save(rule));
    }

    @Transactional
    public MatchingRuleResponse update(Long id, MatchingRuleRequest request) {
        BankMatchingRule rule = find(id);
        String name = requireName(request);
        if (repository.existsByNameIgnoreCaseAndDeletedFalseAndIdNot(name, id)) {
            throw new BusinessException("A matching rule named \"" + name + "\" already exists");
        }
        apply(rule, request, name);
        return BankRecMapper.toResponse(repository.save(rule));
    }

    /** Matches the rule already made keep its name as text, so a rule can always be deleted. */
    @Transactional
    public void delete(Long id) {
        BankMatchingRule rule = find(id);
        User user = SecurityUtils.getCurrentUser();
        rule.softDelete(user != null ? String.valueOf(user.getId()) : null);
        repository.save(rule);
    }

    /** The rules to run for a bank account, as the engine sees them. Empty means "use the standard rule". */
    @Transactional(readOnly = true)
    public List<MatchingCriteria> criteriaFor(Long bankAccountId, Long onlyRuleId) {
        if (onlyRuleId != null) {
            BankMatchingRule rule = find(onlyRuleId);
            if (rule.getBankAccount() != null && !rule.getBankAccount().getId().equals(bankAccountId)) {
                throw new BusinessException("Rule \"" + rule.getName() + "\" is limited to another bank account");
            }
            return List.of(toCriteria(rule));
        }
        return repository.findApplicable(bankAccountId).stream().map(BankMatchingRuleService::toCriteria).toList();
    }

    static MatchingCriteria toCriteria(BankMatchingRule rule) {
        return new MatchingCriteria(
                rule.getName(),
                rule.getDateToleranceDays(),
                Boolean.TRUE.equals(rule.getMatchReference()),
                Boolean.TRUE.equals(rule.getMatchTransactionNumber()),
                Boolean.TRUE.equals(rule.getMatchDescription()),
                Boolean.TRUE.equals(rule.getMatchChequeNumber()),
                Boolean.TRUE.equals(rule.getMatchCounterparty()),
                rule.getAutoConfirmThreshold(),
                rule.getSuggestThreshold());
    }

    private void apply(BankMatchingRule rule, MatchingRuleRequest request, String name) {
        int tolerance = request.getDateToleranceDays() != null ? request.getDateToleranceDays() : 3;
        int autoConfirm = request.getAutoConfirmThreshold() != null ? request.getAutoConfirmThreshold() : 85;
        int suggest = request.getSuggestThreshold() != null ? request.getSuggestThreshold() : 60;
        if (tolerance < 0 || tolerance > 60) {
            throw new BusinessException("Date tolerance must be between 0 and 60 days");
        }
        if (autoConfirm < 40 || autoConfirm > 100 || suggest < 40 || suggest > 100) {
            throw new BusinessException("Thresholds must be between 40 (amount only) and 100");
        }
        if (suggest > autoConfirm) {
            throw new BusinessException("The suggest threshold cannot be higher than the auto-confirm threshold");
        }
        rule.setName(name);
        rule.setDescription(trim(request.getDescription()));
        rule.setPriority(request.getPriority() != null ? request.getPriority() : 100);
        rule.setActive(request.getActive() == null || request.getActive());
        rule.setBankAccount(request.getBankAccountId() == null ? null : bankAccountRepository.findById(request.getBankAccountId())
                .filter(a -> !Boolean.TRUE.equals(a.getDeleted()))
                .orElseThrow(() -> new BusinessException("Bank account not found with ID: " + request.getBankAccountId())));
        rule.setDateToleranceDays(tolerance);
        rule.setMatchReference(request.getMatchReference() == null || request.getMatchReference());
        rule.setMatchTransactionNumber(request.getMatchTransactionNumber() == null || request.getMatchTransactionNumber());
        rule.setMatchDescription(request.getMatchDescription() == null || request.getMatchDescription());
        rule.setMatchChequeNumber(request.getMatchChequeNumber() == null || request.getMatchChequeNumber());
        rule.setMatchCounterparty(request.getMatchCounterparty() == null || request.getMatchCounterparty());
        rule.setAutoConfirmThreshold(autoConfirm);
        rule.setSuggestThreshold(suggest);
    }

    private BankMatchingRule find(Long id) {
        return repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matching rule not found with ID: " + id));
    }

    private static String requireName(MatchingRuleRequest request) {
        String name = trim(request.getName());
        if (name == null) {
            throw new BusinessException("Rule name is required");
        }
        return name;
    }

    private static String trim(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }
}
