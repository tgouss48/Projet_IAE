package ma.mobility.gateway.search;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/journeys")
public class JourneySearchController {

    private final JourneySearchService service;

    public JourneySearchController(JourneySearchService service) {
        this.service = service;
    }

    @GetMapping("/search")
    public Mono<JourneySearchResponse> search(@RequestParam String origin,
                                              @RequestParam(required = false) String via,
                                              @RequestParam String destination,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.search(origin, via, destination, date);
    }
}
