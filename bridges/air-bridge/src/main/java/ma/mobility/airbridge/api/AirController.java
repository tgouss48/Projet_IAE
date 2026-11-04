package ma.mobility.airbridge.api;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import ma.mobility.airbridge.soap.AirClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AirController {

    private final AirClient air;

    public AirController(AirClient air) {
        this.air = air;
    }

    @GetMapping("/offers")
    public List<Offer> search(@RequestParam String origin,
                              @RequestParam String destination,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return air.search(origin, destination, date.toString()).stream().map(FlightMapper::toOffer).toList();
    }

    @PostMapping("/holds")
    @ResponseStatus(HttpStatus.CREATED)
    public HoldResponse hold(@Valid @RequestBody HoldRequest request) {
        String flightId = FlightMapper.toFlightId(request.offerId());
        return new HoldResponse(air.hold(flightId, request.quantity()));
    }

    @PostMapping("/holds/{holdId}/confirmation")
    public ConfirmResponse confirm(@PathVariable String holdId) {
        return new ConfirmResponse(air.confirm(holdId));
    }

    // accepte un holdId ou un providerRef, idempotent comme cancelFlight
    @DeleteMapping("/reservations/{reference}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable String reference) {
        air.cancel(reference);
    }
}
