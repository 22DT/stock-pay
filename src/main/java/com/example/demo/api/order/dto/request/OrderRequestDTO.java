package com.example.demo.api.order.dto.request;


import java.util.List;

public record OrderRequestDTO(
        String merchantOrderId,
        String amount,
        List<ItemQuantity> itemQuantities
) {
    public record ItemQuantity(
            Long itemId,
            Long quantity
    ) {}
}