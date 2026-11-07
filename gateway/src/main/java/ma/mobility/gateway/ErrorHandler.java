package ma.mobility.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;

// Erreurs au format canonique
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    ApiError badCredentials(BadCredentialsException e) {
        return new ApiError("INVALID_CREDENTIALS", "identifiant ou mot de passe incorrect");
    }

    @ExceptionHandler({IllegalArgumentException.class, WebExchangeBindException.class, ServerWebInputException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError invalid(Exception e) {
        return new ApiError("INVALID_REQUEST", e.getMessage());
    }
}
