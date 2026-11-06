package ma.mobility.pricing.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PricingRuleRepository extends JpaRepository<PricingRule, String> {
}
