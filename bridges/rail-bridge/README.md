# rail-bridge

Bridge CORBA -> REST vers l'operateur ferroviaire. Expose le modele canonique
(`architecture/modele-canonique.md`), aucun type CORBA ne sort du service.

Port hote : 8091.

| Methode | URL | Corps / reponse |
|---|---|---|
| GET | `/api/v1/offers?origin=Casablanca&destination=Rabat&date=2026-11-15` | `Offer[]` |
| POST | `/api/v1/holds` | `{"offerId":"TRAIN:RAIL-CAS-RAB-0730","quantity":2}` -> 201 `{"holdId":...}` |
| POST | `/api/v1/holds/{holdId}/confirmation` | `{"providerRef":...}` |
| DELETE | `/api/v1/reservations/{holdId ou providerRef}` | 204, idempotent |

Erreurs : 409 `OFFER_NOT_AVAILABLE` (exception `NotAvailable`), 503
`PROVIDER_UNAVAILABLE` (serveur injoignable), 400 `INVALID_REQUEST`.

Comportement :
- la reference est lue depuis `ior/RailBooking.ior` au premier appel
- si l'appel echoue avec une exception systeme CORBA `COMPLETED_NO` (serveur redemarre,
  IOR change), le bridge relit l'IOR et rejoue une fois. Si l'appel a pu etre execute
  (`COMPLETED_MAYBE`), on ne rejoue pas pour ne pas faire un double hold
- timeouts JacORB : connexion 3 s, reponse 5 s, pas de reprise interne
