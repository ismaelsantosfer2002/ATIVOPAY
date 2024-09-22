package com.suit.checkout.models.dtos;

public record AddressRequestDTO(
        String codIbge,
        String street,
        String number,
        String complement,
        String zipCode,
        String neighborhood,
        String city,
        String state
) {
}
