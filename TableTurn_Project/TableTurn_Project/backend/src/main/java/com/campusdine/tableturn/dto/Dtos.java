package com.campusdine.tableturn.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Request payloads with Bean Validation rules. */
public final class Dtos {

    private Dtos() { }

    public record TableRequest(
            @NotNull(message = "tableNumber is required") @Positive(message = "tableNumber must be positive") Integer tableNumber,
            @NotNull(message = "capacity is required") @Positive(message = "capacity must be positive") Integer capacity) { }

    public record ReservationRequest(
            @NotNull(message = "tableId is required") Long tableId,
            @NotBlank(message = "customerName is required") String customerName,
            @NotBlank(message = "phone is required")
            @Pattern(regexp = "\\d{10,15}", message = "phone must be 10-15 digits") String phone,
            @NotNull(message = "partySize is required") @Positive(message = "partySize must be positive") Integer partySize,
            @NotNull(message = "date is required (yyyy-MM-dd)") LocalDate date,
            @NotNull(message = "startTime is required (HH:mm)") LocalTime startTime,
            @Min(value = 30, message = "durationMinutes must be at least 30")
            @Max(value = 240, message = "durationMinutes must be at most 240") Integer durationMinutes) { }

    public record WalkInRequest(
            @NotNull(message = "tableId is required") Long tableId,
            @NotBlank(message = "customerName is required") String customerName,
            @NotNull(message = "partySize is required") @Positive(message = "partySize must be positive") Integer partySize) { }

    public record ItemRequest(
            @NotBlank(message = "itemName is required") String itemName,
            @NotNull(message = "quantity is required") @Positive(message = "quantity must be positive") Integer quantity,
            @NotNull(message = "unitPrice is required") @Positive(message = "unitPrice must be positive") BigDecimal unitPrice) { }

    public record OrderRequest(
            @NotNull(message = "tableId is required") Long tableId,
            @NotEmpty(message = "an order needs at least one item") List<@Valid ItemRequest> items) { }
}
