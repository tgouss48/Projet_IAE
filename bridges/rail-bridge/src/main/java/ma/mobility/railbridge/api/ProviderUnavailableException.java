package ma.mobility.railbridge.api;

public class ProviderUnavailableException extends RuntimeException {

    public ProviderUnavailableException(String reason) {
        super(reason);
    }
}
