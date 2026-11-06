package ma.mobility.pricing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class QuoteLine {

    @Column(name = "offer_id", nullable = false)
    private String offerId;

    @Column(nullable = false)
    private String mode;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price_minor", nullable = false)
    private long unitPriceMinor;

    @Column(name = "line_total_minor", nullable = false)
    private long lineTotalMinor;

    protected QuoteLine() {
    }

    public QuoteLine(String offerId, String mode, int quantity, long unitPriceMinor) {
        this.offerId = offerId;
        this.mode = mode;
        this.quantity = quantity;
        this.unitPriceMinor = unitPriceMinor;
        this.lineTotalMinor = unitPriceMinor * quantity;
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

    public long getLineTotalMinor() {
        return lineTotalMinor;
    }
}
