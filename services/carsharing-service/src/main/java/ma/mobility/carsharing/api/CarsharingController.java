package ma.mobility.carsharing.api;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
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

// Meme API que les bridges, sauf la recherche qui se fait par ville
@RestController
@RequestMapping("/api/v1")
public class CarsharingController {

    private final CarsharingService service;

    public CarsharingController(CarsharingService service) {
        this.service = service;
    }

    @GetMapping("/offers")
    public List<Offer> search(@RequestParam String city,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.search(city, date);
    }

    @PostMapping("/holds")
    @ResponseStatus(HttpStatus.CREATED)
    public HoldResponse hold(@Valid @RequestBody HoldRequest request) {
        return new HoldResponse(service.hold(request.offerId(), request.quantity()));
    }

    @PostMapping("/holds/{holdId}/confirmation")
    public ConfirmResponse confirm(@PathVariable String holdId) {
        return new ConfirmResponse(service.confirm(holdId));
    }

    @DeleteMapping("/reservations/{reference}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable String reference) {
        service.cancel(reference);
    }
}
