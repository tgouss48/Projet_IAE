package ma.mobility.booking.api;

import org.springframework.http.HttpStatus;

// Erreur fonctionnelle renvoyee au client au format canonique
public class BookingException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public BookingException(HttpStatus status, String code, String reason) {
        super(reason);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    static BookingException notFound(String bookingId) {
        return new BookingException(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", "Dossier inconnu : " + bookingId);
    }
}
