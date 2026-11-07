# gateway

API Gateway / BFF (Spring Cloud Gateway). Seul point d'entree du client.
Port hote : 8080.

## Authentification

```bash
curl -X POST localhost:8080/api/v1/auth/token -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"demo123"}'
# -> {"accessToken":"eyJ...","tokenType":"Bearer","expiresIn":3600}
```

Toutes les autres routes demandent `Authorization: Bearer <token>` (sinon 401).
Comptes de demo : `demo/demo123`, `alice/alice123` (`gateway.users`).
La gateway transmet l'utilisateur aux services dans l'en-tete `X-User-Id`.

## Routes

| Methode | URL | Cible |
|---|---|---|
| POST | `/api/v1/auth/token` | gateway (public) |
| GET | `/api/v1/journeys/search?origin=Casablanca&via=Rabat&destination=Paris&date=2026-11-15` | gateway : rail-bridge + air-bridge + carsharing en parallele |
| POST/GET | `/api/v1/quotes/**` | pricing-service |

Recherche : sans `via`, train et avion sont cherches `origin -> destination` ;
avec `via`, train `origin -> via` puis avion `via -> destination`. La voiture est
cherchee a destination. Chaque operateur a 5 s pour repondre ; s'il echoue, son
segment est renvoye avec `status: UNAVAILABLE` et les autres restent disponibles.
