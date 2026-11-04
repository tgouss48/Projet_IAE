package ma.mobility.airbridge.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ma.mobility.airbridge.soap.gen.Flight;
import org.junit.jupiter.api.Test;

class FlightMapperTest {

    @Test
    void volVersOffre() {
        Flight flight = new Flight();
        flight.setFlightId("AIR-RBA-ORY-1200");
        flight.setOrigin("Rabat");
        flight.setDestination("Paris");
        flight.setDeparture("2026-11-15T12:00:00");
        flight.setArrival("2026-11-15T15:10:00");
        flight.setPriceMinor(180000);
        flight.setSeatsAvailable(40);

        Offer offer = FlightMapper.toOffer(flight);

        assertEquals("FLIGHT:AIR-RBA-ORY-1200", offer.offerId());
        assertEquals("FLIGHT", offer.mode());
        assertEquals("Paris", offer.destination());
        assertEquals(new Money(180000, "MAD"), offer.price());
        assertEquals(40, offer.availability());
    }

    @Test
    void offerIdVersFlightId() {
        assertEquals("AIR-RBA-ORY-1200", FlightMapper.toFlightId("FLIGHT:AIR-RBA-ORY-1200"));
    }

    @Test
    void offerIdAutreModeRefuse() {
        assertThrows(IllegalArgumentException.class, () -> FlightMapper.toFlightId("TRAIN:RAIL-CAS-RAB-0730"));
        assertThrows(IllegalArgumentException.class, () -> FlightMapper.toFlightId("FLIGHT:"));
        assertThrows(IllegalArgumentException.class, () -> FlightMapper.toFlightId(null));
    }
}
