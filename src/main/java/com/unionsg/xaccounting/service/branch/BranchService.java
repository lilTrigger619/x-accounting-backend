package com.unionsg.xaccounting.service.branch;

import com.unionsg.xaccounting.dto.branch.BranchRequest;
import com.unionsg.xaccounting.dto.branch.BranchResponse;
import com.unionsg.xaccounting.entity.branch.Branch;
import com.unionsg.xaccounting.entity.branch.CostCenter;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.branch.BranchRepository;
import com.unionsg.xaccounting.repository.branch.CostCenterRepository;
import com.unionsg.xaccounting.repository.branch.UserAccessScopeRepository;
import com.unionsg.xaccounting.repository.journal.JournalEntryRepository;
import com.unionsg.xaccounting.repository.journal.JournalLineRepository;
import com.unionsg.xaccounting.repository.settings.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BranchService {

    public static final String HEAD_OFFICE_CODE = "HO";
    private static final Pattern CODE = Pattern.compile("^[A-Z0-9][A-Z0-9_-]{0,19}$");

    private final BranchRepository repository;
    private final CostCenterRepository costCenterRepository;
    private final JournalLineRepository journalLineRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final UserAccessScopeRepository scopeRepository;
    private final BankAccountRepository bankAccountRepository;
    private final AccessScopeService accessScopeService;

    @Transactional
    public BranchResponse create(BranchRequest request) {
        String code = validCode(request.getCode());
        String name = validName(request.getName());
        if (repository.existsByCodeIgnoreCaseAndDeletedFalse(code)) {
            throw new BusinessException("A branch with code \"" + code + "\" already exists");
        }
        if (repository.existsByNameIgnoreCaseAndDeletedFalse(name)) {
            throw new BusinessException("A branch named \"" + name + "\" already exists");
        }
        Branch branch = new Branch();
        apply(branch, request, code, name);
        branch.setActive(true);
        boolean firstBranch = repository.findFirstByHeadOfficeTrueAndDeletedFalse().isEmpty();
        if (firstBranch) {
            branch.setHeadOffice(true);
        }
        Branch saved = repository.save(branch);
        if (Boolean.TRUE.equals(saved.getHeadOffice()) && !firstBranch) {
            moveHeadOfficeTo(saved);
        }
        CostCenter defaultCc = createDefaultCostCenter(saved);
        return BranchMapper.toResponse(saved, defaultCc, 1);
    }

    @Transactional
    public BranchResponse update(Long id, BranchRequest request) {
        Branch branch = getEntity(id);
        String code = validCode(request.getCode());
        String name = validName(request.getName());
        if (repository.existsByCodeIgnoreCaseAndDeletedFalseAndIdNot(code, id)) {
            throw new BusinessException("A branch with code \"" + code + "\" already exists");
        }
        if (repository.existsByNameIgnoreCaseAndDeletedFalseAndIdNot(name, id)) {
            throw new BusinessException("A branch named \"" + name + "\" already exists");
        }
        boolean wasHeadOffice = Boolean.TRUE.equals(branch.getHeadOffice());
        if (wasHeadOffice && Boolean.FALSE.equals(request.getHeadOffice())) {
            throw new BusinessException("\"" + branch.getName() + "\" is the Head Office. Make another branch the Head Office instead.");
        }
        apply(branch, request, code, name);
        Branch saved = repository.save(branch);
        if (!wasHeadOffice && Boolean.TRUE.equals(saved.getHeadOffice())) {
            if (!Boolean.TRUE.equals(saved.getActive())) {
                throw new BusinessException("An inactive branch can't be the Head Office");
            }
            moveHeadOfficeTo(saved);
        }
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public BranchResponse get(Long id) {
        Branch branch = getEntity(id);
        accessScopeService.assertCanSeeBranch(branch.getId());
        return toResponse(branch);
    }

    /** Branches the current user may see; everything for a Global user. */
    @Transactional(readOnly = true)
    public List<BranchResponse> list(boolean activeOnly, String search) {
        AccessScope scope = accessScopeService.current();
        String needle = search == null || search.isBlank() ? null : search.trim().toLowerCase(Locale.ROOT);
        List<Branch> branches = activeOnly
                ? repository.findByActiveTrueAndDeletedFalseOrderByNameAsc()
                : repository.findByDeletedFalseOrderByNameAsc();
        return branches.stream()
                .filter(b -> scope.coversBranch(b.getId()))
                .filter(b -> needle == null
                        || b.getName().toLowerCase(Locale.ROOT).contains(needle)
                        || b.getCode().toLowerCase(Locale.ROOT).contains(needle))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BranchResponse setActive(Long id, boolean active) {
        Branch branch = getEntity(id);
        if (!active && Boolean.TRUE.equals(branch.getHeadOffice())) {
            throw new BusinessException("The Head Office can't be deactivated. Make another branch the Head Office first.");
        }
        branch.setActive(active);
        return toResponse(repository.save(branch));
    }

    /** Soft-deletes a branch nothing points at yet; a used branch can only be deactivated. */
    @Transactional
    public void delete(Long id) {
        Branch branch = getEntity(id);
        if (Boolean.TRUE.equals(branch.getHeadOffice())) {
            throw new BusinessException("The Head Office can't be deleted");
        }
        if (journalLineRepository.existsByBranchId(id) || journalLineRepository.existsByCounterpartyBranchId(id)
                || journalEntryRepository.existsByBranchId(id)) {
            throw new BusinessException("\"" + branch.getName() + "\" has journal entries. Deactivate it instead.");
        }
        if (costCenterRepository.existsByBranchIdAndDeletedFalseAndBranchDefaultFalse(id)) {
            throw new BusinessException("\"" + branch.getName() + "\" still has cost centers. Delete or move them first, or deactivate the branch.");
        }
        if (scopeRepository.existsByBranchIdAndDeletedFalse(id)) {
            throw new BusinessException("Users are scoped to \"" + branch.getName() + "\". Change their access first, or deactivate the branch.");
        }
        if (bankAccountRepository.existsByHomeBranchId(id)) {
            throw new BusinessException("Bank accounts belong to \"" + branch.getName() + "\". Change their home branch first, or deactivate the branch.");
        }
        LocalDateTime now = LocalDateTime.now();
        for (CostCenter cc : costCenterRepository.findByBranchIdAndDeletedFalse(id)) {
            cc.setDeleted(true);
            cc.setDeletedAt(now);
            cc.setActive(false);
            costCenterRepository.save(cc);
        }
        branch.setDeleted(true);
        branch.setDeletedAt(now);
        branch.setActive(false);
        repository.save(branch);
    }

    /**
     * The Head Office's default cost center, creating the Head Office branch ("HO") and its
     * default cost center first if this install has none yet. Postings with nothing else to
     * go on (seeders, scheduled jobs, users without a default) land here.
     */
    @Transactional
    public CostCenter ensureHeadOfficeDefaultCostCenter() {
        Branch ho = repository.findFirstByHeadOfficeTrueAndDeletedFalse().orElseGet(() -> {
            Branch branch = new Branch();
            branch.setCode(repository.existsByCodeIgnoreCaseAndDeletedFalse(HEAD_OFFICE_CODE) ? "HQ" : HEAD_OFFICE_CODE);
            branch.setName(repository.existsByNameIgnoreCaseAndDeletedFalse("Head Office") ? "Head Office (main)" : "Head Office");
            branch.setDescription("Main office. Everything recorded before branches were introduced belongs here.");
            branch.setHeadOffice(true);
            branch.setActive(true);
            return repository.save(branch);
        });
        return costCenterRepository.findFirstByBranchIdAndBranchDefaultTrueAndDeletedFalse(ho.getId())
                .orElseGet(() -> createDefaultCostCenter(ho));
    }

    /** The default cost center of a branch, creating it if an old branch lost it. */
    @Transactional
    public CostCenter defaultCostCenterOf(Branch branch) {
        return costCenterRepository.findFirstByBranchIdAndBranchDefaultTrueAndDeletedFalse(branch.getId())
                .orElseGet(() -> createDefaultCostCenter(branch));
    }

    /** Loads a branch that new postings may use. */
    @Transactional(readOnly = true)
    public Branch getUsable(Long id) {
        Branch branch = getEntity(id);
        if (!Boolean.TRUE.equals(branch.getActive())) {
            throw new BusinessException("Branch \"" + branch.getName() + "\" is inactive and can't be used");
        }
        return branch;
    }

    Branch getEntity(Long id) {
        if (id == null) {
            throw new BusinessException("A branch is required");
        }
        return repository.findById(id)
                .filter(b -> !Boolean.TRUE.equals(b.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with ID: " + id));
    }

    BranchResponse toResponse(Branch branch) {
        CostCenter defaultCc = costCenterRepository
                .findFirstByBranchIdAndBranchDefaultTrueAndDeletedFalse(branch.getId()).orElse(null);
        long count = costCenterRepository.findByBranchIdAndDeletedFalse(branch.getId()).size();
        return BranchMapper.toResponse(branch, defaultCc, count);
    }

    CostCenter createDefaultCostCenter(Branch branch) {
        CostCenter cc = new CostCenter();
        String code = branch.getCode() + "-GEN";
        int n = 2;
        while (costCenterRepository.existsByCodeIgnoreCaseAndDeletedFalse(code)) {
            code = branch.getCode() + "-GEN" + n++;
        }
        cc.setCode(code);
        cc.setName(branch.getName() + " - General");
        cc.setDescription("Branch-level costs that aren't charged to a department");
        cc.setBranch(branch);
        cc.setBranchDefault(true);
        cc.setActive(true);
        return costCenterRepository.save(cc);
    }

    private void moveHeadOfficeTo(Branch newHeadOffice) {
        for (Branch other : repository.findByDeletedFalseOrderByNameAsc()) {
            if (!other.getId().equals(newHeadOffice.getId()) && Boolean.TRUE.equals(other.getHeadOffice())) {
                other.setHeadOffice(false);
                repository.save(other);
            }
        }
    }

    private void apply(Branch branch, BranchRequest request, String code, String name) {
        branch.setCode(code);
        branch.setName(name);
        branch.setDescription(trim(request.getDescription(), 500, "description"));
        branch.setAddress(trim(request.getAddress(), 300, "address"));
        branch.setPhone(trim(request.getPhone(), 50, "phone number"));
        String email = trim(request.getEmail(), 150, "email");
        if (email != null && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new BusinessException("\"" + email + "\" isn't a valid email address");
        }
        branch.setEmail(email);
        if (request.getHeadOffice() != null) {
            branch.setHeadOffice(request.getHeadOffice() || Boolean.TRUE.equals(branch.getHeadOffice()));
        }
    }

    static String validCode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException("A branch code is required");
        }
        String code = raw.trim().toUpperCase(Locale.ROOT);
        if (!CODE.matcher(code).matches()) {
            throw new BusinessException("A branch code can only use letters, digits, - and _ (at most 20 characters)");
        }
        return code;
    }

    private static String validName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("A branch name is required");
        }
        if (name.trim().length() > 100) {
            throw new BusinessException("A branch name can be at most 100 characters");
        }
        return name.trim();
    }

    private static String trim(String value, int max, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.trim().length() > max) {
            throw new BusinessException("The branch " + label + " can be at most " + max + " characters");
        }
        return value.trim();
    }
}
