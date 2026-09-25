package com.unionsg.xaccounting.MapperLayer;

import com.unionsg.xaccounting.dto.prepayment.PrepaymentLineResponse;
import com.unionsg.xaccounting.dto.prepayment.PrepaymentListItemResponse;
import com.unionsg.xaccounting.dto.prepayment.PrepaymentResponse;
import com.unionsg.xaccounting.dto.prepayment.PrepaymentTypeResponse;
import com.unionsg.xaccounting.entity.prepayment.Prepayment;
import com.unionsg.xaccounting.entity.prepayment.PrepaymentAmortizationLine;
import com.unionsg.xaccounting.entity.prepayment.PrepaymentType;

import java.util.List;

public class PrepaymentMapper {

    private PrepaymentMapper() {
        throw new UnsupportedOperationException("Mapper class cannot be instantiated");
    }

    public static PrepaymentTypeResponse toTypeResponse(PrepaymentType type) {
        PrepaymentTypeResponse response = new PrepaymentTypeResponse();
        response.setId(type.getId());
        response.setName(type.getName());
        response.setDescription(type.getDescription());
        response.setActive(type.getActive());
        return response;
    }

    public static PrepaymentLineResponse toLineResponse(PrepaymentAmortizationLine line) {
        PrepaymentLineResponse response = new PrepaymentLineResponse();
        response.setId(line.getId());
        response.setPeriodNumber(line.getPeriodNumber());
        response.setPeriodDate(line.getPeriodDate());
        response.setAmount(line.getAmount());
        response.setStatus(line.getStatus());
        return response;
    }

    public static PrepaymentResponse toResponse(Prepayment prepayment) {
        PrepaymentResponse response = new PrepaymentResponse();
        response.setId(prepayment.getId());
        response.setPrepaymentNumber(prepayment.getPrepaymentNumber());

        if (prepayment.getPrepaymentType() != null) {
            response.setPrepaymentTypeId(prepayment.getPrepaymentType().getId());
            response.setPrepaymentTypeName(prepayment.getPrepaymentType().getName());
        }

        response.setCounterpartyType(prepayment.getCounterpartyType());
        response.setCounterpartyName(prepayment.getCounterpartyName());
        response.setSupplierId(prepayment.getSupplier() != null ? prepayment.getSupplier().getId() : null);
        response.setEmployeeId(prepayment.getEmployee() != null ? prepayment.getEmployee().getId() : null);
        response.setTotalAmount(prepayment.getTotalAmount());
        response.setCurrency(prepayment.getCurrency());
        response.setPaymentDate(prepayment.getPaymentDate());
        response.setRecognitionStartDate(prepayment.getRecognitionStartDate());
        response.setNumberOfPeriods(prepayment.getNumberOfPeriods());
        response.setRecognitionFrequency(prepayment.getRecognitionFrequency());
        response.setAmountRecognized(prepayment.getAmountRecognized());
        response.setAmountRemaining(prepayment.getAmountRemaining());
        response.setNotes(prepayment.getNotes());
        response.setStatus(prepayment.getStatus());
        response.setSchedule(
                prepayment.getSchedule().stream().map(PrepaymentMapper::toLineResponse).toList()
        );
        return response;
    }

    public static PrepaymentListItemResponse toListItemResponse(Prepayment prepayment) {
        PrepaymentListItemResponse response = new PrepaymentListItemResponse();
        response.setId(prepayment.getId());
        response.setPrepaymentNumber(prepayment.getPrepaymentNumber());
        response.setPrepaymentTypeName(prepayment.getPrepaymentType() != null ? prepayment.getPrepaymentType().getName() : null);
        response.setCounterpartyName(prepayment.getCounterpartyName());
        response.setTotalAmount(prepayment.getTotalAmount());
        response.setAmountRecognized(prepayment.getAmountRecognized());
        response.setAmountRemaining(prepayment.getAmountRemaining());
        response.setCurrency(prepayment.getCurrency());
        response.setPaymentDate(prepayment.getPaymentDate());
        response.setStatus(prepayment.getStatus());
        return response;
    }

    public static List<PrepaymentListItemResponse> toListItemResponses(List<Prepayment> prepayments) {
        return prepayments.stream().map(PrepaymentMapper::toListItemResponse).toList();
    }
}
