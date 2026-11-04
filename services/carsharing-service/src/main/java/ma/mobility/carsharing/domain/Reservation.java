package ma.mobility.carsharing.domain;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hold_id", nullable = false, unique = true)
    private String holdId;

    @Column(name = "booking_ref", unique = true)
    private String bookingRef;

    @Column(name = "vehicle_id", nullable = false)
    private String vehicleId;

    @Column(name = "rental_date", nullable = false)
    private LocalDate rentalDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Reservation() {
    }

    public Reservation(String holdId, String vehicleId, LocalDate rentalDate, Instant now, Instant expiresAt) {
        this.holdId = holdId;
        this.vehicleId = vehicleId;
        this.rentalDate = rentalDate;
        this.status = ReservationStatus.HELD;
        this.createdAt = now;
        this.expiresAt = expiresAt;
    }

    public boolean isActiveHold(Instant now) {
        return status == ReservationStatus.HELD && expiresAt.isAfter(now);
    }

    public void confirm(String bookingRef) {
        this.status = ReservationStatus.CONFIRMED;
        this.bookingRef = bookingRef;
    }

    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
    }

    public String getHoldId() {
        return holdId;
    }

    public String getBookingRef() {
        return bookingRef;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public LocalDate getRentalDate() {
        return rentalDate;
    }

    public ReservationStatus getStatus() {
        return status;
    }
}
