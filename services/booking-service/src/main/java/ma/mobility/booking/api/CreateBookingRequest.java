package ma.mobility.booking.api;

import jakarta.validation.constraints.NotBlank;

public record CreateBookingRequest(@NotBlank String quoteId) {
}
