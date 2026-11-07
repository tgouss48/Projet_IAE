package ma.mobility.booking.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

// Appelle l'API REST commune des operateurs (format canonique)
@Component
public class HttpProviderClient implements ProviderClient {

    record HoldBody(String offerId, int quantity) {
    }

    record HoldResult(String holdId) {
    }

    record ConfirmResult(String providerRef) {
    }

    record ErrorBody(String code, String reason) {
    }

    private final Map<String, RestClient> clients = new HashMap<>();

    public HttpProviderClient(ProvidersProperties props, RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.connectTimeoutMs());
        factory.setReadTimeout(props.readTimeoutMs());
        props.providers().forEach((mode, url) ->
                clients.put(mode, builder.clone().baseUrl(url).requestFactory(factory).build()));
    }

    @Override
    public String hold(String mode, String offerId, int quantity) {
        return call(mode, c -> c.post().uri("/api/v1/holds")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new HoldBody(offerId, quantity))
                .retrieve().body(HoldResult.class).holdId());
    }

    @Override
    public String confirm(String mode, String holdId) {
        return call(mode, c -> c.post().uri("/api/v1/holds/{holdId}/confirmation", holdId)
                .retrieve().body(ConfirmResult.class).providerRef());
    }

    @Override
    public void cancel(String mode, String reference) {
        call(mode, c -> c.delete().uri("/api/v1/reservations/{ref}", reference).retrieve().toBodilessEntity());
    }

    private <T> T call(String mode, Function<RestClient, T> request) {
        RestClient client = clients.get(mode);
        if (client == null) {
            throw new OfferNotAvailableException("mode non gere : " + mode);
        }
        try {
            return request.apply(client);
        } catch (HttpClientErrorException e) {
            // 409 (indisponible) ou 400 : refus definitif
            throw new OfferNotAvailableException(reason(e));
        } catch (HttpServerErrorException | ResourceAccessException e) {
            throw new ProviderUnavailableException(mode + " injoignable : " + e.getMessage());
        }
    }

    private static String reason(HttpClientErrorException e) {
        try {
            ErrorBody body = e.getResponseBodyAs(ErrorBody.class);
            if (body != null && body.reason() != null) {
                return body.reason();
            }
        } catch (RuntimeException ignored) {
            // corps illisible, on garde le statut HTTP
        }
        return "HTTP " + e.getStatusCode().value();
    }
}
