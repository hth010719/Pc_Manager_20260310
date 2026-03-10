package com.pcmanager.domain.order;

public class OrderItem {
    private final Long productId;
    private final String productName;
    private final int quantity;
    private final int unitPrice;

    public OrderItem(Long productId, String productName, int quantity, int unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getUnitPrice() {
        return unitPrice;
    }

    public int getLinePrice() {
        return unitPrice * quantity;
    }
}
