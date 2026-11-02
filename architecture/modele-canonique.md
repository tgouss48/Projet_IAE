# Modele canonique — v0

Format pivot utilise par la plateforme (gateway, saga, microservices). Les bridges
traduisent les formats des partenaires vers ce modele : aucun type CORBA ou SOAP
ne sort de la couche d'integration.

Conventions :
- JSON, noms en camelCase
- montants en centimes (`amountMinor`) + devise, jamais de flottant
- dates en ISO-8601 sans fuseau (`2026-11-15T07:30:00`), heure locale du lieu
- date de recherche au format `AAAA-MM-JJ`

## 1. Offer (offre de transport)

Resultat d'une recherche, quel que soit l'operateur.

| Champ | Type | Description |
|---|---|---|
| offerId | string | `<MODE>:<id natif>`, ex. `TRAIN:RAIL-CAS-RAB-0730` |
| mode | enum | `TRAIN`, `FLIGHT`, `CARSHARING` |
| provider | string | `rail-operator`, `air-partner`, `carsharing` |
| origin | string | ville de depart |
| destination | string | ville d'arrivee |
| departure | datetime | depart |
| arrival | datetime | arrivee |
| price | Money | prix unitaire (par place / par vehicule) |
| availability | int | places ou vehicules disponibles |

`Money` = `{ "amountMinor": 8000, "currency": "MAD" }`

Le prefixe de `offerId` permet a la saga de savoir vers quel bridge router
`hold` / `confirm` / `cancel` sans exposer l'operateur au client.

```json
{
  "offerId": "TRAIN:RAIL-CAS-RAB-0730",
  "mode": "TRAIN",
  "provider": "rail-operator",
  "origin": "Casablanca",
  "destination": "Rabat",
  "departure": "2026-11-15T07:30:00",
  "arrival": "2026-11-15T08:25:00",
  "price": { "amountMinor": 8000, "currency": "MAD" },
  "availability": 120
}
```

## 2. Booking (dossier de voyage)

Une reservation multimodale = un dossier qui contient plusieurs segments (legs).

| Champ | Type | Description |
|---|---|---|
| bookingId | string | identifiant du dossier (UUID) |
| customerId | string | client authentifie |
| status | enum | voir cycle de vie |
| passengers | int | nombre de places demandees |
| legs | BookingLeg[] | segments ordonnes |
| totalPrice | Money | calcule par le service tarification |
| createdAt | datetime | creation |

### BookingLeg

| Champ | Type | Description |
|---|---|---|
| legId | string | identifiant du segment |
| offerId | string | offre reservee |
| mode | enum | `TRAIN`, `FLIGHT`, `CARSHARING` |
| quantity | int | places (train, avion) ou vehicules (autopartage) |
| holdId | string | renvoye par `hold`, null avant |
| providerRef | string | `bookingRef` renvoye par `confirm`, null avant |
| status | enum | `PENDING`, `HELD`, `CONFIRMED`, `CANCELLED`, `FAILED` |
| failureReason | string | raison de l'echec eventuel |

```json
{
  "bookingId": "6f1c2a9e-...",
  "customerId": "u-123",
  "status": "CONFIRMED",
  "passengers": 2,
  "legs": [
    { "legId": "1", "offerId": "TRAIN:RAIL-CAS-RAB-0730", "mode": "TRAIN", "quantity": 2,
      "holdId": "HOLD-RAIL-...", "providerRef": "BK-RAIL-...", "status": "CONFIRMED" },
    { "legId": "2", "offerId": "FLIGHT:AIR-RBA-ORY-1200", "mode": "FLIGHT", "quantity": 2,
      "holdId": "HOLD-AIR-...", "providerRef": "BK-AIR-...", "status": "CONFIRMED" }
  ],
  "totalPrice": { "amountMinor": 376000, "currency": "MAD" },
  "createdAt": "2026-11-01T10:00:00"
}
```

### Cycle de vie du dossier

```
PENDING -> HOLDING -> CONFIRMING -> CONFIRMED
              |            |
              v            v
         COMPENSATING -> CANCELLED
```

- `HOLDING` : hold en cours sur chaque leg
- `CONFIRMING` : tous les holds obtenus, confirm en cours
- `COMPENSATING` : un hold ou confirm a echoue, cancel des legs deja engages
- `CANCELLED` : compensation terminee (ou annulation demandee par le client)

## 3. Erreur canonique

Les erreurs metier des partenaires sont ramenees a un seul format :

```json
{ "code": "OFFER_NOT_AVAILABLE", "reason": "Plus assez de places sur AIR-CMN-MAD-1800 (demande 5, disponible 3)" }
```

| Code | Origine |
|---|---|
| OFFER_NOT_AVAILABLE | exception CORBA `NotAvailable`, SOAP Fault de hold/confirm, refus autopartage |
| PROVIDER_UNAVAILABLE | partenaire injoignable, timeout, circuit ouvert |

## 4. Correspondances

### Train (CORBA `mobility::rail::Trip`)

| Rail | Canonique |
|---|---|
| tripId | offerId = `TRAIN:` + tripId |
| origin / destination | origin / destination |
| departure / arrival | departure / arrival |
| priceMinor (long) | price.amountMinor, currency `MAD` |
| seatsAvailable | availability |
| hold / confirm / cancel | holdId / providerRef / annulation |
| NotAvailable.reason | erreur `OFFER_NOT_AVAILABLE` |

### Avion (SOAP `Flight`, namespace `http://mobility.ma/air`)

| Air | Canonique |
|---|---|
| flightId | offerId = `FLIGHT:` + flightId |
| origin / destination | origin / destination |
| departure / arrival | departure / arrival |
| priceMinor | price.amountMinor, currency `MAD` |
| seatsAvailable | availability |
| holdFlight / confirmFlight / cancelFlight | holdId / providerRef / annulation |
| SOAP Fault (faultstring) | erreur `OFFER_NOT_AVAILABLE` |
| cancelFlight.status | ignore (cancel idempotent, pas d'erreur) |

### Autopartage (microservice interne, REST)

Expose directement le modele canonique (`mode = CARSHARING`). Une offre correspond
a un vehicule disponible dans une ville : `origin` = `destination` = ville de prise,
`departure` / `arrival` = debut / fin de location, `availability` = vehicules libres.

## 5. Limites de la v0

- pas de fuseau horaire : les deux partenaires renvoient des heures locales sans offset
- devise unique MAD
- pas de modele voyageur detaille (nom, documents), seulement un nombre de places
- les heures de correspondance (train -> avion) ne sont pas encore verifiees
