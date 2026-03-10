package com.pcmanager.domain.product;

public class Product {
    private final Long productId;
    private final Long categoryId;
    private final String name;
    private final int price;
    private int stock;
    private SaleStatus saleStatus;

    public Product(Long productId, Long categoryId, String name, int price, int stock, SaleStatus saleStatus) {
        this.productId = productId;
        this.categoryId = categoryId;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.saleStatus = saleStatus;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }

    public SaleStatus getSaleStatus() {
        return saleStatus;
    }

    public void deductStock(int amount) {
        stock = Math.max(0, stock - amount);
        if (stock == 0) {
            saleStatus = SaleStatus.SOLD_OUT;
        }
    }
}
