package ma.mobility.railbridge.api;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import jakarta.validation.Valid;
import ma.mobility.railbridge.corba.RailClient;
import mobility.rail.Trip;
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
public class RailController {

    private final RailClient rail;

    public RailController(RailClient rail) {
        this.rail = rail;
    }

    @GetMapping("/offers")
    public List<Offer> search(@RequestParam String origin,
                              @RequestParam String destination,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Trip[] trips = rail.call(r -> r.search(origin, destination, date.toString()));
        return Arrays.stream(trips).map(TripMapper::toOffer).toList();
    }

    @PostMapping("/holds")
    @ResponseStatus(HttpStatus.CREATED)
    public HoldResponse hold(@Valid @RequestBody HoldRequest request) {
        String tripId = TripMapper.toTripId(request.offerId());
        return new HoldResponse(rail.call(r -> r.hold(tripId, request.quantity())));
    }

    @PostMapping("/holds/{holdId}/confirmation")
    public ConfirmResponse confirm(@PathVariable String holdId) {
        return new ConfirmResponse(rail.call(r -> r.confirm(holdId)));
    }

    // accepte un holdId ou un providerRef, idempotent comme le cancel CORBA
    @DeleteMapping("/reservations/{reference}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable String reference) {
        rail.call(r -> {
            r.cancel(reference);
            return null;
        });
    }
}
