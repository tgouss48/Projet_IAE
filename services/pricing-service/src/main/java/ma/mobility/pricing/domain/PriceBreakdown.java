package ma.mobility.pricing.domain;

import java.util.List;
import java.util.Map;

// Calcul du prix, sans acces base : remise selon le nombre de modes + frais de service
public record PriceBreakdown(long subtotalMinor, int discountBp, long discountMinor,
                             long serviceFeeMinor, long totalMinor) {

    public static PriceBreakdown compute(List<QuoteLine> lines, Map<String, Long> rules) {
        long subtotal = lines.stream().mapToLong(QuoteLine::getLineTotalMinor).sum();
        long distinctModes = lines.stream().map(QuoteLine::getMode).distinct().count();

        int bp = 0;
        if (distinctModes >= 3) {
            bp = rules.getOrDefault(PricingRule.DISCOUNT_3_MODES, 0L).intValue();
        } else if (distinctModes == 2) {
            bp = rules.getOrDefault(PricingRule.DISCOUNT_2_MODES, 0L).intValue();
        }
        // arrondi au centime le plus proche, en entiers
        long discount = (subtotal * bp + 5_000) / 10_000;
        long fee = rules.getOrDefault(PricingRule.SERVICE_FEE, 0L);
        return new PriceBreakdown(subtotal, bp, discount, fee, subtotal - discount + fee);
    }
}
