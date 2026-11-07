package ma.mobility.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import ma.mobility.booking.api.BookingException;
import ma.mobility.booking.api.BookingService;
import ma.mobility.booking.domain.Booking;
import ma.mobility.booking.domain.BookingRepository;
import ma.mobility.booking.domain.BookingStatus;
import ma.mobility.booking.domain.LegStatus;
import ma.mobility.booking.domain.OutboxEvent;
import ma.mobility.booking.domain.OutboxRepository;
import ma.mobility.booking.provider.ProviderClient;
import ma.mobility.booking.quote.Quote;
import ma.mobility.booking.quote.QuoteClient;
import ma.mobility.booking.saga.SagaOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootTest
class SagaOrchestratorTest {

    static final String USER = "demo";

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        FakeProviders fakeProviders() {
            return new FakeProviders();
        }

        @Bean
        @Primary
        QuoteClient fakeQuotes() {
            Quote.Money mad = new Quote.Money(0, "MAD");
            return id -> switch (id) {
                case "Q-OK" -> Optional.of(new Quote(id, List.of(
                        new Quote.Line("TRAIN:RAIL-CAS-RAB-0730", "TRAIN", 2, new Quote.Money(8000, "MAD")),
                        new Quote.Line("FLIGHT:AIR-RBA-ORY-1200", "FLIGHT", 2, new Quote.Money(180000, "MAD")),
                        new Quote.Line("CARSHARING:VEH-PAR-001:2026-11-15", "CARSHARING", 1, new Quote.Money(45000, "MAD"))),
                        new Quote.Money(380400, "MAD"), "2999-01-01T00:00:00Z"));
                case "Q-OLD" -> Optional.of(new Quote(id, List.of(), mad, "2000-01-01T00:00:00Z"));
                default -> Optional.empty();
            };
        }
    }

    @Autowired
    FakeProviders providers;

    @Autowired
    ProviderClient providerClient;

    @Autowired
    SagaOrchestrator saga;

    @Autowired
    BookingService service;

    @Autowired
    BookingRepository bookings;

    @Autowired
    OutboxRepository outbox;

    @BeforeEach
    void reset() {
        providers.reset();
    }

    @Test
    void cheminNominal() {
        String id = service.create(USER, "Q-OK", null).getId();

        saga.run(id);

        Booking b = bookings.findById(id).orElseThrow();
        assertEquals(BookingStatus.CONFIRMED, b.getStatus());
        assertThat(b.getLegs()).allMatch(l -> l.getStatus() == LegStatus.CONFIRMED);
        assertEquals("BK-FLIGHT", b.leg(1).getProviderRef());
        assertThat(providers.calls).containsExactly(
                "hold TRAIN", "hold FLIGHT", "hold CARSHARING",
                "confirm TRAIN", "confirm FLIGHT", "confirm CARSHARING");
        assertThat(eventTypes(id)).containsExactly(
                "BOOKING_REQUESTED", "LEG_HELD", "LEG_HELD", "LEG_HELD",
                "LEG_CONFIRMED", "LEG_CONFIRMED", "LEG_CONFIRMED", "BOOKING_CONFIRMED");
    }

    @Test
    void volCompletLeTrainEstAnnule() {
        providers.refuseHold("FLIGHT");
        String id = service.create(USER, "Q-OK", null).getId();

        saga.run(id);

        Booking b = bookings.findById(id).orElseThrow();
        assertEquals(BookingStatus.CANCELLED, b.getStatus());
        assertThat(b.getFailureReason()).contains("FLIGHT");
        assertEquals(LegStatus.CANCELLED, b.leg(0).getStatus());
        assertEquals(LegStatus.FAILED, b.leg(1).getStatus());
        assertEquals(LegStatus.CANCELLED, b.leg(2).getStatus());
        // la voiture n'est jamais demandee, seul le train est annule
        assertThat(providers.calls).containsExactly("hold TRAIN", "hold FLIGHT", "cancel TRAIN HOLD-TRAIN");
        assertThat(eventTypes(id)).containsExactly(
                "BOOKING_REQUESTED", "LEG_HELD", "LEG_HOLD_FAILED", "COMPENSATION_STARTED",
                "LEG_CANCELLED", "BOOKING_CANCELLED");
    }

    @Test
    void confirmVoitureEchoueTrainEtAvionAnnulesEnOrdreInverse() {
        providers.refuseConfirm("CARSHARING");
        String id = service.create(USER, "Q-OK", null).getId();

        saga.run(id);

        assertEquals(BookingStatus.CANCELLED, bookings.findById(id).orElseThrow().getStatus());
        // le hold de la voiture est aussi libere, puis les reservations fermes
        assertThat(providers.calls).endsWith(
                "cancel CARSHARING HOLD-CARSHARING", "cancel FLIGHT BK-FLIGHT", "cancel TRAIN BK-TRAIN");
    }

    @Test
    void sagaRejoueeSansEffetDeBord() {
        String id = service.create(USER, "Q-OK", null).getId();
        saga.run(id);
        int calls = providers.calls.size();

        saga.run(id);
        saga.run(id);

        assertEquals(calls, providers.calls.size());
        assertEquals(BookingStatus.CONFIRMED, bookings.findById(id).orElseThrow().getStatus());
    }

    @Test
    void compensationIncompletePuisReprise() {
        providers.refuseHold("CARSHARING");
        providers.cancelDown("TRAIN", 3);
        String id = service.create(USER, "Q-OK", null).getId();

        saga.run(id);

        Booking b = bookings.findById(id).orElseThrow();
        assertEquals(BookingStatus.COMPENSATING, b.getStatus());
        assertEquals(LegStatus.CANCELLED, b.leg(1).getStatus());
        assertEquals(LegStatus.HELD, b.leg(0).getStatus());

        // le train revient : la reprise termine la compensation
        saga.run(id);

        b = bookings.findById(id).orElseThrow();
        assertEquals(BookingStatus.CANCELLED, b.getStatus());
        assertEquals(LegStatus.CANCELLED, b.leg(0).getStatus());
        assertThat(providers.calls.stream().filter(c -> c.startsWith("cancel FLIGHT"))).hasSize(1);
    }

    @Test
    void annulationClientDUnDossierConfirme() {
        String id = service.create(USER, "Q-OK", null).getId();
        saga.run(id);

        service.cancel(USER, id);
        service.cancel(USER, id);
        saga.run(id);

        Booking b = bookings.findById(id).orElseThrow();
        assertEquals(BookingStatus.CANCELLED, b.getStatus());
        assertEquals("annulation demandee par le client", b.getFailureReason());
        assertThat(providers.calls).endsWith(
                "cancel CARSHARING BK-CARSHARING", "cancel FLIGHT BK-FLIGHT", "cancel TRAIN BK-TRAIN");
    }

    @Test
    void idempotencyKeyRenvoieLeMemeDossier() {
        Booking first = service.create(USER, "Q-OK", "cle-1");
        Booking again = service.create(USER, "Q-OK", "cle-1");
        Booking other = service.create("alice", "Q-OK", "cle-1");

        assertEquals(first.getId(), again.getId());
        assertThat(other.getId()).isNotEqualTo(first.getId());
    }

    @Test
    void devisInconnuOuExpireEtDossierDUnAutreClient() {
        assertThrows(BookingException.class, () -> service.create(USER, "Q-INCONNU", null));
        assertThrows(BookingException.class, () -> service.create(USER, "Q-OLD", null));

        String id = service.create(USER, "Q-OK", null).getId();
        assertThrows(BookingException.class, () -> service.get("alice", id));
    }

    private List<String> eventTypes(String bookingId) {
        return outbox.findByAggregateIdOrderByIdAsc(bookingId).stream().map(OutboxEvent::getEventType).toList();
    }
}
