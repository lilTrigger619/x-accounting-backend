package com.unionsg.xaccounting.service.product;

import com.unionsg.xaccounting.MapperLayer.ProductMapper;
import com.unionsg.xaccounting.dto.FileUploadRequestDto;
import com.unionsg.xaccounting.dto.product.CreateProductRequest;
import com.unionsg.xaccounting.dto.product.ProductResponse;
import com.unionsg.xaccounting.dto.product.UpdateProductRequest;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.TaxCategory;
import com.unionsg.xaccounting.entity.product.Product;
import com.unionsg.xaccounting.enums.EntityType;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.TaxCategoryRepository;
import com.unionsg.xaccounting.repository.product.ProductRepository;
import com.unionsg.xaccounting.security.util.SecurityUtils;
import com.unionsg.xaccounting.service.FileService.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final AccountRepository accountRepository;
    private final TaxCategoryRepository taxCategoryRepository;
    private final FileService fileService;

    @Transactional
    public ProductResponse createProduct(MultipartFile image, CreateProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .itemType(request.getItemType())
                .category(request.getCategory())
                .costGroup(request.getCostGroup())
                .description(request.getDescription())
                .price(request.getPrice())
                .incomeAccount(resolveAccount(request.getIncomeAccountId()))
                .taxCategory(resolveTaxCategory(request.getTaxCategoryId()))
                .build();

        Product saved = productRepository.save(product);

        if (image != null && !image.isEmpty()) {
            saved.setImageFileId(uploadImage(saved.getId(), image));
            saved = productRepository.save(saved);
        }

        return ProductMapper.toResponse(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, MultipartFile image, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        product.setName(request.getName());
        product.setItemType(request.getItemType());
        product.setCategory(request.getCategory());
        product.setCostGroup(request.getCostGroup());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setIncomeAccount(resolveAccount(request.getIncomeAccountId()));
        product.setTaxCategory(resolveTaxCategory(request.getTaxCategoryId()));

        if (image != null && !image.isEmpty()) {
            product.setImageFileId(uploadImage(product.getId(), image));
        }

        Product saved = productRepository.save(product);
        return ProductMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return ProductMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(String search, Pageable pageable) {
        Page<Product> page = (search != null && !search.isBlank())
                ? productRepository.findByDeletedFalseAndNameContainingIgnoreCase(search.trim(), pageable)
                : productRepository.findByDeletedFalse(pageable);
        return page.map(ProductMapper::toResponse);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        product.setDeleted(true);
        productRepository.save(product);
    }

    private AccountEntity resolveAccount(Long accountId) {
        if (accountId == null) return null;
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + accountId));
    }

    private TaxCategory resolveTaxCategory(Long taxCategoryId) {
        if (taxCategoryId == null) return null;
        return taxCategoryRepository.findById(taxCategoryId)
                .orElseThrow(() -> new RuntimeException("Tax category not found with id: " + taxCategoryId));
    }

    private String uploadImage(Long productId, MultipartFile image) {
        FileUploadRequestDto uploadRequest = new FileUploadRequestDto();
        uploadRequest.setEntityType(EntityType.PRODUCT);
        uploadRequest.setEntityId(productId.toString());
        uploadRequest.setDescription("Product image");
        UUID currentUserId = SecurityUtils.getCurrentUser().getId();
        uploadRequest.setUploadedBy(currentUserId);
        return fileService.uploadFile(new MultipartFile[]{image}, uploadRequest)
                .get(0)
                .getId();
    }
}
