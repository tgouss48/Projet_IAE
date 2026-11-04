package ma.mobility.carsharing.api;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

// offerId = "CARSHARING:<vehicule>:<date>", ex. "CARSHARING:VEH-PAR-001:2026-11-15"
public record CarOfferId(String vehicleId, LocalDate date) {

    public static final String MODE = "CARSHARING";
    static final String PREFIX = MODE + ":";

    public static CarOfferId parse(String offerId) {
        if (offerId == null || !offerId.startsWith(PREFIX)) {
            throw new IllegalArgumentException("offerId invalide pour l'autopartage : " + offerId);
        }
        String[] parts = offerId.substring(PREFIX.length()).split(":");
        if (parts.length != 2 || parts[0].isBlank()) {
            throw new IllegalArgumentException("offerId invalide pour l'autopartage : " + offerId);
        }
        try {
            return new CarOfferId(parts[0], LocalDate.parse(parts[1]));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("date invalide dans l'offerId : " + offerId);
        }
    }

    public String format() {
        return PREFIX + vehicleId + ":" + date;
    }
}
