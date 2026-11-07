package ma.mobility.booking.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "booking_leg")
public class BookingLeg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Column(name = "leg_index", nullable = false)
    private int legIndex;

    @Column(name = "offer_id", nullable = false)
    private String offerId;

    @Column(nullable = false)
    private String mode;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price_minor", nullable = false)
    private long unitPriceMinor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LegStatus status = LegStatus.PENDING;

    @Column(name = "hold_id")
    private String holdId;

    @Column(name = "provider_ref")
    private String providerRef;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "held_at")
    private Instant heldAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    protected BookingLeg() {
    }

    BookingLeg(Booking booking, int legIndex, String offerId, String mode, int quantity, long unitPriceMinor) {
        this.booking = booking;
        this.legIndex = legIndex;
        this.offerId = offerId;
        this.mode = mode;
        this.quantity = quantity;
        this.unitPriceMinor = unitPriceMinor;
    }

    public void held(String holdId, Instant now) {
        this.status = LegStatus.HELD;
        this.holdId = holdId;
        this.heldAt = now;
    }

    public void confirmed(String providerRef) {
        this.status = LegStatus.CONFIRMED;
        this.providerRef = providerRef;
    }

    public void failed(String reason) {
        this.status = LegStatus.FAILED;
        this.failureReason = reason;
    }

    // le segment a ete libere chez l'operateur (ou n'avait rien a liberer)
    public void released(Instant now) {
        if (status != LegStatus.FAILED) {
            status = LegStatus.CANCELLED;
        }
        releasedAt = now;
    }

    // quelque chose a ete engage chez l'operateur et n'a pas encore ete annule
    public boolean needsRelease() {
        return holdId != null && releasedAt == null;
    }

    // reference a annuler : la reservation ferme si elle existe, sinon le maintien
    public String cancelReference() {
        return providerRef != null ? providerRef : holdId;
    }

    public int getLegIndex() {
        return legIndex;
    }

    public String getOfferId() {
        return offerId;
    }

    public String getMode() {
        return mode;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getUnitPriceMinor() {
        return unitPriceMinor;
    }

    public LegStatus getStatus() {
        return status;
    }

    public String getHoldId() {
        return holdId;
    }

    public String getProviderRef() {
        return providerRef;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getHeldAt() {
        return heldAt;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }
}
