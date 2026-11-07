package ma.mobility.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import ma.mobility.gateway.security.TokenResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayTest {

    static final String TRAIN = """
            [{"offerId":"TRAIN:RAIL-CAS-RAB-0730","mode":"TRAIN","provider":"rail-operator","origin":"Casablanca",
              "destination":"Rabat","departure":"2026-11-15T07:30:00","arrival":"2026-11-15T08:25:00",
              "price":{"amountMinor":8000,"currency":"MAD"},"availability":120}]""";
    static final String CAR = """
            [{"offerId":"CARSHARING:VEH-PAR-001:2026-11-15","mode":"CARSHARING","provider":"carsharing",
              "origin":"Paris","destination":"Paris","departure":"2026-11-15T00:00:00",
              "arrival":"2026-11-16T00:00:00","price":{"amountMinor":45000,"currency":"MAD"},"availability":1}]""";

    static MockWebServer rail;
    static MockWebServer air;
    static MockWebServer carsharing;

    @BeforeAll
    static void start() throws IOException {
        rail = new MockWebServer();
        air = new MockWebServer();
        carsharing = new MockWebServer();
        rail.start();
        air.start();
        carsharing.start();
    }

    @AfterAll
    static void stop() throws IOException {
        rail.shutdown();
        air.shutdown();
        carsharing.shutdown();
    }

    @DynamicPropertySource
    static void urls(DynamicPropertyRegistry registry) {
        registry.add("gateway.services.rail", () -> rail.url("/").toString());
        registry.add("gateway.services.air", () -> air.url("/").toString());
        registry.add("gateway.services.carsharing", () -> carsharing.url("/").toString());
    }

    @Autowired
    WebTestClient client;

    @Test
    void sansTokenRefuse() {
        client.get().uri("/api/v1/journeys/search?origin=Casablanca&destination=Paris&date=2026-11-15")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void mauvaisMotDePasse() {
        client.post().uri("/api/v1/auth/token").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"username\":\"demo\",\"password\":\"faux\"}")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.code").isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void rechercheAgregeeAvecUnOperateurEnPanne() throws InterruptedException {
        rail.enqueue(json(TRAIN));
        air.enqueue(new MockResponse().setResponseCode(503));
        carsharing.enqueue(json(CAR));

        client.get().uri("/api/v1/journeys/search?origin=Casablanca&via=Rabat&destination=Paris&date=2026-11-15")
                .header("Authorization", "Bearer " + token())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.segments.length()").isEqualTo(3)
                .jsonPath("$.segments[0].mode").isEqualTo("TRAIN")
                .jsonPath("$.segments[0].status").isEqualTo("OK")
                .jsonPath("$.segments[0].offers[0].offerId").isEqualTo("TRAIN:RAIL-CAS-RAB-0730")
                .jsonPath("$.segments[1].mode").isEqualTo("FLIGHT")
                .jsonPath("$.segments[1].from").isEqualTo("Rabat")
                .jsonPath("$.segments[1].status").isEqualTo("UNAVAILABLE")
                .jsonPath("$.segments[2].status").isEqualTo("OK");

        RecordedRequest trainCall = rail.takeRequest();
        assertThat(trainCall.getPath()).contains("origin=Casablanca").contains("destination=Rabat");
        RecordedRequest carCall = carsharing.takeRequest();
        assertThat(carCall.getPath()).contains("city=Paris");
        air.takeRequest();
    }

    private String token() {
        TokenResponse t = client.post().uri("/api/v1/auth/token").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"username\":\"demo\",\"password\":\"demo123\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody(TokenResponse.class).returnResult().getResponseBody();
        assertThat(t).isNotNull();
        return t.accessToken();
    }

    private static MockResponse json(String body) {
        return new MockResponse().setHeader("Content-Type", "application/json").setBody(body);
    }
}
