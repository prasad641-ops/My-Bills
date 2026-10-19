package com.example.billing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/** Everything the frontend sends. Nothing is stored on the server. */
public record BillRequest(
        String invoiceNo,                                   // optional, generated if blank
        @NotNull @Valid Shop shop,
        @NotNull @Valid Customer customer,
        @NotEmpty @Valid List<Item> items,
        @DecimalMin("0") @DecimalMax("100") BigDecimal gstPercent,
        @Valid Discount discount                            // optional
) {
    public record Shop(
            @NotBlank String name,
            @NotBlank String gstin,
            @NotBlank String address,
            @NotBlank String phones) {}

    public record Customer(
            @NotBlank String name,
            @NotBlank @Pattern(regexp = "\\d{10}", message = "must be a 10 digit number") String mobile,
            String address) {}                              // optional

    public record Item(
            @NotBlank String name,
            @NotNull @DecimalMin("0.001") BigDecimal qty,
            @NotNull @DecimalMin("0") BigDecimal price) {}

    /** type = PERCENT or FLAT */
    public record Discount(String type, BigDecimal value) {}
}
