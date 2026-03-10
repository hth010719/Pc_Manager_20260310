package com.pcmanager.application.order;

import com.pcmanager.common.exception.BusinessException;
import com.pcmanager.domain.product.Product;
import com.pcmanager.domain.product.SaleStatus;
import com.pcmanager.infrastructure.persistence.memory.MemoryStore;

import java.util.List;

public class ProductService {
    private final MemoryStore store;

    public ProductService(MemoryStore store) {
        this.store = store;
    }

    public List<Product> getOnSaleProducts() {
        return store.getProducts().stream()
                .filter(product -> product.getSaleStatus() == SaleStatus.ON_SALE)
                .toList();
    }

    public Product getById(Long productId) {
        return store.getProducts().stream()
                .filter(product -> product.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("존재하지 않는 상품입니다. productId=" + productId));
    }
}
