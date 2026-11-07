package ma.mobility.booking.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, String> {

    Optional<Booking> findByCustomerIdAndIdempotencyKey(String customerId, String idempotencyKey);

    List<Booking> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<Booking> findByStatusAndUpdatedAtBefore(BookingStatus status, Instant before);
}
