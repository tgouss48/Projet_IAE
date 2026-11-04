package ma.mobility.carsharing.api;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import ma.mobility.carsharing.domain.Reservation;
import ma.mobility.carsharing.domain.ReservationRepository;
import ma.mobility.carsharing.domain.ReservationStatus;
import ma.mobility.carsharing.domain.Vehicle;
import ma.mobility.carsharing.domain.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Meme cycle que les partenaires : hold (temporaire) -> confirm, cancel idempotent
@Service
public class CarsharingService {

    private static final Logger log = LoggerFactory.getLogger(CarsharingService.class);
    static final String PROVIDER = "carsharing";
    static final String CURRENCY = "MAD";

    private final VehicleRepository vehicles;
    private final ReservationRepository reservations;
    private final Clock clock;
    private final Duration holdTtl;

    public CarsharingService(VehicleRepository vehicles, ReservationRepository reservations, Clock clock,
                             @Value("${carsharing.hold-ttl}") Duration holdTtl) {
        this.vehicles = vehicles;
        this.reservations = reservations;
        this.clock = clock;
        this.holdTtl = holdTtl;
    }

    @Transactional(readOnly = true)
    public List<Offer> search(String city, LocalDate date) {
        Instant now = clock.instant();
        return vehicles.findByCityIgnoreCaseOrderById(city).stream()
                .filter(v -> !reservations.isTaken(v.getId(), date, now))
                .map(v -> toOffer(v, date))
                .toList();
    }

    @Transactional
    public String hold(String offerId, int quantity) {
        CarOfferId id = CarOfferId.parse(offerId);
        if (quantity != 1) {
            throw new IllegalArgumentException("une offre d'autopartage correspond a un seul vehicule");
        }
        Vehicle vehicle = vehicles.findByIdForUpdate(id.vehicleId())
                .orElseThrow(() -> new OfferNotAvailableException("Vehicule inconnu : " + id.vehicleId()));
        Instant now = clock.instant();
        if (reservations.isTaken(vehicle.getId(), id.date(), now)) {
            throw new OfferNotAvailableException("Vehicule deja reserve : " + vehicle.getId() + " le " + id.date());
        }
        String holdId = "HOLD-CAR-" + UUID.randomUUID();
        reservations.save(new Reservation(holdId, vehicle.getId(), id.date(), now, now.plus(holdTtl)));
        return holdId;
    }

    @Transactional
    public String confirm(String holdId) {
        Reservation r = reservations.findByHoldId(holdId)
                .orElseThrow(() -> new OfferNotAvailableException("Maintien inconnu : " + holdId));
        // confirm rejoue : on renvoie la meme reference
        if (r.getStatus() == ReservationStatus.CONFIRMED) {
            return r.getBookingRef();
        }
        if (!r.isActiveHold(clock.instant())) {
            throw new OfferNotAvailableException("Maintien expire ou annule : " + holdId);
        }
        r.confirm("BK-CAR-" + UUID.randomUUID());
        return r.getBookingRef();
    }

    // holdId ou bookingRef ; reference inconnue ou deja annulee = pas d'erreur
    @Transactional
    public void cancel(String reference) {
        reservations.findByHoldId(reference)
                .or(() -> reservations.findByBookingRef(reference))
                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                .ifPresent(Reservation::cancel);
    }

    @Scheduled(fixedDelayString = "${carsharing.expiry-check-ms}")
    @Transactional
    public void expireHolds() {
        int n = reservations.expireHolds(clock.instant());
        if (n > 0) {
            log.info("{} maintien(s) expire(s)", n);
        }
    }

    private static Offer toOffer(Vehicle v, LocalDate date) {
        return new Offer(
                new CarOfferId(v.getId(), date).format(),
                CarOfferId.MODE,
                PROVIDER,
                v.getCity(),
                v.getCity(),
                date.atStartOfDay().format(ISO_LOCAL_DATE_TIME),
                date.plusDays(1).atStartOfDay().format(ISO_LOCAL_DATE_TIME),
                new Money(v.getDailyPriceMinor(), CURRENCY),
                1);
    }
}
