package ma.mobility.booking.api;

import ma.mobility.booking.provider.ProviderUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Erreurs au format canonique
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(BookingException.class)
    ResponseEntity<ApiError> booking(BookingException e) {
        return ResponseEntity.status(e.getStatus()).body(new ApiError(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(ProviderUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    ApiError unavailable(ProviderUnavailableException e) {
        return new ApiError("PROVIDER_UNAVAILABLE", e.getMessage());
    }

    // X-User-Id est pose par la gateway apres verification du token
    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    ApiError unauthenticated(MissingRequestHeaderException e) {
        return new ApiError("UNAUTHENTICATED", "utilisateur non identifie");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError invalid(MethodArgumentNotValidException e) {
        return new ApiError("INVALID_REQUEST", e.getMessage());
    }
}
