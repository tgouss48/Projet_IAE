package ma.mobility.pricing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pricing_rule")
public class PricingRule {

    public static final String DISCOUNT_2_MODES = "DISCOUNT_2_MODES";
    public static final String DISCOUNT_3_MODES = "DISCOUNT_3_MODES";
    public static final String SERVICE_FEE = "SERVICE_FEE";

    @Id
    private String code;

    @Column(name = "rule_value", nullable = false)
    private long value;

    @Column(nullable = false)
    private String description;

    protected PricingRule() {
    }

    public String getCode() {
        return code;
    }

    public long getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
}
