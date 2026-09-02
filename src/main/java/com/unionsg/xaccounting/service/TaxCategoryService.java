package com.unionsg.xaccounting.service;

import com.unionsg.xaccounting.dto.TaxCategoryDTO;
import com.unionsg.xaccounting.entity.TaxCategory;
import com.unionsg.xaccounting.repository.TaxCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaxCategoryService {

    private final TaxCategoryRepository taxCategoryRepository;

    @Transactional
    public TaxCategoryDTO create(TaxCategoryDTO dto) {
        TaxCategory entity = TaxCategory.builder()
                .name(dto.getName())
                .type(dto.getType())
                .rate(dto.getRate())
                .build();
        return toDto(taxCategoryRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<TaxCategoryDTO> getAll() {
        return taxCategoryRepository.findByDeletedFalse().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TaxCategoryDTO getById(Long id) {
        return toDto(taxCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tax category not found with id: " + id)));
    }

    @Transactional
    public TaxCategoryDTO update(Long id, TaxCategoryDTO dto) {
        TaxCategory entity = taxCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tax category not found with id: " + id));
        entity.setName(dto.getName());
        entity.setType(dto.getType());
        entity.setRate(dto.getRate());
        return toDto(taxCategoryRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        TaxCategory entity = taxCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tax category not found with id: " + id));
        entity.setDeleted(true);
        taxCategoryRepository.save(entity);
    }

    private TaxCategoryDTO toDto(TaxCategory entity) {
        return TaxCategoryDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .type(entity.getType())
                .rate(entity.getRate())
                .build();
    }
}
