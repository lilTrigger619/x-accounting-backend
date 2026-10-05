package com.unionsg.xaccounting.service.prepayment;

import com.unionsg.xaccounting.dto.prepayment.CreatePrepaymentTypeRequest;
import com.unionsg.xaccounting.dto.prepayment.PrepaymentTypeResponse;
import com.unionsg.xaccounting.entity.prepayment.PrepaymentType;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.prepayment.PrepaymentRepository;
import com.unionsg.xaccounting.repository.prepayment.PrepaymentTypeRepository;
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
class PrepaymentTypeServiceTest {

    @Mock
    private PrepaymentTypeRepository repository;

    @Mock
    private PrepaymentRepository prepaymentRepository;

    @InjectMocks
    private PrepaymentTypeService service;

    private static PrepaymentType existing(long id, String name) {
        PrepaymentType type = new PrepaymentType();
        type.setId(id);
        type.setName(name);
        type.setActive(true);
        return type;
    }

    private static CreatePrepaymentTypeRequest request(String name) {
        CreatePrepaymentTypeRequest request = new CreatePrepaymentTypeRequest();
        request.setName(name);
        return request;
    }

    @Test
    void createTrimsTheNameAndStartsActive() {
        when(repository.existsByNameIgnoreCaseAndDeletedFalse("Prepaid Rent")).thenReturn(false);
        when(repository.save(any(PrepaymentType.class))).thenAnswer(inv -> inv.getArgument(0));

        PrepaymentTypeResponse response = service.create(request("  Prepaid Rent "));

        assertThat(response.getName()).isEqualTo("Prepaid Rent");
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
        when(repository.findById(1L)).thenReturn(Optional.of(existing(1L, "Prepaid Rent")));
        when(repository.existsByNameIgnoreCaseAndDeletedFalseAndIdNot("Prepaid Insurance", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, request("Prepaid Insurance")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void deleteRefusesATypeUsedByPrepayments() {
        when(repository.findById(1L)).thenReturn(Optional.of(existing(1L, "Prepaid Rent")));
        when(prepaymentRepository.existsByPrepaymentTypeId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Deactivate it instead");
        verify(repository, never()).save(any());
    }

    @Test
    void deleteSoftDeletesAnUnusedType() {
        PrepaymentType type = existing(1L, "Prepaid Rent");
        when(repository.findById(1L)).thenReturn(Optional.of(type));
        when(prepaymentRepository.existsByPrepaymentTypeId(1L)).thenReturn(false);

        service.delete(1L);

        assertThat(type.getDeleted()).isTrue();
        assertThat(type.getActive()).isFalse();
        verify(repository).save(type);
    }

    @Test
    void deletedTypesAreNotFound() {
        PrepaymentType type = existing(1L, "Prepaid Rent");
        type.setDeleted(true);
        when(repository.findById(1L)).thenReturn(Optional.of(type));

        assertThatThrownBy(() -> service.get(1L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
