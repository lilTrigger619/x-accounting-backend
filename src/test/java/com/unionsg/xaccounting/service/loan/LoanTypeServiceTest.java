package com.unionsg.xaccounting.service.loan;

import com.unionsg.xaccounting.dto.loan.CreateLoanTypeRequest;
import com.unionsg.xaccounting.dto.loan.LoanTypeResponse;
import com.unionsg.xaccounting.entity.loan.LoanType;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.loan.LoanRepository;
import com.unionsg.xaccounting.repository.loan.LoanTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanTypeServiceTest {

    @Mock
    private LoanTypeRepository repository;

    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private LoanTypeService service;

    private static LoanType existing(long id, String name) {
        LoanType type = new LoanType();
        type.setId(id);
        type.setName(name);
        type.setActive(true);
        return type;
    }

    private static CreateLoanTypeRequest request(String name) {
        CreateLoanTypeRequest request = new CreateLoanTypeRequest();
        request.setName(name);
        return request;
    }

    @Test
    void createTrimsTheNameAndStartsActive() {
        when(repository.existsByNameIgnoreCaseAndDeletedFalse("Bank Loan")).thenReturn(false);
        when(repository.save(any(LoanType.class))).thenAnswer(inv -> inv.getArgument(0));

        LoanTypeResponse response = service.create(request("  Bank Loan "));

        assertThat(response.getName()).isEqualTo("Bank Loan");
        assertThat(response.getActive()).isTrue();
    }

    @Test
    void createRejectsABlankName() {
        assertThatThrownBy(() -> service.create(request(" ")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("name is required");
    }

    @Test
    void updateRejectsANameAnotherTypeUses() {
        when(repository.findById(1L)).thenReturn(Optional.of(existing(1L, "Bank Loan")));
        when(repository.existsByNameIgnoreCaseAndDeletedFalseAndIdNot("Staff Loan", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, request("Staff Loan")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void deleteRefusesATypeUsedByLoans() {
        when(repository.findById(1L)).thenReturn(Optional.of(existing(1L, "Bank Loan")));
        when(loanRepository.existsByLoanTypeId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Deactivate it instead");
        verify(repository, never()).save(any());
    }

    @Test
    void deleteSoftDeletesAnUnusedType() {
        LoanType type = existing(1L, "Bank Loan");
        when(repository.findById(1L)).thenReturn(Optional.of(type));
        when(loanRepository.existsByLoanTypeId(1L)).thenReturn(false);

        service.delete(1L);

        assertThat(type.getDeleted()).isTrue();
        assertThat(type.getActive()).isFalse();
        verify(repository).save(type);
    }

    @Test
    void deletedTypesAreNotFound() {
        LoanType type = existing(1L, "Bank Loan");
        type.setDeleted(true);
        when(repository.findById(1L)).thenReturn(Optional.of(type));

        assertThatThrownBy(() -> service.get(1L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
