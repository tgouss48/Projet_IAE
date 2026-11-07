package ma.mobility.booking.saga;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// Demarre (ou reprend) la saga quand un dossier est demande ou annule par le client
@Component
public class SagaListener {

    private final SagaOrchestrator orchestrator;
    private final ObjectMapper mapper;

    public SagaListener(SagaOrchestrator orchestrator, ObjectMapper mapper) {
        this.orchestrator = orchestrator;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "${booking.topic}", groupId = "booking-saga")
    public void on(String payload) throws JsonProcessingException {
        BookingEvent event = mapper.readValue(payload, BookingEvent.class);
        if (BookingEvent.BOOKING_REQUESTED.equals(event.type())
                || BookingEvent.BOOKING_CANCEL_REQUESTED.equals(event.type())) {
            orchestrator.run(event.bookingId());
        }
    }
}
