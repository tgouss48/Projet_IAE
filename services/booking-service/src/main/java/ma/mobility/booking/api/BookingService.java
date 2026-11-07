package ma.mobility.booking.api;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ma.mobility.booking.domain.Booking;
import ma.mobility.booking.domain.BookingRepository;
import ma.mobility.booking.domain.BookingStatus;
import ma.mobility.booking.quote.Quote;
import ma.mobility.booking.quote.QuoteClient;
import ma.mobility.booking.saga.BookingEvent;
import ma.mobility.booking.saga.BookingEvents;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class BookingService {

    private final BookingRepository bookings;
    private final QuoteClient quotes;
    private final BookingEvents events;
    private final TransactionTemplate tx;
    private final Clock clock;

    public BookingService(BookingRepository bookings, QuoteClient quotes, BookingEvents events,
                          TransactionTemplate tx, Clock clock) {
        this.bookings = bookings;
        this.quotes = quotes;
        this.events = events;
        this.tx = tx;
        this.clock = clock;
    }

    // Cree le dossier et l'evenement BOOKING_REQUESTED ; la saga s'execute ensuite en asynchrone.
    // Avec la meme Idempotency-Key, un client qui rejoue sa requete recupere le meme dossier.
    public Booking create(String customerId, String quoteId, String idempotencyKey) {
        if (idempotencyKey != null) {
            var existing = bookings.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        Quote quote = quotes.find(quoteId).orElseThrow(() ->
                new BookingException(HttpStatus.CONFLICT, "QUOTE_INVALID", "Devis inconnu : " + quoteId));
        Instant now = clock.instant();
        if (now.isAfter(Instant.parse(quote.expiresAt()))) {
            throw new BookingException(HttpStatus.CONFLICT, "QUOTE_EXPIRED", "Devis expire : " + quoteId);
        }

        try {
            return tx.execute(s -> {
                Booking b = new Booking(UUID.randomUUID().toString(), customerId, quoteId, idempotencyKey,
                        quote.total().amountMinor(), quote.total().currency(), now);
                quote.lines().forEach(l -> b.addLeg(l.offerId(), l.mode(), l.quantity(), l.unitPrice().amountMinor()));
                bookings.save(b);
                events.record(BookingEvent.BOOKING_REQUESTED, b);
                return b;
            });
        } catch (DataIntegrityViolationException e) {
            // deux requetes simultanees avec la meme cle : on renvoie celle qui a gagne
            return bookings.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey).orElseThrow(() -> e);
        }
    }

    public Booking get(String customerId, String bookingId) {
        return bookings.findById(bookingId)
                .filter(b -> b.getCustomerId().equals(customerId))
                .orElseThrow(() -> BookingException.notFound(bookingId));
    }

    public List<Booking> list(String customerId) {
        return bookings.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    // Annulation par le client d'un dossier confirme : meme compensation que la saga
    public Booking cancel(String customerId, String bookingId) {
        return tx.execute(s -> {
            Booking b = get(customerId, bookingId);
            if (b.getStatus() == BookingStatus.CANCELLED || b.getStatus() == BookingStatus.COMPENSATING) {
                return b;
            }
            if (b.getStatus() != BookingStatus.CONFIRMED) {
                throw new BookingException(HttpStatus.CONFLICT, "INVALID_STATE",
                        "Reservation en cours (" + b.getStatus() + "), annulation impossible pour l'instant");
            }
            b.startCompensation("annulation demandee par le client", clock.instant());
            events.record(BookingEvent.BOOKING_CANCEL_REQUESTED, b);
            return b;
        });
    }
}
