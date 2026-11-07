package ma.mobility.booking.saga;

import com.fasterxml.jackson.annotation.JsonInclude;

// Evenement publie sur le topic booking-events (cle = bookingId)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookingEvent(
        String eventId,
        String type,
        String bookingId,
        String customerId,
        String bookingStatus,
        Integer legIndex,
        String mode,
        String offerId,
        String reference,
        String reason,
        Long totalMinor,
        String currency,
        String occurredAt) {

    public static final String BOOKING_REQUESTED = "BOOKING_REQUESTED";
    public static final String LEG_HELD = "LEG_HELD";
    public static final String LEG_HOLD_FAILED = "LEG_HOLD_FAILED";
    public static final String LEG_CONFIRMED = "LEG_CONFIRMED";
    public static final String LEG_CONFIRM_FAILED = "LEG_CONFIRM_FAILED";
    public static final String BOOKING_CONFIRMED = "BOOKING_CONFIRMED";
    public static final String COMPENSATION_STARTED = "COMPENSATION_STARTED";
    public static final String LEG_CANCELLED = "LEG_CANCELLED";
    public static final String BOOKING_CANCELLED = "BOOKING_CANCELLED";
    public static final String BOOKING_CANCEL_REQUESTED = "BOOKING_CANCEL_REQUESTED";
}
