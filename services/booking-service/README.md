# booking-service

Reservations multimodales et orchestrateur de saga. Base PostgreSQL dediee
(`booking-db`), evenements publies sur Kafka (topic `booking-events`, cle = bookingId).

Port hote : 8095 (en temps normal on passe par la gateway).

| Methode | URL | Description |
|---|---|---|
| POST | `/api/v1/bookings` | `{"quoteId":"Q-..."}` -> 202, dossier `PENDING`. En-tete optionnel `Idempotency-Key` |
| GET | `/api/v1/bookings/{id}` | etat du dossier et de chaque segment |
| GET | `/api/v1/bookings` | dossiers du client |
| DELETE | `/api/v1/bookings/{id}` | annulation d'un dossier `CONFIRMED` -> 202 `COMPENSATING` |

Le client est identifie par `X-User-Id` (pose par la gateway).

## Saga

```
PENDING -> HOLDING -> CONFIRMING -> CONFIRMED
              |            |
              v            v
         COMPENSATING -> CANCELLED
```

1. `POST /bookings` lit le devis (pricing-service), enregistre le dossier et
   l'evenement `BOOKING_REQUESTED` dans la meme transaction (outbox)
2. `OutboxRelay` publie l'evenement sur Kafka ; `SagaListener` le consomme et lance
   `SagaOrchestrator.run()`
3. hold de chaque segment dans l'ordre ; si un hold echoue -> compensation
4. confirm de chaque segment ; si un confirm echoue ou si les holds ont plus de 9 min
   -> compensation
5. compensation : cancel en ordre inverse de tout ce qui a ete engage (3 essais par
   cancel). Si un operateur reste injoignable, le dossier reste `COMPENSATING` et
   `CompensationRetryJob` reessaie toutes les 30 s

Chaque etape est sauvegardee dans sa propre transaction avec son evenement. Apres un
crash, `run()` reprend a l'etat enregistre ; le rappeler sur un dossier termine ne fait rien.

Evenements : `BOOKING_REQUESTED`, `LEG_HELD`, `LEG_HOLD_FAILED`, `LEG_CONFIRMED`,
`LEG_CONFIRM_FAILED`, `BOOKING_CONFIRMED`, `COMPENSATION_STARTED`, `LEG_CANCELLED`,
`BOOKING_CANCELLED`, `BOOKING_CANCEL_REQUESTED`.

Tests : `SagaOrchestratorTest` (faux operateurs) couvre le chemin nominal, l'echec au
hold, l'echec au confirm, la saga rejouee, la compensation interrompue puis reprise,
l'annulation client et l'Idempotency-Key.
