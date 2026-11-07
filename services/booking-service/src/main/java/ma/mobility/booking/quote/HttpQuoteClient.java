package ma.mobility.booking.quote;

import java.util.Optional;

import ma.mobility.booking.provider.ProviderUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpQuoteClient implements QuoteClient {

    private final RestClient client;

    public HttpQuoteClient(RestClient.Builder builder, @Value("${booking.pricing-url}") String url,
                           @Value("${booking.connect-timeout-ms}") int connectTimeout,
                           @Value("${booking.read-timeout-ms}") int readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        this.client = builder.clone().baseUrl(url).requestFactory(factory).build();
    }

    @Override
    public Optional<Quote> find(String quoteId) {
        try {
            return Optional.ofNullable(client.get().uri("/api/v1/quotes/{id}", quoteId).retrieve().body(Quote.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new ProviderUnavailableException("tarification injoignable : " + e.getMessage());
        }
    }
}
