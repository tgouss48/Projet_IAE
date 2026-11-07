package ma.mobility.booking.saga;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ma.mobility.booking.domain.OutboxEvent;
import ma.mobility.booking.domain.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Publie sur Kafka les evenements de l'outbox, dans l'ordre. Un evenement peut etre publie
// deux fois si le service s'arrete entre l'envoi et le marquage : les consommateurs
// doivent donc etre idempotents (eventId).
@Component
@ConditionalOnProperty(name = "booking.outbox.enabled", havingValue = "true")
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;
    private final Clock clock;
    private final String topic;

    public OutboxRelay(OutboxRepository outbox, KafkaTemplate<String, String> kafka, Clock clock,
                       @Value("${booking.topic}") String topic) {
        this.outbox = outbox;
        this.kafka = kafka;
        this.clock = clock;
        this.topic = topic;
    }

    @Scheduled(fixedDelayString = "${booking.outbox.poll-ms}")
    public void publish() {
        List<OutboxEvent> pending = outbox.findTop50ByPublishedAtIsNullOrderByIdAsc();
        for (OutboxEvent event : pending) {
            try {
                kafka.send(topic, event.getAggregateId(), event.getPayload()).get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("publication Kafka impossible ({}), nouvel essai plus tard", e.getMessage());
                return;
            }
            event.published(clock.instant());
            outbox.save(event);
        }
    }
}
