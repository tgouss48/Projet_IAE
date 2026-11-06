package ma.mobility.pricing.api;

import java.util.List;

public record QuoteResponse(String quoteId,
                            List<Line> lines,
                            Money subtotal,
                            int discountPercent,
                            Money discount,
                            Money serviceFee,
                            Money total,
                            String expiresAt) {

    public record Line(String offerId, String mode, int quantity, Money unitPrice, Money lineTotal) {
    }
}
