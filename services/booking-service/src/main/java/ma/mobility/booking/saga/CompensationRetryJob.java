package ma.mobility.booking.saga;

import java.time.Clock;
import java.time.Duration;

import ma.mobility.booking.domain.Booking;
import ma.mobility.booking.domain.BookingRepository;
import ma.mobility.booking.domain.BookingStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Relance les compensations restees incompletes (operateur injoignable pendant le cancel)
@Component
@ConditionalOnProperty(name = "booking.saga.retry-enabled", havingValue = "true")
public class CompensationRetryJob {

    private final BookingRepository bookings;
    private final SagaOrchestrator orchestrator;
    private final Clock clock;
    private final Duration delay;

    public CompensationRetryJob(BookingRepository bookings, SagaOrchestrator orchestrator, Clock clock,
                                @Value("${booking.saga.retry-delay-ms}") long delayMs) {
        this.bookings = bookings;
        this.orchestrator = orchestrator;
        this.clock = clock;
        this.delay = Duration.ofMillis(delayMs);
    }

    @Scheduled(fixedDelayString = "${booking.saga.retry-delay-ms}")
    public void retry() {
        bookings.findByStatusAndUpdatedAtBefore(BookingStatus.COMPENSATING, clock.instant().minus(delay))
                .stream().map(Booking::getId).forEach(orchestrator::run);
    }
}
