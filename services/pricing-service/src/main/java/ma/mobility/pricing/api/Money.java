package ma.mobility.pricing.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record Money(@Min(0) long amountMinor, @NotBlank String currency) {
}
