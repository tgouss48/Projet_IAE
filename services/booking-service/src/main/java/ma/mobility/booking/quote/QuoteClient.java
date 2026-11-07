package ma.mobility.booking.quote;

import java.util.Optional;

public interface QuoteClient {

    Optional<Quote> find(String quoteId);
}
