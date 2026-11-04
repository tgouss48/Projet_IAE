# air-bridge

Bridge SOAP -> REST vers le partenaire aerien. Meme API REST que `rail-bridge`,
au format canonique (`architecture/modele-canonique.md`).

Port hote : 8092.

| Methode | URL | Corps / reponse |
|---|---|---|
| GET | `/api/v1/offers?origin=Rabat&destination=Paris&date=2026-11-15` | `Offer[]` |
| POST | `/api/v1/holds` | `{"offerId":"FLIGHT:AIR-RBA-ORY-1200","quantity":2}` -> 201 `{"holdId":...}` |
| POST | `/api/v1/holds/{holdId}/confirmation` | `{"providerRef":...}` |
| DELETE | `/api/v1/reservations/{holdId ou providerRef}` | 204, idempotent |

Erreurs : 409 `OFFER_NOT_AVAILABLE` (SOAP Fault), 503 `PROVIDER_UNAVAILABLE`
(erreur reseau ou HTTP), 400 `INVALID_REQUEST`.

Notes :
- classes JAXB generees depuis `src/main/resources/wsdl/air.wsdl` (copie du WSDL publie)
- le WSDL ne declare pas de fault typee : toutes les faults renvoient
  `SOAP-ENV:Server` + un texte. Une fault sur hold/confirm est donc traitee comme
  `OFFER_NOT_AVAILABLE`
- le `status` de `cancelFlight` (`NOT_FOUND`...) n'est pas une erreur
- timeouts HTTP : connexion 3 s, lecture 5 s
