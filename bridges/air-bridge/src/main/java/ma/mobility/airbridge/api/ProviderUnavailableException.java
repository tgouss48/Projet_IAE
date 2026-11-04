package ma.mobility.airbridge.api;

public class ProviderUnavailableException extends RuntimeException {

    public ProviderUnavailableException(String reason) {
        super(reason);
    }
}
