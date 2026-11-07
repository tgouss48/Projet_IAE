package ma.mobility.gateway.search;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

// Resultat d'un operateur ; status UNAVAILABLE si l'operateur n'a pas repondu
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SegmentResult(String mode, String from, String to, String status, List<Offer> offers, String error) {

    static SegmentResult ok(String mode, String from, String to, List<Offer> offers) {
        return new SegmentResult(mode, from, to, "OK", offers, null);
    }

    static SegmentResult unavailable(String mode, String from, String to, String error) {
        return new SegmentResult(mode, from, to, "UNAVAILABLE", List.of(), error);
    }
}
