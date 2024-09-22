package com.suit.checkout.models.dtos;

import java.util.List;

public record HorizonRequestPaymentDTO(
        Integer amount,
        String paymentMethod,
        ClientRequestHorizon customer,
        List<ItemsHorizonDTO> items,
        String postbackUrl
) {
}
