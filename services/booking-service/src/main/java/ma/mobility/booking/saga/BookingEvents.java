package ma.mobility.booking.saga;

import java.time.Clock;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.mobility.booking.domain.Booking;
import ma.mobility.booking.domain.BookingLeg;
import ma.mobility.booking.domain.OutboxEvent;
import ma.mobility.booking.domain.OutboxRepository;
import org.springframework.stereotype.Component;

// Ecrit les evenements dans l'outbox ; doit etre appele dans la transaction qui modifie le dossier
@Component
public class BookingEvents {

    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final Clock clock;

    public BookingEvents(OutboxRepository outbox, ObjectMapper mapper, Clock clock) {
        this.outbox = outbox;
        this.mapper = mapper;
        this.clock = clock;
    }

    public void record(String type, Booking booking) {
        record(type, booking, null, null, booking.getFailureReason());
    }

    public void record(String type, Booking booking, BookingLeg leg, String reference, String reason) {
        BookingEvent event = new BookingEvent(
                UUID.randomUUID().toString(),
                type,
                booking.getId(),
                booking.getCustomerId(),
                booking.getStatus().name(),
                leg != null ? leg.getLegIndex() : null,
                leg != null ? leg.getMode() : null,
                leg != null ? leg.getOfferId() : null,
                reference,
                reason,
                leg == null ? booking.getTotalMinor() : null,
                leg == null ? booking.getCurrency() : null,
                clock.instant().toString());
        try {
            outbox.save(new OutboxEvent(event.eventId(), booking.getId(), type,
                    mapper.writeValueAsString(event), clock.instant()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serialisation impossible de " + type, e);
        }
    }
}
