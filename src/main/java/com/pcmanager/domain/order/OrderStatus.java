package com.pcmanager.domain.order;

public enum OrderStatus {
    REQUESTED,
    ACCEPTED,
    PREPARING,
    DELIVERING,
    COMPLETED,
    CANCELED;

    public OrderStatus next() {
        return switch (this) {
            case REQUESTED -> ACCEPTED;
            case ACCEPTED -> PREPARING;
            case PREPARING -> DELIVERING;
            case DELIVERING -> COMPLETED;
            case COMPLETED, CANCELED -> this;
        };
    }
}
