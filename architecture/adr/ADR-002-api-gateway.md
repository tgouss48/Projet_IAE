# ADR-002 — API Gateway avec Spring Cloud Gateway et JWT

- Statut : accepte
- Date : 2026-10-31

## Contexte

Le client doit avoir un point d'entree unique, sans connaitre les services internes
ni les bridges. Il faut authentifier les appels, router vers les microservices et
proposer une recherche qui combine trois operateurs (CORBA, SOAP, REST).

On pourrait aussi passer par un ESB (mediation centralisee, transformations dans le bus).

## Decision

- Spring Cloud Gateway (reactif) comme gateway / BFF
- la recherche multimodale est un endpoint de la gateway (BFF) qui appelle les trois
  operateurs en parallele (`Mono.zip`), avec un timeout de 5 s par operateur
- un operateur en echec ne fait pas echouer la recherche : son segment est marque
  `UNAVAILABLE` (degradation partielle)
- authentification JWT signee HS256, emise par la gateway (`/api/v1/auth/token`) ;
  l'identite est transmise aux services dans `X-User-Id` (en-tete client ecrase)

## Consequences

- les transformations de protocole restent dans les bridges, la gateway ne fait que
  router et agreger (« smart endpoints, dumb pipes »), contrairement a un ESB
- temps de reponse de la recherche = l'operateur le plus lent, pas la somme
- la gateway est un point de passage unique : elle doit rester simple et sans etat
- limite : les utilisateurs sont en configuration ; en production on deleguerait a un
  serveur d'identite (Keycloak, OAuth2) et on signerait en RS256
