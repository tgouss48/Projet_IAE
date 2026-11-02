package ma.mobility.railbridge.api;

import mobility.rail.Trip;

// Traduction CORBA <-> modele canonique
public final class TripMapper {

    public static final String MODE = "TRAIN";
    public static final String PREFIX = MODE + ":";
    static final String PROVIDER = "rail-operator";
    static final String CURRENCY = "MAD";

    private TripMapper() {
    }

    public static Offer toOffer(Trip trip) {
        return new Offer(
                PREFIX + trip.tripId,
                MODE,
                PROVIDER,
                trip.origin,
                trip.destination,
                trip.departure,
                trip.arrival,
                new Money(trip.priceMinor, CURRENCY),
                trip.seatsAvailable);
    }

    // "TRAIN:RAIL-CAS-RAB-0730" -> "RAIL-CAS-RAB-0730"
    public static String toTripId(String offerId) {
        if (offerId == null || !offerId.startsWith(PREFIX) || offerId.length() == PREFIX.length()) {
            throw new IllegalArgumentException("offerId invalide pour le train : " + offerId);
        }
        return offerId.substring(PREFIX.length());
    }
}
