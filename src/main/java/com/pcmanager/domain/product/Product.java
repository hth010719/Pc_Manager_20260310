package com.pcmanager.domain.product;

/**
 * 주문 가능한 상품 1개를 표현하는 도메인 객체다.
 *
 * 상품명/가격/재고/판매 상태를 함께 관리하고,
 * 주문 서비스는 이 객체를 통해 재고 차감과 품절 전환을 처리한다.
 */
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

    /**
     * 주문 수량만큼 재고를 차감한다.
     * 음수 재고는 허용하지 않고, 0개가 되면 자동으로 품절 상태로 바꾼다.
     */
    public void deductStock(int amount) {
        stock = Math.max(0, stock - amount);
        if (stock == 0) {
            saleStatus = SaleStatus.SOLD_OUT;
        }
    }
}
