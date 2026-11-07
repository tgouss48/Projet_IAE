package ma.mobility.booking.provider;

public abstract class ProviderException extends RuntimeException {

    protected ProviderException(String message) {
        super(message);
    }
}
