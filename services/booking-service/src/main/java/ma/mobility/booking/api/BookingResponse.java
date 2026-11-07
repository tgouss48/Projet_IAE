package ma.mobility.booking.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import ma.mobility.booking.domain.Booking;
import ma.mobility.booking.domain.BookingLeg;

// Dossier de voyage au format canonique (Booking / BookingLeg)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookingResponse(String bookingId, String customerId, String quoteId, String status,
                              String failureReason, Money totalPrice, List<Leg> legs,
                              String createdAt, String updatedAt) {

    public record Money(long amountMinor, String currency) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Leg(int legIndex, String offerId, String mode, int quantity, Money unitPrice,
                      String status, String holdId, String providerRef, String failureReason) {
    }

    static BookingResponse from(Booking b) {
        List<Leg> legs = b.getLegs().stream().map(l -> leg(l, b.getCurrency())).toList();
        return new BookingResponse(b.getId(), b.getCustomerId(), b.getQuoteId(), b.getStatus().name(),
                b.getFailureReason(), new Money(b.getTotalMinor(), b.getCurrency()), legs,
                b.getCreatedAt().toString(), b.getUpdatedAt().toString());
    }

    private static Leg leg(BookingLeg l, String currency) {
        return new Leg(l.getLegIndex(), l.getOfferId(), l.getMode(), l.getQuantity(),
                new Money(l.getUnitPriceMinor(), currency), l.getStatus().name(),
                l.getHoldId(), l.getProviderRef(), l.getFailureReason());
    }
}
