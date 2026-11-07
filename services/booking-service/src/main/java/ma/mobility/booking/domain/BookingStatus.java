package ma.mobility.booking.domain;

public enum BookingStatus {
    PENDING, HOLDING, CONFIRMING, CONFIRMED, COMPENSATING, CANCELLED;

    public boolean isTerminal() {
        return this == CONFIRMED || this == CANCELLED;
    }
}
