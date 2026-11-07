package ma.mobility.booking.quote;

import java.util.List;

// Devis renvoye par pricing-service
public record Quote(String quoteId, List<Line> lines, Money total, String expiresAt) {

    public record Line(String offerId, String mode, int quantity, Money unitPrice) {
    }

    public record Money(long amountMinor, String currency) {
    }
}
