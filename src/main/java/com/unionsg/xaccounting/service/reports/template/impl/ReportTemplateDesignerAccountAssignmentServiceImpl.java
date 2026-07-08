package com.unionsg.xaccounting.service.reports.template.impl;

import com.unionsg.xaccounting.dto.reports.*;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.reports.ReportTemplateSection;
import com.unionsg.xaccounting.entity.reports.ReportTemplateSectionAccount;
import com.unionsg.xaccounting.enums.AccountStatus;
import com.unionsg.xaccounting.enums.AccountType;
import com.unionsg.xaccounting.enums.SectionType;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.reports.ReportTemplateSectionAccountRepository;
import com.unionsg.xaccounting.repository.reports.ReportTemplateSectionRepository;
import com.unionsg.xaccounting.service.reports.exception.*;
import com.unionsg.xaccounting.service.reports.mapper.ReportTemplateSectionAccountMapper;
import com.unionsg.xaccounting.service.reports.mapper.ReportTemplateSectionTreeMapper;
import com.unionsg.xaccounting.service.reports.template.ReportTemplateDesignerAccountAssignmentService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportTemplateDesignerAccountAssignmentServiceImpl implements ReportTemplateDesignerAccountAssignmentService {

    private final ReportTemplateSectionRepository sectionRepository;
    private final ReportTemplateSectionAccountRepository assignmentRepository;
    private final AccountRepository accountRepository;
    private final ReportTemplateSectionTreeMapper treeMapper;
    private final ReportTemplateSectionAccountMapper assignmentMapper;

    @Override
    @Transactional
    public ReportTemplateSectionDesignerResponseDto assignAccounts(Long sectionId, DesignerSectionAccountAssignRequestDto request) {
        ReportTemplateSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new TemplateNotFoundException("Template section not found: " + sectionId));

        validateSectionAllowAccounts(section);

        // validate duplicates in payload
        Set<Long> uniqueAccountIds = request.accountIds().stream().collect(Collectors.toSet());

        for (Long accountId : uniqueAccountIds) {
            if (assignmentRepository.existsByReportTemplateSectionIdAndAccountId(sectionId, accountId)) {
                throw new TemplateSectionAccountAlreadyAssignedException("Account already assigned. sectionId=" + sectionId + " accountId=" + accountId);
            }
        }

        // preserve display order: incoming displayOrder is base, subsequent accounts increment by 1
        final int baseOrder = request.displayOrder();
        List<Long> orderedAccountIds = uniqueAccountIds.stream().sorted().toList();

        List<ReportTemplateSectionAccount> entities = new java.util.ArrayList<>();
        for (int i = 0; i < orderedAccountIds.size(); i++) {
            Long accountId = orderedAccountIds.get(i);
            AccountEntity account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new TemplateNotFoundException("Account not found: " + accountId));
            entities.add(ReportTemplateSectionAccount.builder()
                    .reportTemplateSection(section)
                    .account(account)
                    .displayOrder(baseOrder + i)
                    .build());
        }


        assignmentRepository.saveAll(entities);
        return new ReportTemplateSectionDesignerResponseDto(section.getReportTemplate().getId(), treeMapper.toTreeNode(findRoot(section.getReportTemplate().getId()),
                sectionRepository.findByReportTemplateId(section.getReportTemplate().getId())));
    }

    @Override
    @Transactional
    public ReportTemplateSectionDesignerResponseDto removeAccounts(Long sectionId, DesignerSectionAccountRemoveRequestDto request) {
        // remove only existing assignments
        for (Long accountId : request.accountIds()) {
            assignmentRepository.findByReportTemplateSectionIdAndAccountId(sectionId, accountId)
                    .ifPresent(assignmentRepository::delete);
        }

        ReportTemplateSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new TemplateNotFoundException("Template section not found: " + sectionId));

        return new ReportTemplateSectionDesignerResponseDto(section.getReportTemplate().getId(), treeMapper.toTreeNode(findRoot(section.getReportTemplate().getId()),
                sectionRepository.findByReportTemplateId(section.getReportTemplate().getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public DesignerSectionAccountListResponseDto listAssignedAccounts(Long sectionId) {
        List<ReportTemplateSectionAccount> assigned = assignmentRepository.findByReportTemplateSectionIdOrderByDisplayOrderAsc(sectionId);
        List<ReportTemplateSectionAccountListItemDto> content = assigned.stream()
                .map(a -> ReportTemplateSectionAccountListItemDto.builder().accountId(a.getAccount().getId()).displayOrder(a.getDisplayOrder()).build())
                .toList();
        return new DesignerSectionAccountListResponseDto(content);
    }

    @Override
    public DesignerAccountSearchPageResponseDto searchUnassignedAccounts(Long sectionId, String search, AccountType accountType, AccountStatus status, int page, int size) {
        // fallback: fetch all accounts with spec then filter by assigned ids (simpler, still layout-only)
        Specification<AccountEntity> spec = buildAccountSearchSpec(search, accountType, status);
        Pageable pageable = PageRequest.of(page, size);
        Page<AccountEntity> accounts = accountRepository.findAll(spec, pageable);

        Set<Long> assignedIds = assignmentRepository.findByReportTemplateSectionId(sectionId).stream()
                .map(a -> a.getAccount().getId())
                .collect(Collectors.toSet());

        List<DesignerAccountListItemDto> filtered = accounts.getContent().stream()
                .filter(a -> !assignedIds.contains(a.getId()))
                .map(a -> DesignerAccountListItemDto.builder()
                        .accountEntityId(a.getId())
                        .accountNumber(a.getAccountId())
                        .accountName(a.getAccountName())
                        .accountType(a.getCoaClearTo().getChartOfAccount().getAccountType())
                        .subType(a.getCoaClearTo().getClearToCode() != null ? a.getCoaClearTo().getClearToCode().toString() : null)
                        .status(a.getIsActive() != null && a.getIsActive() ? AccountStatus.ACTIVE : AccountStatus.INACTIVE)
                        .balance(null)
                        .displayOrder(null)
                        .build())
                .toList();

        return new DesignerAccountSearchPageResponseDto(
                filtered,
                accounts.getNumber(),
                accounts.getSize(),
                accounts.getTotalElements(),
                accounts.getTotalPages(),
                accounts.isLast()
        );
    }

    @Override
    public DesignerAccountSearchPageResponseDto searchAssignedAccounts(Long sectionId, String search, AccountType accountType, AccountStatus status, int page, int size) {
        Specification<AccountEntity> spec = buildAccountSearchSpec(search, accountType, status);
        Pageable pageable = PageRequest.of(page, size);
        Page<AccountEntity> accounts = accountRepository.findAll(spec, pageable);

        // preserve display order for assigned picker using assignment displayOrder
        var assignedMap = assignmentRepository.findByReportTemplateSectionIdOrderByDisplayOrderAsc(sectionId).stream()
                .collect(Collectors.toMap(a -> a.getAccount().getId(), ReportTemplateSectionAccount::getDisplayOrder, (a, b) -> a));

        List<DesignerAccountListItemDto> filtered = accounts.getContent().stream()
                .filter(a -> assignedMap.containsKey(a.getId()))
                .sorted(Comparator.comparingInt(a -> assignedMap.get(a.getId())))
                .map(a -> DesignerAccountListItemDto.builder()
                        .accountEntityId(a.getId())
                        .accountNumber(a.getAccountId())
                        .accountName(a.getAccountName())
                        .accountType(a.getCoaClearTo().getChartOfAccount().getAccountType())
                        .subType(a.getCoaClearTo().getClearToCode() != null ? a.getCoaClearTo().getClearToCode().toString() : null)
                        .status(a.getIsActive() != null && a.getIsActive() ? AccountStatus.ACTIVE : AccountStatus.INACTIVE)
                        .balance(null)
                        .displayOrder(assignedMap.get(a.getId()))
                        .build())
                .toList();

        return new DesignerAccountSearchPageResponseDto(
                filtered,
                accounts.getNumber(),
                accounts.getSize(),
                accounts.getTotalElements(),
                accounts.getTotalPages(),
                accounts.isLast()
        );
    }

    // @Override
    @Transactional(readOnly = true)
    public List<ReportTemplateSectionAccountListItemDto> listUnassignedAccountsIds(Long sectionId) {
        Set<Long> assignedIds = assignmentRepository.findByReportTemplateSectionId(sectionId).stream()
                .map(a -> a.getAccount().getId())
                .collect(Collectors.toSet());
        return List.of();
    }

    private void validateSectionAllowAccounts(ReportTemplateSection section) {
        if (section.getSectionType() == null) return;
        if (section.getSectionType() != SectionType.SECTION) {
            throw new IllegalArgumentException("Cannot assign accounts to non-SECTION formula section. sectionId=" + section.getId());
        }
        // keep consistency with formula
        if (section.getFormula() != null && !section.getFormula().isBlank()) {
            section.setFormula(null);
            sectionRepository.save(section);
        }
    }

    private Specification<AccountEntity> buildAccountSearchSpec(String search, AccountType accountType, AccountStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), false));

            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("accountId")), like),
                        cb.like(cb.lower(root.get("accountName")), like)
                ));
            }

            if (accountType != null) {
                predicates.add(cb.equal(root.get("coaClearTo").get("chartOfAccount").get("accountType"), accountType));
            }

            if (status != null) {
                boolean isActive = status == AccountStatus.ACTIVE;
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private ReportTemplateSection findRoot(Long templateId) {
        List<ReportTemplateSection> all = sectionRepository.findByReportTemplateId(templateId);
        return all.stream().filter(s -> s.getParentSection() == null)
                .min(Comparator.comparingInt(ReportTemplateSection::getDisplayOrder))
                .orElse(null);
    }
}

