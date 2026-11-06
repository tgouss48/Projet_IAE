package ma.mobility.pricing.api;

public class QuoteNotFoundException extends RuntimeException {

    public QuoteNotFoundException(String quoteId) {
        super("Devis inconnu : " + quoteId);
    }
}
