package ma.mobility.carsharing.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByHoldId(String holdId);

    Optional<Reservation> findByBookingRef(String bookingRef);

    // un vehicule est pris si une reservation est confirmee ou si un hold n'a pas expire
    @Query("""
            select count(r) > 0 from Reservation r
            where r.vehicleId = :vehicleId and r.rentalDate = :date
              and (r.status = ma.mobility.carsharing.domain.ReservationStatus.CONFIRMED
                   or (r.status = ma.mobility.carsharing.domain.ReservationStatus.HELD and r.expiresAt > :now))
            """)
    boolean isTaken(@Param("vehicleId") String vehicleId, @Param("date") LocalDate date, @Param("now") Instant now);

    @Modifying
    @Query("""
            update Reservation r set r.status = ma.mobility.carsharing.domain.ReservationStatus.EXPIRED
            where r.status = ma.mobility.carsharing.domain.ReservationStatus.HELD and r.expiresAt <= :now
            """)
    int expireHolds(@Param("now") Instant now);
}
