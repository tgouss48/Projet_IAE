package ma.mobility.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import ma.mobility.pricing.domain.PriceBreakdown;
import ma.mobility.pricing.domain.QuoteLine;
import org.junit.jupiter.api.Test;

class PriceBreakdownTest {

    static final Map<String, Long> RULES = Map.of(
            "DISCOUNT_2_MODES", 500L,
            "DISCOUNT_3_MODES", 1000L,
            "SERVICE_FEE", 1500L);

    @Test
    void unSeulModePasDeRemise() {
        PriceBreakdown p = PriceBreakdown.compute(
                List.of(new QuoteLine("TRAIN:A", "TRAIN", 2, 8000)), RULES);

        assertEquals(16000, p.subtotalMinor());
        assertEquals(0, p.discountMinor());
        assertEquals(17500, p.totalMinor());
    }

    @Test
    void deuxModesRemiseCinqPourcent() {
        PriceBreakdown p = PriceBreakdown.compute(List.of(
                new QuoteLine("TRAIN:A", "TRAIN", 2, 8000),
                new QuoteLine("FLIGHT:B", "FLIGHT", 2, 180000)), RULES);

        assertEquals(376000, p.subtotalMinor());
        assertEquals(500, p.discountBp());
        assertEquals(18800, p.discountMinor());
        assertEquals(376000 - 18800 + 1500, p.totalMinor());
    }

    @Test
    void troisModesRemiseDixPourcent() {
        PriceBreakdown p = PriceBreakdown.compute(List.of(
                new QuoteLine("TRAIN:A", "TRAIN", 2, 8000),
                new QuoteLine("FLIGHT:B", "FLIGHT", 2, 180000),
                new QuoteLine("CARSHARING:C", "CARSHARING", 1, 45000)), RULES);

        assertEquals(421000, p.subtotalMinor());
        assertEquals(42100, p.discountMinor());
        assertEquals(421000 - 42100 + 1500, p.totalMinor());
    }

    @Test
    void deuxSegmentsMemeModeComptentPourUnMode() {
        PriceBreakdown p = PriceBreakdown.compute(List.of(
                new QuoteLine("TRAIN:A", "TRAIN", 1, 8000),
                new QuoteLine("TRAIN:B", "TRAIN", 1, 13500)), RULES);

        assertEquals(0, p.discountMinor());
    }

    @Test
    void arrondiAuCentime() {
        // 5 % de 333 = 16,65 -> 17
        PriceBreakdown p = PriceBreakdown.compute(List.of(
                new QuoteLine("TRAIN:A", "TRAIN", 1, 111),
                new QuoteLine("FLIGHT:B", "FLIGHT", 1, 222)), RULES);

        assertEquals(17, p.discountMinor());
    }
}
