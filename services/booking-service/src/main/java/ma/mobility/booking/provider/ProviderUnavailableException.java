package ma.mobility.booking.provider;

// operateur injoignable ou timeout : l'appel peut etre rejoue s'il est idempotent
public class ProviderUnavailableException extends ProviderException {

    public ProviderUnavailableException(String message) {
        super(message);
    }
}
