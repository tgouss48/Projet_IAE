package ma.mobility.gateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

// Transmet l'utilisateur authentifie aux services (X-User-Id). Un X-User-Id envoye
// par le client est toujours supprime pour qu'il ne puisse pas se faire passer pour un autre.
@Component
public class UserIdHeaderFilter implements GlobalFilter, Ordered {

    public static final String HEADER = "X-User-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .map(p -> withUser(exchange, ((Authentication) p).getName()))
                .defaultIfEmpty(withUser(exchange, null))
                .flatMap(chain::filter);
    }

    private static ServerWebExchange withUser(ServerWebExchange exchange, String user) {
        return exchange.mutate().request(r -> r.headers(h -> {
            h.remove(HEADER);
            if (user != null) {
                h.set(HEADER, user);
            }
        })).build();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
