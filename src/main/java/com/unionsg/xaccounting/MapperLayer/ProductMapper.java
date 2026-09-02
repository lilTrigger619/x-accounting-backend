package com.unionsg.xaccounting.MapperLayer;

import com.unionsg.xaccounting.dto.product.ProductResponse;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.TaxCategory;
import com.unionsg.xaccounting.entity.product.Product;

public class ProductMapper {

    public static ProductResponse toResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setItemType(product.getItemType());
        response.setCategory(product.getCategory());
        response.setCostGroup(product.getCostGroup());
        response.setImageFileId(product.getImageFileId());
        if (product.getImageFileId() != null) {
            response.setImageUrl("/api/files/" + product.getImageFileId() + "/download");
        }
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setCreatedAt(product.getCreatedAt());

        AccountEntity incomeAccount = product.getIncomeAccount();
        if (incomeAccount != null) {
            response.setIncomeAccountId(incomeAccount.getId());
            response.setIncomeAccountCode(incomeAccount.getAccountId());
            response.setIncomeAccountName(incomeAccount.getAccountName());
        }

        TaxCategory taxCategory = product.getTaxCategory();
        if (taxCategory != null) {
            response.setTaxCategoryId(taxCategory.getId());
            response.setTaxCategoryName(taxCategory.getName());
            response.setTaxCategoryType(taxCategory.getType());
            response.setTaxRate(taxCategory.getRate());
        }

        return response;
    }
}
