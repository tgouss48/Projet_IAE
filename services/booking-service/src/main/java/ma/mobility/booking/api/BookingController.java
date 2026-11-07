package ma.mobility.booking.api;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    static final String USER = "X-User-Id";

    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    // 202 : la saga tourne en arriere-plan, le client suit l'etat avec GET
    @PostMapping
    public ResponseEntity<BookingResponse> create(@RequestHeader(USER) String user,
                                                  @RequestHeader(value = "Idempotency-Key", required = false) String key,
                                                  @Valid @RequestBody CreateBookingRequest request) {
        BookingResponse body = BookingResponse.from(service.create(user, request.quoteId(), key));
        return ResponseEntity.accepted().location(URI.create("/api/v1/bookings/" + body.bookingId())).body(body);
    }

    @GetMapping("/{bookingId}")
    public BookingResponse get(@RequestHeader(USER) String user, @PathVariable String bookingId) {
        return BookingResponse.from(service.get(user, bookingId));
    }

    @GetMapping
    public List<BookingResponse> list(@RequestHeader(USER) String user) {
        return service.list(user).stream().map(BookingResponse::from).toList();
    }

    @DeleteMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> cancel(@RequestHeader(USER) String user, @PathVariable String bookingId) {
        return ResponseEntity.accepted().body(BookingResponse.from(service.cancel(user, bookingId)));
    }
}
