package ma.mobility.pricing.api;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record QuoteRequest(@NotEmpty List<@Valid QuoteLegRequest> legs) {
}
