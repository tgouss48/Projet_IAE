package ma.mobility.railbridge.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// Erreurs au format canonique
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(OfferNotAvailableException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ApiError notAvailable(OfferNotAvailableException e) {
        return new ApiError("OFFER_NOT_AVAILABLE", e.getMessage());
    }

    @ExceptionHandler(ProviderUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    ApiError providerUnavailable(ProviderUnavailableException e) {
        return new ApiError("PROVIDER_UNAVAILABLE", e.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class,
            MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError invalid(Exception e) {
        return new ApiError("INVALID_REQUEST", e.getMessage());
    }
}
