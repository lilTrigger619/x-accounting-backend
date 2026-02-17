package com.unionsg.xaccounting.service;

//public class ChartOfAccountService {
//}
//
import ch.qos.logback.core.net.SyslogOutputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import com.unionsg.xaccounting.repository.ChartOfAccountRepository;
import com.unionsg.xaccounting.dto.ChartOfAccountDTO;
import com.unionsg.xaccounting.entity.ChartOfAccount;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChartOfAccountService {

    private final ChartOfAccountRepository chartOfAccountRepository;

    @Transactional
    public ChartOfAccountDTO createChartOfAccount(ChartOfAccountDTO dto) {
        if (chartOfAccountRepository.existsByCoaCode(dto.getCoaCode())) {
            throw new RuntimeException("Chart of Account code already exists: " + dto.getCoaCode());
        }

        ChartOfAccount entity = ChartOfAccount.builder()
                .coaCode(dto.getCoaCode())
                .coa_description(dto.getCoaDescription())
                .dateCreated(dto.getDateCreated())
                .build();

        ChartOfAccount saved = chartOfAccountRepository.save(entity);
        return convertToDTO(saved);
    }

    @Transactional(readOnly = true)
    public ChartOfAccountDTO getChartOfAccountById(Long id) {
        ChartOfAccount entity = chartOfAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Chart of Account not found with id: " + id));
        return convertToDTO(entity);
    }

    /*
    @Transactional(readOnly = true)
    public ChartOfAccountDTO getChartOfAccountByCode(Long coaCode) {
        ChartOfAccount entity = chartOfAccountRepository.findByCoaCode(coaCode)
                .orElseThrow(() -> new RuntimeException("Chart of Account not found with code: " + coaCode));
        return convertToDTO(entity);
    }
     */

    @Transactional(readOnly = true)
    public List<ChartOfAccountDTO> getAllChartOfAccounts() {
        return chartOfAccountRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChartOfAccountDTO updateChartOfAccount(Long id, ChartOfAccountDTO dto) {
        ChartOfAccount entity = chartOfAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Chart of Account not found with id: " + id));

        entity.setCoaCode(dto.getCoaCode());
        entity.setCoa_description(dto.getCoaDescription());
        entity.setDateCreated(dto.getDateCreated());

        ChartOfAccount updated = chartOfAccountRepository.save(entity);
        return convertToDTO(updated);
    }

    @Transactional
    public void deleteChartOfAccount(Long id) {
        /*
        if (!chartOfAccountRepository.existsById(id)) {
            throw new RuntimeException("Chart of Account not found with id: " + id);
        }
        */

        // instead of deleting set deleted to true
        ChartOfAccount entity = chartOfAccountRepository.findById(id)
                        .orElseThrow(()->new RuntimeException("Chart of account not found with given id"));
        entity.setDeleted(true);
        // test the delete endpoint
        entity.setDeletedBy("Admin");
        entity.setDeletedAt(LocalDateTime.now());
        //chartOfAccountRepository.deleteById(id);
    }

    private ChartOfAccountDTO convertToDTO(ChartOfAccount entity) {
        return ChartOfAccountDTO.builder()
                .id(entity.getId())
                .coaCode(entity.getCoaCode())
                .coaDescription(entity.getCoa_description())
                .dateCreated(entity.getDateCreated())
                .build();
    }
}