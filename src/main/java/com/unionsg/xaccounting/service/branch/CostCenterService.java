package com.unionsg.xaccounting.service.branch;

import com.unionsg.xaccounting.dto.branch.CostCenterRequest;
import com.unionsg.xaccounting.dto.branch.CostCenterResponse;
import com.unionsg.xaccounting.entity.branch.Branch;
import com.unionsg.xaccounting.entity.branch.CostCenter;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.branch.CostCenterRepository;
import com.unionsg.xaccounting.repository.branch.UserAccessScopeRepository;
import com.unionsg.xaccounting.repository.branch.UserDimensionDefaultRepository;
import com.unionsg.xaccounting.repository.journal.JournalEntryRepository;
import com.unionsg.xaccounting.repository.journal.JournalLineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CostCenterService {

    private static final Pattern CODE = Pattern.compile("^[A-Z0-9][A-Z0-9_-]{0,29}$");

    private final CostCenterRepository repository;
    private final BranchService branchService;
    private final JournalLineRepository journalLineRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final UserAccessScopeRepository scopeRepository;
    private final UserDimensionDefaultRepository userDefaultRepository;
    private final AccessScopeService accessScopeService;

    @Transactional
    public CostCenterResponse create(CostCenterRequest request) {
        Branch branch = branchService.getUsable(request.getBranchId());
        String code = validCode(request.getCode());
        String name = validName(request.getName());
        if (repository.existsByCodeIgnoreCaseAndDeletedFalse(code)) {
            throw new BusinessException("A cost center with code \"" + code + "\" already exists");
        }
        if (repository.existsByBranchIdAndNameIgnoreCaseAndDeletedFalse(branch.getId(), name)) {
            throw new BusinessException("\"" + branch.getName() + "\" already has a cost center named \"" + name + "\"");
        }
        CostCenter cc = new CostCenter();
        cc.setCode(code);
        cc.setName(name);
        cc.setDescription(validDescription(request.getDescription()));
        cc.setBranch(branch);
        cc.setBranchDefault(false);
        cc.setActive(true);
        return BranchMapper.toResponse(repository.save(cc));
    }

    @Transactional
    public CostCenterResponse update(Long id, CostCenterRequest request) {
        CostCenter cc = getEntity(id);
        String code = validCode(request.getCode());
        String name = validName(request.getName());
        Long branchId = request.getBranchId() != null ? request.getBranchId() : cc.getBranch().getId();
        if (!branchId.equals(cc.getBranch().getId())) {
            if (Boolean.TRUE.equals(cc.getBranchDefault())) {
                throw new BusinessException("\"" + cc.getName() + "\" is its branch's default cost center and can't move to another branch");
            }
            if (journalLineRepository.existsByCostCenterId(id)) {
                throw new BusinessException("\"" + cc.getName() + "\" has journal entries, so it can't move to another branch. Create a new cost center in that branch instead.");
            }
            cc.setBranch(branchService.getUsable(branchId));
        }
        if (repository.existsByCodeIgnoreCaseAndDeletedFalseAndIdNot(code, id)) {
            throw new BusinessException("A cost center with code \"" + code + "\" already exists");
        }
        if (repository.existsByBranchIdAndNameIgnoreCaseAndDeletedFalseAndIdNot(branchId, name, id)) {
            throw new BusinessException("\"" + cc.getBranch().getName() + "\" already has a cost center named \"" + name + "\"");
        }
        cc.setCode(code);
        cc.setName(name);
        cc.setDescription(validDescription(request.getDescription()));
        return BranchMapper.toResponse(repository.save(cc));
    }

    @Transactional(readOnly = true)
    public CostCenterResponse get(Long id) {
        CostCenter cc = getEntity(id);
        accessScopeService.assertCanUseCostCenter(cc);
        return BranchMapper.toResponse(cc);
    }

    /** Cost centers the current user may see, optionally for one branch. */
    @Transactional(readOnly = true)
    public List<CostCenterResponse> list(Long branchId, boolean activeOnly, String search) {
        AccessScope scope = accessScopeService.current();
        String needle = search == null || search.isBlank() ? null : "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        return repository.search(branchId, activeOnly, needle).stream()
                .filter(cc -> scope.coversCostCenter(cc.getId()))
                .map(BranchMapper::toResponse)
                .toList();
    }

    @Transactional
    public CostCenterResponse setActive(Long id, boolean active) {
        CostCenter cc = getEntity(id);
        if (!active && Boolean.TRUE.equals(cc.getBranchDefault())) {
            throw new BusinessException("\"" + cc.getName() + "\" is its branch's default cost center. Deactivate the branch instead.");
        }
        if (active && !Boolean.TRUE.equals(cc.getBranch().getActive())) {
            throw new BusinessException("Activate the branch \"" + cc.getBranch().getName() + "\" first");
        }
        cc.setActive(active);
        return BranchMapper.toResponse(repository.save(cc));
    }

    @Transactional
    public void delete(Long id) {
        CostCenter cc = getEntity(id);
        if (Boolean.TRUE.equals(cc.getBranchDefault())) {
            throw new BusinessException("\"" + cc.getName() + "\" is its branch's default cost center and can't be deleted");
        }
        if (journalLineRepository.existsByCostCenterId(id) || journalEntryRepository.existsByCostCenterId(id)) {
            throw new BusinessException("\"" + cc.getName() + "\" has journal entries. Deactivate it instead.");
        }
        if (scopeRepository.existsByCostCenter(id)) {
            throw new BusinessException("Users are scoped to \"" + cc.getName() + "\". Change their access first, or deactivate it.");
        }
        if (userDefaultRepository.existsByCostCenterId(id)) {
            throw new BusinessException("\"" + cc.getName() + "\" is some users' default cost center. Change their default first, or deactivate it.");
        }
        cc.setDeleted(true);
        cc.setDeletedAt(LocalDateTime.now());
        cc.setActive(false);
        repository.save(cc);
    }

    /** Loads a cost center that new postings may use. */
    @Transactional(readOnly = true)
    public CostCenter getUsable(Long id) {
        CostCenter cc = getEntity(id);
        if (!Boolean.TRUE.equals(cc.getActive()) || !Boolean.TRUE.equals(cc.getBranch().getActive())) {
            throw new BusinessException("Cost center " + cc.getCode() + " (" + cc.getName() + ") is inactive and can't be used");
        }
        return cc;
    }

    CostCenter getEntity(Long id) {
        if (id == null) {
            throw new BusinessException("A cost center is required");
        }
        return repository.findById(id)
                .filter(c -> !Boolean.TRUE.equals(c.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Cost center not found with ID: " + id));
    }

    private static String validCode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException("A cost center code is required");
        }
        String code = raw.trim().toUpperCase(Locale.ROOT);
        if (!CODE.matcher(code).matches()) {
            throw new BusinessException("A cost center code can only use letters, digits, - and _ (at most 30 characters)");
        }
        return code;
    }

    private static String validName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("A cost center name is required");
        }
        if (name.trim().length() > 100) {
            throw new BusinessException("A cost center name can be at most 100 characters");
        }
        return name.trim();
    }

    private static String validDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        if (description.trim().length() > 500) {
            throw new BusinessException("A cost center description can be at most 500 characters");
        }
        return description.trim();
    }
}
