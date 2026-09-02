package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.product.CreateProductRequest;
import com.unionsg.xaccounting.dto.product.ProductResponse;
import com.unionsg.xaccounting.dto.product.UpdateProductRequest;
import com.unionsg.xaccounting.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> create(
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart
            CreateProductRequest request
    ) {
        return ResponseEntity.ok(productService.createProduct(image, request));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long id,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart
            UpdateProductRequest request
    ) {
        return ResponseEntity.ok(productService.updateProduct(id, image, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> list(
            @RequestParam(required = false) String search,
            Pageable pageable
    ) {
        return ResponseEntity.ok(productService.getProducts(search, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
