package ma.mobility.carsharing.api;

public class OfferNotAvailableException extends RuntimeException {

    public OfferNotAvailableException(String reason) {
        super(reason);
    }
}
