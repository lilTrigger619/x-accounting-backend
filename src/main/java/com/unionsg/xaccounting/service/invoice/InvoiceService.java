package com.unionsg.xaccounting.service.invoice;

import com.unionsg.xaccounting.MapperLayer.InvoiceMapper;
import com.unionsg.xaccounting.dto.FileUploadRequestDto;
import com.unionsg.xaccounting.dto.invoice.CreateInvoiceRequest;
import com.unionsg.xaccounting.dto.invoice.InvoiceResponse;
import com.unionsg.xaccounting.dto.invoice.InvoiceTotalsResponse;
import com.unionsg.xaccounting.dto.invoice.InvoiceTotalsRow;

import com.unionsg.xaccounting.entity.customer.Customer;
import com.unionsg.xaccounting.entity.customer.PaymentTerms;
import com.unionsg.xaccounting.entity.invoice.Invoice;
import com.unionsg.xaccounting.entity.invoice.InvoiceItem;
import com.unionsg.xaccounting.enums.DocumentModule;
import com.unionsg.xaccounting.enums.EntityType;
import com.unionsg.xaccounting.enums.InvoiceStatus;
import com.unionsg.xaccounting.repository.CustomerPaymentTermsRepo;
import com.unionsg.xaccounting.repository.CustomerRepository;
import com.unionsg.xaccounting.repository.invoice.InvoiceRepository;
import com.unionsg.xaccounting.security.DocumentNumberGeneratorService;
import com.unionsg.xaccounting.security.util.SecurityUtils;
import com.unionsg.xaccounting.service.FileService.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final CustomerPaymentTermsRepo paymentTermsRepository;
    private final InvoiceCalculationService calculationService;
    private final DocumentNumberGeneratorService generator;
    private final FileService fileService;

    @Transactional
    public InvoiceResponse createInvoice(
            MultipartFile[] files,
            CreateInvoiceRequest request
    ) {

        Customer customer =
                customerRepository.findById(request.getCustomerId())
                        .orElseThrow(() ->
                                new RuntimeException("Customer not found"));

        PaymentTerms paymentTerms = null;

        if (request.getPaymentTermsId() != null) {

            paymentTerms =
                    paymentTermsRepository
                            .findById(request.getPaymentTermsId())
                            .orElseThrow(() ->
                                    new RuntimeException("Payment terms not found"));
        }

        Invoice invoice =
                InvoiceMapper.toEntity(
                        request,
                        customer,
                        paymentTerms
                );

        invoice.setCreatedAt(LocalDateTime.now());
        invoice.setInvoiceNumber(generator.generate(DocumentModule.INVOICE));

        calculationService.calculateInvoice(invoice);

        Invoice saved =
                invoiceRepository.save(invoice);

        FileUploadRequestDto fileUploadMetaDto = new FileUploadRequestDto();
        fileUploadMetaDto.setEntityType(EntityType.INVOICE);
        fileUploadMetaDto.setEntityId(saved.getId().toString());
        fileUploadMetaDto.setDescription("Invoice creation document upload");
        UUID currentUserId = SecurityUtils.getCurrentUser().getId();
        fileUploadMetaDto.setUploadedBy(currentUserId);
        fileService.uploadFile(files, fileUploadMetaDto);
        return InvoiceMapper.toResponse(saved);
    }


    // update service

    @Transactional
    public InvoiceResponse updateInvoice(
            Long invoiceId,
            CreateInvoiceRequest request
    ) {

        Invoice invoice =
                invoiceRepository.findById(invoiceId)
                        .orElseThrow(() ->
                                new RuntimeException("Invoice not found"));

        Customer customer =
                customerRepository.findById(request.getCustomerId())
                        .orElseThrow(() ->
                                new RuntimeException("Customer not found"));

        PaymentTerms paymentTerms = null;

        if (request.getPaymentTermsId() != null) {
            paymentTerms =
                    paymentTermsRepository
                            .findById(request.getPaymentTermsId())
                            .orElseThrow(() ->
                                    new RuntimeException("Payment terms not found"));
        }

        invoice.setCustomer(customer);
        invoice.setIssueDate(request.getIssueDate());
        invoice.setDueDate(request.getDueDate());
        invoice.setReference(request.getReference());
        invoice.setNotes(request.getNotes());
        invoice.setTerms(request.getTerms());
        invoice.setDiscountType(request.getDiscountType());
        invoice.setDiscountValue(request.getDiscountValue());
        invoice.setPaymentTerms(paymentTerms);

        invoice.getItems().clear();

        request.getItems().forEach(item -> {

            InvoiceItem entity =
                    new InvoiceItem();

            entity.setDescription(item.getDescription());
            entity.setQuantity(item.getQuantity());
            entity.setUnitPrice(item.getUnitPrice());
            entity.setTaxRate(item.getTaxRate());
            entity.setInvoice(invoice);

            invoice.getItems().add(entity);
        });

        calculationService.calculateInvoice(invoice);

        return InvoiceMapper.toResponse(
                invoiceRepository.save(invoice)
        );
    }


    // get invoice by id

    public InvoiceResponse getInvoice(Long id) {

        Invoice invoice =
                invoiceRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException("Invoice not found"));

        return InvoiceMapper.toResponse(invoice);
    }


    public Page<InvoiceResponse> getInvoices(
            Pageable pageable
    ) {

        return invoiceRepository
                .findAll(pageable)
                .map(InvoiceMapper::toResponse);
    }


    public void deleteInvoice(Long id) {

        Invoice invoice =
                invoiceRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException("Invoice not found"));

        invoiceRepository.delete(invoice);
    }


    @Transactional
    public InvoiceResponse sendInvoice(Long id) {

        Invoice invoice =
                invoiceRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException("Invoice not found"));

        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setSentAt(LocalDateTime.now());

        return InvoiceMapper.toResponse(
                invoiceRepository.save(invoice)
        );
    }


    @Transactional
    public InvoiceResponse markAsPaid(Long id) {

        Invoice invoice =
                invoiceRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException("Invoice not found"));

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now());

        return InvoiceMapper.toResponse(
                invoiceRepository.save(invoice)
        );
    }


    @Transactional
    public InvoiceResponse cancelInvoice(Long id) {

        Invoice invoice =
                invoiceRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException("Invoice not found"));

        invoice.setStatus(InvoiceStatus.CANCELLED);

        return InvoiceMapper.toResponse(
                invoiceRepository.save(invoice)
        );
    }


    @Transactional(readOnly = true)
    public InvoiceTotalsResponse getInvoiceTotals() {
        InvoiceTotalsRow row = invoiceRepository.getInvoiceTotals(
                InvoiceStatus.PAID,
                InvoiceStatus.OVERDUE,
                java.util.List.of(
                        InvoiceStatus.SENT,
                        InvoiceStatus.DRAFT
                )
        );

        InvoiceTotalsResponse response = new InvoiceTotalsResponse();

        response.setPaid(new InvoiceTotalsResponse.SummaryItem(
                row.getPaidCount(),
                row.getPaidAmount()
        ));

        response.setOverdue(new InvoiceTotalsResponse.SummaryItem(
                row.getOverdueCount(),
                row.getOverdueAmount()
        ));

        response.setPending(new InvoiceTotalsResponse.SummaryItem(
                row.getPendingCount(),
                row.getPendingAmount()
        ));

        response.setGrandTotal(new InvoiceTotalsResponse.SummaryItem(
                row.getGrandCount(),
                row.getGrandAmount()
        ));

        return response;
    }

}
