package ma.mobility.carsharing.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record HoldRequest(@NotBlank String offerId, @Min(1) int quantity) {
}
