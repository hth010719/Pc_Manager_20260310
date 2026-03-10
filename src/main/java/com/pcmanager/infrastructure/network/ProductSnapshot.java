package com.pcmanager.infrastructure.network;

public record ProductSnapshot(Long productId, String name, int price, int stock) {
}
