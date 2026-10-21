package com.unionsg.xaccounting.service.fixedasset;

import com.unionsg.xaccounting.MapperLayer.FixedAssetMapper;
import com.unionsg.xaccounting.dto.fixedasset.AccountRoleResponse;
import com.unionsg.xaccounting.dto.fixedasset.AssetCategoryResponse;
import com.unionsg.xaccounting.dto.fixedasset.SaveAssetCategoryRequest;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.fixedasset.AssetCategory;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationConvention;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationMethod;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.fixedasset.AssetCategoryRepository;
import com.unionsg.xaccounting.repository.fixedasset.FixedAssetRepository;
import com.unionsg.xaccounting.service.fixedasset.FixedAssetAccountRules.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Asset categories carry the defaults a new asset starts from (method, convention, life, residual
 * rate) and the GL accounts its postings use unless the asset overrides them.
 */
@Service
@RequiredArgsConstructor
public class AssetCategoryService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final AssetCategoryRepository repository;
    private final FixedAssetRepository assetRepository;
    private final FixedAssetAccountRules rules;

    @Transactional
    public AssetCategoryResponse create(SaveAssetCategoryRequest request) {
        AssetCategory category = new AssetCategory();
        apply(category, request, null);
        return toResponse(repository.save(category));
    }

    @Transactional
    public AssetCategoryResponse update(Long id, SaveAssetCategoryRequest request) {
        AssetCategory category = getEntity(id);
        apply(category, request, id);
        return toResponse(repository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        AssetCategory category = getEntity(id);
        long used = assetRepository.countByCategoryIdAndDeletedFalse(id);
        if (used > 0) {
            throw new BusinessException("\"" + category.getName() + "\" is used by " + used
                    + " asset(s) and can't be deleted. Deactivate it instead.");
        }
        category.setDeleted(true);
        category.setActive(false);
        repository.save(category);
    }

    @Transactional
    public AssetCategoryResponse setActive(Long id, boolean active) {
        AssetCategory category = getEntity(id);
        category.setActive(active);
        return toResponse(repository.save(category));
    }

    @Transactional(readOnly = true)
    public AssetCategoryResponse get(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<AssetCategoryResponse> list(boolean activeOnly) {
        return (activeOnly ? repository.findByActiveTrueAndDeletedFalseOrderByNameAsc() : repository.findByDeletedFalseOrderByNameAsc())
                .stream().map(this::toResponse).toList();
    }

    public AssetCategory getEntity(Long id) {
        return repository.findById(id)
                .filter(c -> !Boolean.TRUE.equals(c.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Asset category not found with ID: " + id));
    }

    private void apply(AssetCategory c, SaveAssetCategoryRequest r, Long id) {
        String code = trim(r.getCode());
        String name = trim(r.getName());
        if (code == null) throw new BusinessException("A category code is required");
        if (name == null) throw new BusinessException("A category name is required");
        if (code.length() > 30) throw new BusinessException("A category code can be at most 30 characters");
        if (name.length() > 100) throw new BusinessException("A category name can be at most 100 characters");
        boolean codeTaken = id == null ? repository.existsByCodeIgnoreCaseAndDeletedFalse(code)
                : repository.existsByCodeIgnoreCaseAndDeletedFalseAndIdNot(code, id);
        if (codeTaken) throw new BusinessException("A category with code \"" + code + "\" already exists");
        boolean nameTaken = id == null ? repository.existsByNameIgnoreCaseAndDeletedFalse(name)
                : repository.existsByNameIgnoreCaseAndDeletedFalseAndIdNot(name, id);
        if (nameTaken) throw new BusinessException("A category named \"" + name + "\" already exists");

        DepreciationMethod method = r.getDefaultMethod() != null ? r.getDefaultMethod() : DepreciationMethod.STRAIGHT_LINE;
        DepreciationConvention convention = r.getDefaultConvention() != null ? r.getDefaultConvention() : DepreciationConvention.DAILY_PRORATA;
        Integer life = r.getDefaultUsefulLifeMonths();
        if (life != null && (life <= 0 || life > 1200)) {
            throw new BusinessException("The default useful life must be between 1 and 1200 months");
        }
        if (method == DepreciationMethod.STRAIGHT_LINE && life == null) {
            throw new BusinessException("Straight line needs a default useful life");
        }
        BigDecimal residualRate = r.getDefaultResidualRate();
        if (residualRate != null && (residualRate.signum() < 0 || residualRate.compareTo(HUNDRED) >= 0)) {
            throw new BusinessException("The default residual value must be at least 0% and below 100% of cost");
        }
        BigDecimal annualRate = r.getDefaultAnnualRate();
        if (annualRate != null && (annualRate.signum() <= 0 || annualRate.compareTo(HUNDRED) > 0)) {
            throw new BusinessException("The default annual rate must be above 0% and at most 100%");
        }
        if (method == DepreciationMethod.REDUCING_BALANCE && annualRate == null && life == null) {
            throw new BusinessException("Reducing balance needs a default annual rate or a default useful life");
        }

        c.setCode(code);
        c.setName(name);
        c.setDescription(trim(r.getDescription()));
        c.setDefaultMethod(method);
        c.setDefaultConvention(convention);
        c.setDefaultUsefulLifeMonths(method == DepreciationMethod.NOT_DEPRECIATED ? null : life);
        c.setDefaultResidualRate(residualRate);
        c.setDefaultAnnualRate(method == DepreciationMethod.REDUCING_BALANCE ? annualRate : null);
        c.setCostAccount(account(r.getCostAccountId(), Role.COST));
        c.setAccumulatedDepreciationAccount(account(r.getAccumulatedDepreciationAccountId(), Role.ACCUMULATED_DEPRECIATION));
        c.setDepreciationExpenseAccount(account(r.getDepreciationExpenseAccountId(), Role.DEPRECIATION_EXPENSE));
        c.setAccumulatedImpairmentAccount(account(r.getAccumulatedImpairmentAccountId(), Role.ACCUMULATED_IMPAIRMENT));
        c.setImpairmentLossAccount(account(r.getImpairmentLossAccountId(), Role.IMPAIRMENT_LOSS));
        c.setDisposalGainAccount(account(r.getDisposalGainAccountId(), Role.DISPOSAL_GAIN));
        c.setDisposalLossAccount(account(r.getDisposalLossAccountId(), Role.DISPOSAL_LOSS));
        if (r.getActive() != null) c.setActive(r.getActive());

        AccountEntity cost = rules.resolve(c, Role.COST);
        rules.assertDistinct(cost, rules.resolve(c, Role.ACCUMULATED_DEPRECIATION), rules.resolve(c, Role.ACCUMULATED_IMPAIRMENT));
    }

    private AccountEntity account(Long id, Role role) {
        return id == null ? null : rules.validate(rules.byId(id, role.getLabel()), role);
    }

    AssetCategoryResponse toResponse(AssetCategory c) {
        AssetCategoryResponse r = new AssetCategoryResponse();
        r.setId(c.getId());
        r.setCode(c.getCode());
        r.setName(c.getName());
        r.setDescription(c.getDescription());
        r.setDefaultMethod(c.getDefaultMethod());
        r.setDefaultConvention(c.getDefaultConvention());
        r.setDefaultUsefulLifeMonths(c.getDefaultUsefulLifeMonths());
        r.setDefaultResidualRate(c.getDefaultResidualRate());
        r.setDefaultAnnualRate(c.getDefaultAnnualRate());
        r.setActive(c.getActive());
        r.setAssetCount(c.getId() != null ? assetRepository.countByCategoryIdAndDeletedFalse(c.getId()) : 0L);
        r.setCreatedAt(c.getCreatedAt());
        List<AccountRoleResponse> roles = new ArrayList<>();
        for (Role role : Role.values()) {
            AccountRoleResponse a = new AccountRoleResponse();
            a.setRole(role.name());
            a.setLabel(role.getLabel());
            AccountEntity own = FixedAssetAccountRules.categoryAccount(c, role);
            a.setOwn(FixedAssetMapper.toRef(own));
            a.setSource(own != null ? "CATEGORY" : "MAPPING");
            try {
                a.setResolved(FixedAssetMapper.toRef(rules.resolve(c, role)));
            } catch (BusinessException e) {
                a.setError(e.getMessage());
            }
            roles.add(a);
        }
        r.setAccounts(roles);
        return r;
    }

    private static String trim(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
