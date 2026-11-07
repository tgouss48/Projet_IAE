package ma.mobility.gateway.security;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
