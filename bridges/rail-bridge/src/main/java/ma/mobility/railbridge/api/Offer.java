package ma.mobility.railbridge.api;

// Offre au format canonique (voir architecture/modele-canonique.md)
public record Offer(
        String offerId,
        String mode,
        String provider,
        String origin,
        String destination,
        String departure,
        String arrival,
        Money price,
        int availability) {
}
