package ma.mobility.airbridge.soap;

import java.util.List;

import ma.mobility.airbridge.api.OfferNotAvailableException;
import ma.mobility.airbridge.api.ProviderUnavailableException;
import ma.mobility.airbridge.soap.gen.CancelFlightRequest;
import ma.mobility.airbridge.soap.gen.CancelFlightResponse;
import ma.mobility.airbridge.soap.gen.ConfirmFlightRequest;
import ma.mobility.airbridge.soap.gen.ConfirmFlightResponse;
import ma.mobility.airbridge.soap.gen.Flight;
import ma.mobility.airbridge.soap.gen.HoldFlightRequest;
import ma.mobility.airbridge.soap.gen.HoldFlightResponse;
import ma.mobility.airbridge.soap.gen.SearchFlightsRequest;
import ma.mobility.airbridge.soap.gen.SearchFlightsResponse;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.WebServiceIOException;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;

// Appels SOAP au partenaire aerien, traduit les erreurs SOAP en erreurs du bridge
@Component
public class AirClient {

    private final WebServiceTemplate ws;

    public AirClient(WebServiceTemplate ws) {
        this.ws = ws;
    }

    public List<Flight> search(String origin, String destination, String date) {
        SearchFlightsRequest req = new SearchFlightsRequest();
        req.setOrigin(origin);
        req.setDestination(destination);
        req.setDate(date);
        return this.<SearchFlightsResponse>send(req).getFlight();
    }

    public String hold(String flightId, int seats) {
        HoldFlightRequest req = new HoldFlightRequest();
        req.setFlightId(flightId);
        req.setSeats(seats);
        return this.<HoldFlightResponse>send(req).getHoldId();
    }

    public String confirm(String holdId) {
        ConfirmFlightRequest req = new ConfirmFlightRequest();
        req.setHoldId(holdId);
        return this.<ConfirmFlightResponse>send(req).getBookingRef();
    }

    // le status (BOOKING_CANCELLED, HOLD_RELEASED, NOT_FOUND) n'est pas une erreur
    public String cancel(String reference) {
        CancelFlightRequest req = new CancelFlightRequest();
        req.setReference(reference);
        return this.<CancelFlightResponse>send(req).getStatus();
    }

    @SuppressWarnings("unchecked")
    private <T> T send(Object request) {
        try {
            return (T) ws.marshalSendAndReceive(request);
        } catch (SoapFaultClientException e) {
            // le WSDL ne declare pas de fault type : toute fault metier = indisponible
            throw new OfferNotAvailableException(e.getFaultStringOrReason());
        } catch (WebServiceIOException e) {
            // couvre aussi WebServiceTransportException (statut HTTP en erreur)
            throw new ProviderUnavailableException("partenaire aerien injoignable : " + e.getMessage());
        }
    }
}
