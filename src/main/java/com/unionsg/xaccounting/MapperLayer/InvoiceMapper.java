package com.unionsg.xaccounting.MapperLayer;


import com.unionsg.xaccounting.dto.invoice.*;
import com.unionsg.xaccounting.embeddables.InvoiceBillingInfo;
import com.unionsg.xaccounting.entity.customer.Customer;
import com.unionsg.xaccounting.entity.customer.PaymentTerms;
import com.unionsg.xaccounting.entity.invoice.Invoice;
import com.unionsg.xaccounting.entity.invoice.InvoiceItem;

import java.util.List;
import java.util.stream.Collectors;

public class InvoiceMapper {

    public static Invoice toEntity(
            CreateInvoiceRequest request,
            Customer customer,
            PaymentTerms paymentTerms
    ) {

        Invoice invoice = new Invoice();

        invoice.setInvoiceNumber(request.getInvoiceNumber());
        invoice.setReference(request.getReference());
        invoice.setIssueDate(request.getIssueDate());
        invoice.setDueDate(request.getDueDate());
        invoice.setCustomer(customer);
        invoice.setPaymentTerms(paymentTerms);
        invoice.setNotes(request.getNotes());
        invoice.setTerms(request.getTerms());
        invoice.setDiscountType(request.getDiscountType());
        invoice.setDiscountValue(request.getDiscountValue());

        invoice.setBillingInfo(mapBillingInfo(request));

        List<InvoiceItem> items = request.getItems()
                .stream()
                .map(item -> toItemEntity(item, invoice))
                .collect(Collectors.toList());

        invoice.setItems(items);

        return invoice;
    }


    private static InvoiceItem toItemEntity(
            InvoiceItemRequest request,
            Invoice invoice
    ) {

        InvoiceItem item = new InvoiceItem();

        item.setDescription(request.getDescription());
        item.setQuantity(request.getQuantity());
        item.setUnitPrice(request.getUnitPrice());
        item.setTaxRate(request.getTaxRate());
        item.setInvoice(invoice);

        return item;
    }


    private static InvoiceBillingInfo mapBillingInfo(
            CreateInvoiceRequest request
    ) {

        if (request.getBillingInfo() == null) {
            return null;
        }

        InvoiceBillingInfo billing = new InvoiceBillingInfo();

        billing.setBillingName(request.getBillingInfo().getBillingName());
        billing.setBillingEmail(request.getBillingInfo().getBillingEmail());
        billing.setBillingPhone(request.getBillingInfo().getBillingPhone());
        billing.setAddressLine1(request.getBillingInfo().getAddressLine1());
        billing.setAddressLine2(request.getBillingInfo().getAddressLine2());
        billing.setCity(request.getBillingInfo().getCity());
        billing.setState(request.getBillingInfo().getState());
        billing.setPostalCode(request.getBillingInfo().getPostalCode());
        billing.setCountry(request.getBillingInfo().getCountry());

        return billing;
    }


    public static InvoiceResponse toResponse(Invoice invoice) {

        InvoiceResponse response = new InvoiceResponse();

        response.setId(invoice.getId());
        response.setInvoiceNumber(invoice.getInvoiceNumber());
        response.setReference(invoice.getReference());
        response.setIssueDate(invoice.getIssueDate());
        response.setDueDate(invoice.getDueDate());
        response.setStatus(invoice.getStatus());

        response.setCustomerId(invoice.getCustomer().getId());
        response.setCustomerName(invoice.getCustomer().getFirstName() + " "+ invoice.getCustomer().getLastName());

        response.setPaymentTermsId(
                invoice.getPaymentTerms() != null
                        ? invoice.getPaymentTerms().getId()
                        : null
        );

        response.setNotes(invoice.getNotes());
        response.setTerms(invoice.getTerms());

        response.setSubtotal(invoice.getSubtotal());
        response.setTotalTax(invoice.getTotalTax());
        response.setTotalAmount(invoice.getTotalAmount());
        response.setTotalDue(invoice.getTotalDue());

        response.setItems(
                invoice.getItems()
                        .stream()
                        .map(InvoiceMapper::toItemResponse)
                        .collect(Collectors.toList())
        );

        response.setBillingInfo(mapBillingResponse(invoice));

        return response;
    }


    private static InvoiceItemResponse toItemResponse(InvoiceItem item) {

        InvoiceItemResponse response = new InvoiceItemResponse();

        response.setId(item.getId());
        response.setDescription(item.getDescription());
        response.setQuantity(item.getQuantity());
        response.setUnitPrice(item.getUnitPrice());
        response.setTaxRate(item.getTaxRate());
        response.setLineSubtotal(item.getLineSubtotal());
        response.setLineTax(item.getLineTax());
        response.setLineTotal(item.getLineTotal());

        return response;
    }


    private static InvoiceBillingInfoResponse mapBillingResponse(Invoice invoice) {

        if (invoice.getBillingInfo() == null) {
            return null;
        }

        InvoiceBillingInfoResponse response =
                new InvoiceBillingInfoResponse();

        response.setBillingName(invoice.getBillingInfo().getBillingName());
        response.setBillingEmail(invoice.getBillingInfo().getBillingEmail());
        response.setBillingPhone(invoice.getBillingInfo().getBillingPhone());
        response.setAddressLine1(invoice.getBillingInfo().getAddressLine1());
        response.setAddressLine2(invoice.getBillingInfo().getAddressLine2());
        response.setCity(invoice.getBillingInfo().getCity());
        response.setState(invoice.getBillingInfo().getState());
        response.setPostalCode(invoice.getBillingInfo().getPostalCode());
        response.setCountry(invoice.getBillingInfo().getCountry());

        return response;
    }

}