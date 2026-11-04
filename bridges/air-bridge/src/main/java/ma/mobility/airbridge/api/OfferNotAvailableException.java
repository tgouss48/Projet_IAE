package ma.mobility.airbridge.api;

public class OfferNotAvailableException extends RuntimeException {

    public OfferNotAvailableException(String reason) {
        super(reason);
    }
}
