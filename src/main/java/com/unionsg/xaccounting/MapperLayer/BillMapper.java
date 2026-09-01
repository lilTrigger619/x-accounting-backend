package com.unionsg.xaccounting.MapperLayer;


import com.unionsg.xaccounting.dto.bill.*;
import com.unionsg.xaccounting.entity.bill.Bill;
import com.unionsg.xaccounting.entity.bill.BillItem;
import com.unionsg.xaccounting.entity.supplier.Supplier;

import java.util.List;
import java.util.stream.Collectors;

public class BillMapper {

    public static Bill toEntity(
            CreateBillRequest request,
            Supplier supplier
    ) {

        Bill bill = new Bill();

        bill.setSupplierReference(request.getSupplierReference());
        bill.setBillDate(request.getBillDate());
        bill.setDueDate(request.getDueDate());
        bill.setSupplier(supplier);
        bill.setNotes(request.getNotes());
        bill.setTerms(request.getTerms());
        bill.setDiscountType(request.getDiscountType());
        bill.setDiscountValue(request.getDiscountValue());

        List<BillItem> items = request.getItems()
                .stream()
                .map(item -> toItemEntity(item, bill))
                .collect(Collectors.toList());

        bill.setItems(items);

        return bill;
    }


    public static void applyUpdate(
            Bill bill,
            UpdateBillRequest request,
            Supplier supplier
    ) {
        bill.setSupplierReference(request.getSupplierReference());
        bill.setBillDate(request.getBillDate());
        bill.setDueDate(request.getDueDate());
        bill.setSupplier(supplier);
        bill.setNotes(request.getNotes());
        bill.setTerms(request.getTerms());
        bill.setDiscountType(request.getDiscountType());
        bill.setDiscountValue(request.getDiscountValue());

        bill.getItems().clear();

        if (request.getItems() != null) {
            request.getItems().stream()
                    .map(item -> toItemEntity(item, bill))
                    .forEach(item -> bill.getItems().add(item));
        }
    }


    private static BillItem toItemEntity(
            BillItemRequest request,
            Bill bill
    ) {

        BillItem item = new BillItem();

        item.setDescription(request.getDescription());
        item.setQuantity(request.getQuantity());
        item.setUnitPrice(request.getUnitPrice());
        item.setTaxRate(request.getTaxRate());
        item.setBill(bill);

        return item;
    }


    public static BillResponse toResponse(Bill bill) {

        BillResponse response = new BillResponse();

        response.setId(bill.getId());
        response.setBillNumber(bill.getBillNumber());
        response.setSupplierReference(bill.getSupplierReference());
        response.setBillDate(bill.getBillDate());
        response.setDueDate(bill.getDueDate());
        response.setStatus(bill.getStatus());

        response.setSupplierId(bill.getSupplier().getId());
        response.setSupplierName(bill.getSupplier().getDisplayName());

        response.setNotes(bill.getNotes());
        response.setTerms(bill.getTerms());

        response.setDiscountType(bill.getDiscountType());
        response.setDiscountValue(bill.getDiscountValue());
        response.setDiscountAmount(bill.getDiscountAmount());

        response.setSubtotal(bill.getSubtotal());
        response.setTotalTax(bill.getTotalTax());
        response.setTotalAmount(bill.getTotalAmount());
        response.setTotalDue(bill.getTotalDue());
        response.setAmountPaid(bill.getAmountPaid());
        response.setBalance(bill.getBalance());

        response.setItems(
                bill.getItems()
                        .stream()
                        .map(BillMapper::toItemResponse)
                        .collect(Collectors.toList())
        );

        return response;
    }


    private static BillItemResponse toItemResponse(BillItem item) {

        BillItemResponse response = new BillItemResponse();

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

}
