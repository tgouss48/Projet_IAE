# ADR-001 — Modele canonique comme couche anti-corruption

- Statut : accepte
- Date : 2026-10-31

## Contexte

La plateforme integre trois systemes aux modeles differents : CORBA (structs IDL,
exception `NotAvailable`), SOAP (XML JAXB, SOAP Fault) et nos microservices REST.
Les systemes fournis ne peuvent pas etre modifies. Si les types CORBA ou SOAP
remontent jusqu'a la gateway, chaque changement chez un partenaire impacte toute
la plateforme et le client devient dependant des protocoles legacy.

## Decision

On definit un modele canonique unique (`Offer`, `Booking`, `BookingLeg`, `Money`,
erreur canonique) decrit dans `architecture/modele-canonique.md`.

- chaque bridge traduit son protocole vers ce modele et inversement
- la saga, les microservices et la gateway ne manipulent que ce modele
- `offerId` porte le mode en prefixe (`TRAIN:`, `FLIGHT:`, `CARSHARING:`) pour
  router les operations vers le bon bridge
- les montants restent en centimes (`long`) comme chez les partenaires

## Consequences

- le client ne voit jamais de CORBA ni de SOAP
- ajouter un operateur = ecrire un bridge, sans toucher a la saga ni a la gateway
- une traduction supplementaire par appel (cout negligeable ici)
- le modele doit rester stable : toute evolution est versionnee (v0, v1...)
