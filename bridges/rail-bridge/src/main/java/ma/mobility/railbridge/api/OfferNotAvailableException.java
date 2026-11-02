package ma.mobility.railbridge.api;

public class OfferNotAvailableException extends RuntimeException {

    public OfferNotAvailableException(String reason) {
        super(reason);
    }
}
