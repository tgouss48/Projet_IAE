package ma.mobility.pricing.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;

@Entity
public class Quote {

    @Id
    private String id;

    @Column(nullable = false)
    private String currency;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "quote_line", joinColumns = @JoinColumn(name = "quote_id"))
    @OrderColumn(name = "line_index")
    private List<QuoteLine> lines = new ArrayList<>();

    @Column(name = "subtotal_minor", nullable = false)
    private long subtotalMinor;

    @Column(name = "discount_bp", nullable = false)
    private int discountBp;

    @Column(name = "discount_minor", nullable = false)
    private long discountMinor;

    @Column(name = "service_fee_minor", nullable = false)
    private long serviceFeeMinor;

    @Column(name = "total_minor", nullable = false)
    private long totalMinor;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected Quote() {
    }

    public Quote(String id, String currency, List<QuoteLine> lines, PriceBreakdown price,
                 Instant createdAt, Instant expiresAt) {
        this.id = id;
        this.currency = currency;
        this.lines = new ArrayList<>(lines);
        this.subtotalMinor = price.subtotalMinor();
        this.discountBp = price.discountBp();
        this.discountMinor = price.discountMinor();
        this.serviceFeeMinor = price.serviceFeeMinor();
        this.totalMinor = price.totalMinor();
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getId() {
        return id;
    }

    public String getCurrency() {
        return currency;
    }

    public List<QuoteLine> getLines() {
        return lines;
    }

    public long getSubtotalMinor() {
        return subtotalMinor;
    }

    public int getDiscountBp() {
        return discountBp;
    }

    public long getDiscountMinor() {
        return discountMinor;
    }

    public long getServiceFeeMinor() {
        return serviceFeeMinor;
    }

    public long getTotalMinor() {
        return totalMinor;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
