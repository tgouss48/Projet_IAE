package ma.mobility.booking.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Version;

// Dossier de voyage : etat de la saga et de chaque segment
@Entity
public class Booking {

    @Id
    private String id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "quote_id", nullable = false)
    private String quoteId;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "total_minor", nullable = false)
    private long totalMinor;

    @Column(nullable = false)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("legIndex")
    private List<BookingLeg> legs = new ArrayList<>();

    protected Booking() {
    }

    public Booking(String id, String customerId, String quoteId, String idempotencyKey,
                   long totalMinor, String currency, Instant now) {
        this.id = id;
        this.customerId = customerId;
        this.quoteId = quoteId;
        this.idempotencyKey = idempotencyKey;
        this.totalMinor = totalMinor;
        this.currency = currency;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void addLeg(String offerId, String mode, int quantity, long unitPriceMinor) {
        legs.add(new BookingLeg(this, legs.size(), offerId, mode, quantity, unitPriceMinor));
    }

    public BookingLeg leg(int index) {
        return legs.get(index);
    }

    public void moveTo(BookingStatus next, Instant now) {
        this.status = next;
        this.updatedAt = now;
    }

    public void startCompensation(String reason, Instant now) {
        if (failureReason == null) {
            failureReason = reason;
        }
        moveTo(BookingStatus.COMPENSATING, now);
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getQuoteId() {
        return quoteId;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public long getTotalMinor() {
        return totalMinor;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<BookingLeg> getLegs() {
        return legs;
    }
}
