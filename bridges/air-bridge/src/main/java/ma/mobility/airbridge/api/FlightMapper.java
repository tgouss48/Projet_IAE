package ma.mobility.airbridge.api;

import ma.mobility.airbridge.soap.gen.Flight;

// Traduction SOAP <-> modele canonique
public final class FlightMapper {

    public static final String MODE = "FLIGHT";
    public static final String PREFIX = MODE + ":";
    static final String PROVIDER = "air-partner";
    static final String CURRENCY = "MAD";

    private FlightMapper() {
    }

    public static Offer toOffer(Flight flight) {
        return new Offer(
                PREFIX + flight.getFlightId(),
                MODE,
                PROVIDER,
                flight.getOrigin(),
                flight.getDestination(),
                flight.getDeparture(),
                flight.getArrival(),
                new Money(flight.getPriceMinor(), CURRENCY),
                flight.getSeatsAvailable());
    }

    // "FLIGHT:AIR-RBA-ORY-1200" -> "AIR-RBA-ORY-1200"
    public static String toFlightId(String offerId) {
        if (offerId == null || !offerId.startsWith(PREFIX) || offerId.length() == PREFIX.length()) {
            throw new IllegalArgumentException("offerId invalide pour l'avion : " + offerId);
        }
        return offerId.substring(PREFIX.length());
    }
}
