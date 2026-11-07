package ma.mobility.gateway;

import java.time.Duration;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway")
public record GatewayProperties(String jwtSecret,
                                Duration tokenTtl,
                                Map<String, String> users,
                                Duration searchTimeout,
                                Services services) {

    public record Services(String rail, String air, String carsharing) {
    }
}
