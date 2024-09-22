package com.suit.checkout.models.dtos;

public record ResponsePIX(
        Integer id,
        String paymentCodeBase64,
        String paymentCode,
        String description
) {
}
