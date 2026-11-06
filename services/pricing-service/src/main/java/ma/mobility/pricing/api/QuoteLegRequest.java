package ma.mobility.pricing.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QuoteLegRequest(@NotBlank String offerId,
                              @NotBlank String mode,
                              @NotNull @Valid Money unitPrice,
                              @Min(1) int quantity) {
}
