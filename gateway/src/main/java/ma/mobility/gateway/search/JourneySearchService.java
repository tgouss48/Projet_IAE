package ma.mobility.gateway.search;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

import ma.mobility.gateway.GatewayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

// Recherche multimodale : les trois operateurs sont interroges en parallele
@Service
public class JourneySearchService {

    private static final Logger log = LoggerFactory.getLogger(JourneySearchService.class);

    private final WebClient rail;
    private final WebClient air;
    private final WebClient carsharing;
    private final Duration timeout;

    public JourneySearchService(WebClient.Builder builder, GatewayProperties props) {
        this.rail = builder.clone().baseUrl(props.services().rail()).build();
        this.air = builder.clone().baseUrl(props.services().air()).build();
        this.carsharing = builder.clone().baseUrl(props.services().carsharing()).build();
        this.timeout = props.searchTimeout();
    }

    // sans via : train et avion origin -> destination ; avec via : train origin -> via, avion via -> destination.
    // la voiture est toujours proposee a destination.
    public Mono<JourneySearchResponse> search(String origin, String via, String destination, LocalDate date) {
        boolean hasVia = via != null && !via.isBlank();
        String trainTo = hasVia ? via : destination;
        String flightFrom = hasVia ? via : origin;

        Mono<SegmentResult> train = segment(rail, "TRAIN", origin, trainTo,
                b -> b.path("/api/v1/offers").queryParam("origin", origin)
                        .queryParam("destination", trainTo).queryParam("date", date).build());
        Mono<SegmentResult> flight = segment(air, "FLIGHT", flightFrom, destination,
                b -> b.path("/api/v1/offers").queryParam("origin", flightFrom)
                        .queryParam("destination", destination).queryParam("date", date).build());
        Mono<SegmentResult> car = segment(carsharing, "CARSHARING", destination, destination,
                b -> b.path("/api/v1/offers").queryParam("city", destination).queryParam("date", date).build());

        return Mono.zip(train, flight, car)
                .map(t -> new JourneySearchResponse(origin, hasVia ? via : null, destination, date.toString(),
                        List.of(t.getT1(), t.getT2(), t.getT3())));
    }

    private Mono<SegmentResult> segment(WebClient client, String mode, String from, String to,
                                        Function<UriBuilder, URI> uri) {
        return client.get().uri(uri).retrieve()
                .bodyToFlux(Offer.class).collectList()
                .timeout(timeout)
                .map(offers -> SegmentResult.ok(mode, from, to, offers))
                .onErrorResume(e -> {
                    log.warn("recherche {} indisponible : {}", mode, e.toString());
                    return Mono.just(SegmentResult.unavailable(mode, from, to, describe(e)));
                });
    }

    private static String describe(Throwable e) {
        if (e instanceof WebClientResponseException w) {
            return "HTTP " + w.getStatusCode().value();
        }
        return e.getClass().getSimpleName();
    }
}
