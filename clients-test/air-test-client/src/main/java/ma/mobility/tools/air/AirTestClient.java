package ma.mobility.tools.air;

import ma.mobility.tools.air.ws.CancelFlightRequest;
import ma.mobility.tools.air.ws.CancelFlightResponse;
import ma.mobility.tools.air.ws.ConfirmFlightRequest;
import ma.mobility.tools.air.ws.ConfirmFlightResponse;
import ma.mobility.tools.air.ws.Flight;
import ma.mobility.tools.air.ws.HoldFlightRequest;
import ma.mobility.tools.air.ws.HoldFlightResponse;
import ma.mobility.tools.air.ws.SearchFlightsRequest;
import ma.mobility.tools.air.ws.SearchFlightsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;

// Client de test pour le service SOAP aerien
@SpringBootApplication
public class AirTestClient {

    public static void main(String[] args) {
        SpringApplication.run(AirTestClient.class, args);
    }

    @Bean
    WebServiceTemplate airTemplate(@Value("${air.url}") String url) {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("ma.mobility.tools.air.ws");
        WebServiceTemplate template = new WebServiceTemplate(marshaller);
        template.setDefaultUri(url);
        return template;
    }

    @Bean
    CommandLineRunner run(WebServiceTemplate ws) {
        return args -> {
            SearchFlightsRequest search = new SearchFlightsRequest();
            search.setOrigin("Rabat");
            search.setDestination("Paris");
            search.setDate("2026-11-15");
            SearchFlightsResponse found = (SearchFlightsResponse) ws.marshalSendAndReceive(search);
            System.out.println("[OK] searchFlights : " + found.getFlight().size() + " vol(s)");
            for (Flight f : found.getFlight()) {
                System.out.printf("     %s %s -> %s %s %d %d%n",
                        f.getFlightId(), f.getOrigin(), f.getDestination(), f.getDeparture(),
                        f.getPriceMinor(), f.getSeatsAvailable());
            }
            if (found.getFlight().isEmpty()) {
                throw new IllegalStateException("aucun vol");
            }
            String flightId = found.getFlight().get(0).getFlightId();

            // cycle nominal puis annulation (cancel doit etre idempotent)
            String holdId = hold(ws, flightId, 2);
            System.out.println("[OK] holdFlight : " + holdId);
            ConfirmFlightRequest confirm = new ConfirmFlightRequest();
            confirm.setHoldId(holdId);
            String bookingRef = ((ConfirmFlightResponse) ws.marshalSendAndReceive(confirm)).getBookingRef();
            System.out.println("[OK] confirmFlight : " + bookingRef);
            System.out.println("[OK] cancelFlight : " + cancel(ws, bookingRef));
            System.out.println("[OK] cancelFlight rejoue : " + cancel(ws, bookingRef));

            // liberation d'un hold non confirme
            String holdId2 = hold(ws, flightId, 1);
            System.out.println("[OK] hold + cancelFlight(holdId) : " + cancel(ws, holdId2));

            // cas d'erreur attendu : le vol CMN-MAD n'a que 3 places
            try {
                hold(ws, "AIR-CMN-MAD-1800", 5);
                System.out.println("[KO] hold 5 places sur AIR-CMN-MAD-1800 accepte");
            } catch (SoapFaultClientException e) {
                System.out.println("[OK] hold 5 places refuse : " + e.getFaultStringOrReason());
            }
        };
    }

    private static String hold(WebServiceTemplate ws, String flightId, int seats) {
        HoldFlightRequest req = new HoldFlightRequest();
        req.setFlightId(flightId);
        req.setSeats(seats);
        return ((HoldFlightResponse) ws.marshalSendAndReceive(req)).getHoldId();
    }

    private static String cancel(WebServiceTemplate ws, String reference) {
        CancelFlightRequest req = new CancelFlightRequest();
        req.setReference(reference);
        return ((CancelFlightResponse) ws.marshalSendAndReceive(req)).getStatus();
    }
}
