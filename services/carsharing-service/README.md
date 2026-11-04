# carsharing-service

Microservice d'autopartage. Base PostgreSQL dediee (`carsharing-db`), schema et
vehicules geres par Flyway (`src/main/resources/db/migration`).

Port hote : 8093.

Une offre = un vehicule dans une ville pour une journee :
`CARSHARING:<vehicule>:<date>`, ex. `CARSHARING:VEH-PAR-001:2026-11-15`.

| Methode | URL | Corps / reponse |
|---|---|---|
| GET | `/api/v1/offers?city=Paris&date=2026-11-15` | `Offer[]` (vehicules libres) |
| POST | `/api/v1/holds` | `{"offerId":"CARSHARING:VEH-PAR-001:2026-11-15","quantity":1}` -> 201 `{"holdId":...}` |
| POST | `/api/v1/holds/{holdId}/confirmation` | `{"providerRef":...}` |
| DELETE | `/api/v1/reservations/{holdId ou providerRef}` | 204, idempotent |

Meme cycle que les partenaires :
- un hold expire apres 10 minutes (`carsharing.hold-ttl`), une tache planifiee passe
  les holds expires en `EXPIRED`
- confirm rejoue : renvoie la meme `providerRef`
- cancel d'une reference inconnue ou deja annulee : pas d'erreur
- deux holds simultanes sur le meme vehicule : le vehicule est verrouille
  (`SELECT ... FOR UPDATE`), un seul passe

Tests : `CarsharingServiceTest` (H2 en mode PostgreSQL, horloge de test pour l'expiration).
