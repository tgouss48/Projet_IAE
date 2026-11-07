package ma.mobility.booking.saga;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import ma.mobility.booking.domain.Booking;
import ma.mobility.booking.domain.BookingLeg;
import ma.mobility.booking.domain.BookingRepository;
import ma.mobility.booking.domain.BookingStatus;
import ma.mobility.booking.domain.LegStatus;
import ma.mobility.booking.provider.ProviderClient;
import ma.mobility.booking.provider.ProviderException;
import ma.mobility.booking.provider.ProviderUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

// Saga : hold de chaque segment, puis confirm ; un echec declenche la compensation.
// Chaque etape est sauvegardee (avec son evenement) dans sa propre transaction,
// donc run() peut etre rappele apres un crash et reprend la ou il en etait.
@Component
public class SagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SagaOrchestrator.class);

    private final BookingRepository bookings;
    private final ProviderClient providers;
    private final BookingEvents events;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final Duration holdWindow;
    private final int cancelAttempts;

    public SagaOrchestrator(BookingRepository bookings, ProviderClient providers, BookingEvents events,
                            TransactionTemplate tx, Clock clock,
                            @Value("${booking.saga.hold-window}") Duration holdWindow,
                            @Value("${booking.saga.cancel-attempts}") int cancelAttempts) {
        this.bookings = bookings;
        this.providers = providers;
        this.events = events;
        this.tx = tx;
        this.clock = clock;
        this.holdWindow = holdWindow;
        this.cancelAttempts = cancelAttempts;
    }

    public void run(String bookingId) {
        Booking booking = load(bookingId).orElse(null);
        if (booking == null || booking.getStatus().isTerminal()) {
            return;
        }
        log.info("saga {} : reprise a l'etat {}", bookingId, booking.getStatus());

        if (booking.getStatus() == BookingStatus.PENDING || booking.getStatus() == BookingStatus.HOLDING) {
            update(bookingId, b -> b.moveTo(BookingStatus.HOLDING, clock.instant()));
            if (holdAll(bookingId)) {
                update(bookingId, b -> b.moveTo(BookingStatus.CONFIRMING, clock.instant()));
            }
        }
        if (status(bookingId) == BookingStatus.CONFIRMING && confirmAll(bookingId)) {
            update(bookingId, b -> {
                b.moveTo(BookingStatus.CONFIRMED, clock.instant());
                events.record(BookingEvent.BOOKING_CONFIRMED, b);
            });
            log.info("saga {} : CONFIRMED", bookingId);
            return;
        }
        if (status(bookingId) == BookingStatus.COMPENSATING) {
            compensate(bookingId);
        }
    }

    // Phase 1 : renvoie false si un hold a echoue (le dossier passe alors en COMPENSATING)
    private boolean holdAll(String bookingId) {
        for (BookingLeg leg : load(bookingId).orElseThrow().getLegs()) {
            if (leg.getStatus() != LegStatus.PENDING) {
                continue;
            }
            int i = leg.getLegIndex();
            try {
                // un hold n'est pas rejoue : un hold en double bloquerait des places pour rien
                String holdId = providers.hold(leg.getMode(), leg.getOfferId(), leg.getQuantity());
                update(bookingId, b -> {
                    b.leg(i).held(holdId, clock.instant());
                    events.record(BookingEvent.LEG_HELD, b, b.leg(i), holdId, null);
                });
            } catch (ProviderException e) {
                log.warn("saga {} : hold {} echoue : {}", bookingId, leg.getMode(), e.getMessage());
                update(bookingId, b -> {
                    b.leg(i).failed(e.getMessage());
                    events.record(BookingEvent.LEG_HOLD_FAILED, b, b.leg(i), null, e.getMessage());
                    b.startCompensation("hold " + leg.getMode() + " refuse : " + e.getMessage(), clock.instant());
                    events.record(BookingEvent.COMPENSATION_STARTED, b);
                });
                return false;
            }
        }
        return true;
    }

    // Phase 2 : renvoie false si un confirm a echoue (le dossier passe alors en COMPENSATING)
    private boolean confirmAll(String bookingId) {
        Booking booking = load(bookingId).orElseThrow();
        Instant firstHold = booking.getLegs().stream().map(BookingLeg::getHeldAt)
                .filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(clock.instant());
        if (clock.instant().isAfter(firstHold.plus(holdWindow))) {
            update(bookingId, b -> {
                b.startCompensation("maintiens trop anciens, ils ont pu expirer chez l'operateur", clock.instant());
                events.record(BookingEvent.COMPENSATION_STARTED, b);
            });
            return false;
        }

        for (BookingLeg leg : booking.getLegs()) {
            if (leg.getStatus() != LegStatus.HELD) {
                continue;
            }
            int i = leg.getLegIndex();
            try {
                String ref = providers.confirm(leg.getMode(), leg.getHoldId());
                update(bookingId, b -> {
                    b.leg(i).confirmed(ref);
                    events.record(BookingEvent.LEG_CONFIRMED, b, b.leg(i), ref, null);
                });
            } catch (ProviderException e) {
                log.warn("saga {} : confirm {} echoue : {}", bookingId, leg.getMode(), e.getMessage());
                update(bookingId, b -> {
                    b.leg(i).failed(e.getMessage());
                    events.record(BookingEvent.LEG_CONFIRM_FAILED, b, b.leg(i), null, e.getMessage());
                    b.startCompensation("confirm " + leg.getMode() + " refuse : " + e.getMessage(), clock.instant());
                    events.record(BookingEvent.COMPENSATION_STARTED, b);
                });
                return false;
            }
        }
        return true;
    }

    // Annule en ordre inverse tout ce qui a ete engage. cancel est idempotent chez tous
    // les operateurs, on peut donc le rejouer sans risque.
    private void compensate(String bookingId) {
        boolean done = true;
        List<BookingLeg> legs = new ArrayList<>(load(bookingId).orElseThrow().getLegs());
        Collections.reverse(legs);
        for (BookingLeg leg : legs) {
            int i = leg.getLegIndex();
            if (!leg.needsRelease()) {
                if (leg.getReleasedAt() == null) {
                    update(bookingId, b -> b.leg(i).released(clock.instant()));
                }
                continue;
            }
            String ref = leg.cancelReference();
            if (cancelWithRetry(bookingId, leg.getMode(), ref)) {
                update(bookingId, b -> {
                    b.leg(i).released(clock.instant());
                    events.record(BookingEvent.LEG_CANCELLED, b, b.leg(i), ref, null);
                });
            } else {
                done = false;
            }
        }
        if (done) {
            update(bookingId, b -> {
                b.moveTo(BookingStatus.CANCELLED, clock.instant());
                events.record(BookingEvent.BOOKING_CANCELLED, b);
            });
            log.info("saga {} : CANCELLED ({})", bookingId, load(bookingId).orElseThrow().getFailureReason());
        } else {
            // reste en COMPENSATING, CompensationRetryJob reessaiera
            log.warn("saga {} : compensation incomplete, nouvel essai plus tard", bookingId);
        }
    }

    private boolean cancelWithRetry(String bookingId, String mode, String ref) {
        for (int attempt = 1; attempt <= cancelAttempts; attempt++) {
            try {
                providers.cancel(mode, ref);
                return true;
            } catch (ProviderUnavailableException e) {
                log.warn("saga {} : cancel {} essai {}/{} : {}", bookingId, mode, attempt, cancelAttempts, e.getMessage());
                pause(attempt);
            } catch (ProviderException e) {
                // cancel ne doit pas echouer metier : la reference est consideree liberee
                return true;
            }
        }
        return false;
    }

    private static void pause(int attempt) {
        try {
            Thread.sleep(200L * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Optional<Booking> load(String bookingId) {
        return tx.execute(s -> bookings.findById(bookingId));
    }

    private BookingStatus status(String bookingId) {
        return load(bookingId).orElseThrow().getStatus();
    }

    private void update(String bookingId, Consumer<Booking> change) {
        tx.executeWithoutResult(s -> {
            Booking b = bookings.findById(bookingId).orElseThrow();
            change.accept(b);
        });
    }
}
