package ma.mobility.railbridge.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import mobility.rail.Trip;
import org.junit.jupiter.api.Test;

class TripMapperTest {

    @Test
    void tripVersOffre() {
        Trip trip = new Trip("RAIL-CAS-RAB-0730", "Casablanca", "Rabat",
                "2026-11-15T07:30:00", "2026-11-15T08:25:00", 8000, 120);

        Offer offer = TripMapper.toOffer(trip);

        assertEquals("TRAIN:RAIL-CAS-RAB-0730", offer.offerId());
        assertEquals("TRAIN", offer.mode());
        assertEquals("Casablanca", offer.origin());
        assertEquals("2026-11-15T08:25:00", offer.arrival());
        assertEquals(new Money(8000, "MAD"), offer.price());
        assertEquals(120, offer.availability());
    }

    @Test
    void offerIdVersTripId() {
        assertEquals("RAIL-CAS-RAB-0730", TripMapper.toTripId("TRAIN:RAIL-CAS-RAB-0730"));
    }

    @Test
    void offerIdAutreModeRefuse() {
        assertThrows(IllegalArgumentException.class, () -> TripMapper.toTripId("FLIGHT:AIR-RBA-ORY-1200"));
        assertThrows(IllegalArgumentException.class, () -> TripMapper.toTripId("TRAIN:"));
        assertThrows(IllegalArgumentException.class, () -> TripMapper.toTripId(null));
    }
}
