# ADR-003 — Saga orchestree, asynchrone, avec outbox Kafka

- Statut : accepte
- Date : 2026-10-31

## Contexte

Une reservation engage trois operateurs (train CORBA, avion SOAP, autopartage REST).
Une transaction ACID distribuee est impossible : chaque operateur est transactionnel
chez lui, sans coordinateur commun (pas de 2PC). Les trois exposent le meme cycle
hold / confirm / cancel, ou cancel est idempotent et les holds expirent apres 10 min.

## Decision

1. **Orchestration plutot que choregraphie.** Un orchestrateur unique
   (`booking-service`) pilote les etapes. Les bridges CORBA/SOAP sont synchrones par
   nature ; en choregraphie, il faudrait leur ajouter des consommateurs Kafka et la
   logique de compensation serait repartie entre plusieurs services. Avec un
   orchestrateur, l'etat de la saga est a un seul endroit, lisible et testable.

2. **Demande asynchrone.** `POST /bookings` repond 202 tout de suite ; la saga tourne
   en arriere-plan, declenchee par l'evenement Kafka `BOOKING_REQUESTED`. Le client
   suit l'etat avec `GET`. Le client n'attend pas trois operateurs lents.

3. **Outbox transactionnelle.** Chaque changement d'etat et son evenement sont
   enregistres dans la meme transaction ; un relais les publie ensuite sur Kafka.
   Pas d'etat sauvegarde sans evenement, ni l'inverse. Kafka peut etre arrete un moment
   sans perte.

4. **Reprise et idempotence.**
   - etat de la saga persiste a chaque etape : `run()` peut etre rejoue
   - cancel rejoue jusqu'a 3 fois, puis repris par un job planifie
   - hold jamais rejoue (un doublon bloquerait des places) ; un hold orphelin
     expire tout seul chez l'operateur apres 10 min
   - pas de confirm apres 9 min de maintien
   - `Idempotency-Key` sur la creation de dossier

## Consequences

- coherence a terme : pendant la saga, un dossier peut avoir un train bloque et
  un vol pas encore demande ; l'etat final est soit tout confirme, soit tout annule
- l'outbox peut publier un evenement deux fois : les consommateurs dedoublonnent
  sur `eventId`
- limite : si un confirm part en timeout sans reponse, on ne sait pas s'il a ete
  execute ; on annule avec le holdId, ce que tous les operateurs ne garantissent pas
  de traiter comme l'annulation d'une reservation ferme
- un seul orchestrateur : le relais d'outbox suppose une seule instance (sinon il
  faudrait `SELECT ... FOR UPDATE SKIP LOCKED`)
