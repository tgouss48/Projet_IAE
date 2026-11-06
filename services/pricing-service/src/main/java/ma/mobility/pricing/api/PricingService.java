package ma.mobility.pricing.api;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import ma.mobility.pricing.domain.PriceBreakdown;
import ma.mobility.pricing.domain.PricingRule;
import ma.mobility.pricing.domain.PricingRuleRepository;
import ma.mobility.pricing.domain.Quote;
import ma.mobility.pricing.domain.QuoteLine;
import ma.mobility.pricing.domain.QuoteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PricingService {

    static final Set<String> MODES = Set.of("TRAIN", "FLIGHT", "CARSHARING");

    private final PricingRuleRepository rules;
    private final QuoteRepository quotes;
    private final Clock clock;
    private final String currency;
    private final Duration quoteTtl;

    public PricingService(PricingRuleRepository rules, QuoteRepository quotes, Clock clock,
                          @Value("${pricing.currency}") String currency,
                          @Value("${pricing.quote-ttl}") Duration quoteTtl) {
        this.rules = rules;
        this.quotes = quotes;
        this.clock = clock;
        this.currency = currency;
        this.quoteTtl = quoteTtl;
    }

    @Transactional
    public QuoteResponse createQuote(QuoteRequest request) {
        List<QuoteLine> lines = request.legs().stream().map(this::toLine).toList();
        Map<String, Long> ruleValues = rules.findAll().stream()
                .collect(Collectors.toMap(PricingRule::getCode, PricingRule::getValue));

        Instant now = clock.instant();
        Quote quote = new Quote("Q-" + UUID.randomUUID(), currency, lines,
                PriceBreakdown.compute(lines, ruleValues), now, now.plus(quoteTtl));
        return toResponse(quotes.save(quote));
    }

    @Transactional(readOnly = true)
    public QuoteResponse getQuote(String quoteId) {
        return quotes.findById(quoteId).map(this::toResponse)
                .orElseThrow(() -> new QuoteNotFoundException(quoteId));
    }

    private QuoteLine toLine(QuoteLegRequest leg) {
        if (!MODES.contains(leg.mode())) {
            throw new IllegalArgumentException("mode inconnu : " + leg.mode());
        }
        if (!currency.equals(leg.unitPrice().currency())) {
            throw new IllegalArgumentException("devise non supportee : " + leg.unitPrice().currency());
        }
        return new QuoteLine(leg.offerId(), leg.mode(), leg.quantity(), leg.unitPrice().amountMinor());
    }

    private QuoteResponse toResponse(Quote q) {
        List<QuoteResponse.Line> lines = q.getLines().stream()
                .map(l -> new QuoteResponse.Line(l.getOfferId(), l.getMode(), l.getQuantity(),
                        money(l.getUnitPriceMinor()), money(l.getLineTotalMinor())))
                .toList();
        return new QuoteResponse(q.getId(), lines, money(q.getSubtotalMinor()), q.getDiscountBp() / 100,
                money(q.getDiscountMinor()), money(q.getServiceFeeMinor()), money(q.getTotalMinor()),
                q.getExpiresAt().toString());
    }

    private Money money(long amountMinor) {
        return new Money(amountMinor, currency);
    }
}
