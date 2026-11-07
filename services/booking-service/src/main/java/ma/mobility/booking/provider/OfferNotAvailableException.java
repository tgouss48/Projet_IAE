package ma.mobility.booking.provider;

// refus metier de l'operateur (plus de place, maintien expire...) : inutile de rejouer
public class OfferNotAvailableException extends ProviderException {

    public OfferNotAvailableException(String message) {
        super(message);
    }
}
