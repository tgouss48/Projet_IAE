package ma.mobility.carsharing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import ma.mobility.carsharing.api.CarsharingService;
import ma.mobility.carsharing.api.Offer;
import ma.mobility.carsharing.api.OfferNotAvailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CarsharingServiceTest {

    static final LocalDate DAY = LocalDate.of(2026, 11, 15);
    static final String OFFER = "CARSHARING:VEH-PAR-001:2026-11-15";

    // horloge qu'on peut avancer pour tester l'expiration des holds
    static class TestClock extends Clock {
        Instant now = Instant.parse("2026-11-01T10:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        TestClock testClock() {
            return new TestClock();
        }
    }

    @Autowired
    CarsharingService service;

    @Autowired
    TestClock clock;

    @Test
    void rechercheParVille() {
        List<Offer> offers = service.search("paris", DAY);

        assertEquals(3, offers.size());
        Offer first = offers.get(0);
        assertEquals(OFFER, first.offerId());
        assertEquals("CARSHARING", first.mode());
        assertEquals("2026-11-15T00:00:00", first.departure());
        assertEquals("2026-11-16T00:00:00", first.arrival());
        assertEquals(45000, first.price().amountMinor());
    }

    @Test
    void vehiculeEnHoldNestPlusPropose() {
        service.hold(OFFER, 1);

        List<Offer> offers = service.search("Paris", DAY);
        assertEquals(2, offers.size());
        assertFalse(offers.stream().anyMatch(o -> o.offerId().equals(OFFER)));
        // autre jour : toujours libre
        assertEquals(3, service.search("Paris", DAY.plusDays(1)).size());
    }

    @Test
    void doubleHoldRefuse() {
        service.hold(OFFER, 1);
        assertThrows(OfferNotAvailableException.class, () -> service.hold(OFFER, 1));
    }

    @Test
    void confirmPuisConfirmRejoue() {
        String holdId = service.hold(OFFER, 1);

        String ref = service.confirm(holdId);
        assertTrue(ref.startsWith("BK-CAR-"));
        assertEquals(ref, service.confirm(holdId));
    }

    @Test
    void cancelIdempotentEtLibereLeVehicule() {
        String ref = service.confirm(service.hold(OFFER, 1));

        service.cancel(ref);
        service.cancel(ref);
        service.cancel("REFERENCE-INCONNUE");

        assertEquals(3, service.search("Paris", DAY).size());
    }

    @Test
    void cancelDuHoldEmpecheLeConfirm() {
        String holdId = service.hold(OFFER, 1);
        service.cancel(holdId);

        assertThrows(OfferNotAvailableException.class, () -> service.confirm(holdId));
    }

    @Test
    void holdExpireApres10Minutes() {
        String holdId = service.hold(OFFER, 1);
        clock.now = clock.now.plus(Duration.ofMinutes(11));

        assertThrows(OfferNotAvailableException.class, () -> service.confirm(holdId));
        // le vehicule est de nouveau disponible
        assertEquals(3, service.search("Paris", DAY).size());
        service.hold(OFFER, 1);
    }

    @Test
    void requetesInvalides() {
        assertThrows(IllegalArgumentException.class, () -> service.hold(OFFER, 2));
        assertThrows(IllegalArgumentException.class, () -> service.hold("TRAIN:RAIL-CAS-RAB-0730", 1));
        assertThrows(IllegalArgumentException.class, () -> service.hold("CARSHARING:VEH-PAR-001", 1));
        assertThrows(OfferNotAvailableException.class, () -> service.hold("CARSHARING:VEH-XXX:2026-11-15", 1));
    }
}
