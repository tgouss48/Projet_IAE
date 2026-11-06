package ma.mobility.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import ma.mobility.pricing.api.Money;
import ma.mobility.pricing.api.PricingService;
import ma.mobility.pricing.api.QuoteLegRequest;
import ma.mobility.pricing.api.QuoteNotFoundException;
import ma.mobility.pricing.api.QuoteRequest;
import ma.mobility.pricing.api.QuoteResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PricingServiceTest {

    @Autowired
    PricingService service;

    @Test
    void devisEnregistrePuisRelu() {
        QuoteResponse created = service.createQuote(new QuoteRequest(List.of(
                new QuoteLegRequest("TRAIN:RAIL-CAS-RAB-0730", "TRAIN", new Money(8000, "MAD"), 2),
                new QuoteLegRequest("FLIGHT:AIR-RBA-ORY-1200", "FLIGHT", new Money(180000, "MAD"), 2))));

        assertEquals(5, created.discountPercent());
        assertEquals(new Money(358700, "MAD"), created.total());

        QuoteResponse read = service.getQuote(created.quoteId());
        assertEquals(created.total(), read.total());
        assertEquals(2, read.lines().size());
        assertEquals("TRAIN:RAIL-CAS-RAB-0730", read.lines().get(0).offerId());
        assertEquals(new Money(16000, "MAD"), read.lines().get(0).lineTotal());
    }

    @Test
    void devisInconnu() {
        assertThrows(QuoteNotFoundException.class, () -> service.getQuote("Q-inconnu"));
    }

    @Test
    void modeOuDeviseInvalide() {
        assertThrows(IllegalArgumentException.class, () -> service.createQuote(new QuoteRequest(List.of(
                new QuoteLegRequest("BUS:X", "BUS", new Money(1000, "MAD"), 1)))));
        assertThrows(IllegalArgumentException.class, () -> service.createQuote(new QuoteRequest(List.of(
                new QuoteLegRequest("TRAIN:X", "TRAIN", new Money(1000, "EUR"), 1)))));
    }
}
