package ma.mobility.booking.provider;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

// URL de chaque operateur par mode : TRAIN, FLIGHT, CARSHARING
@ConfigurationProperties(prefix = "booking")
public record ProvidersProperties(Map<String, String> providers, int connectTimeoutMs, int readTimeoutMs) {
}
