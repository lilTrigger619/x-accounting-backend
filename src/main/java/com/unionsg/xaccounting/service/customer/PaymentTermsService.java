package com.unionsg.xaccounting.service.customer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import com.unionsg.xaccounting.repository.CustomerPaymentTermsRepo;
import com.unionsg.xaccounting.dto.customer.PaymentTermsDTO;
import com.unionsg.xaccounting.dto.customer.PaymentTermsRequestDTO;
import com.unionsg.xaccounting.entity.customer.PaymentTerms;
import com.unionsg.xaccounting.response.ApiResponse;

import com.unionsg.xaccounting.exception.ResourceNotFoundException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentTermsService {
    private final CustomerPaymentTermsRepo customerPaymentTermsRepo;

    public PaymentTerms getCustomerPaymentTerms(PaymentTermsRequestDTO request){
        Optional<PaymentTerms> paymentTerm =  customerPaymentTermsRepo.findById(request.getId());
        return paymentTerm.orElseThrow(()-> new ResourceNotFoundException("Not payment terms found with provided user id"));
//        if (paymentTerm.isEmpty())
////            return ApiResponse.builder()
////                    .message("Failed to get payment terms")
//            paymentTerm
//            return PaymentTermsDTO.builder()
//        return PaymentTermsDTO.builder()
//                .paymentTermType(paymentTerm.getPaymentTermType())

    }
}
