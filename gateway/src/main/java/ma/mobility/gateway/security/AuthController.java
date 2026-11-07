package ma.mobility.gateway.security;

import java.time.Instant;

import jakarta.validation.Valid;
import ma.mobility.gateway.GatewayProperties;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final ReactiveAuthenticationManager loginManager;
    private final JwtEncoder encoder;
    private final GatewayProperties props;

    public AuthController(ReactiveAuthenticationManager loginManager, JwtEncoder encoder, GatewayProperties props) {
        this.loginManager = loginManager;
        this.encoder = encoder;
        this.props = props;
    }

    @PostMapping("/token")
    public Mono<TokenResponse> token(@Valid @RequestBody LoginRequest request) {
        return loginManager
                .authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()))
                .map(auth -> issue(auth.getName()));
    }

    private TokenResponse issue(String username) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("mobilite-gateway")
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plus(props.tokenTtl()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", props.tokenTtl().toSeconds());
    }
}
