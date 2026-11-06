# pricing-service

Microservice de tarification : calcule le prix total d'un voyage multimodal et
enregistre le devis. Base PostgreSQL dediee (`pricing-db`), migrations Flyway.

Port hote : 8094.

| Methode | URL | Corps / reponse |
|---|---|---|
| POST | `/api/v1/quotes` | `{"legs":[{"offerId","mode","unitPrice":{"amountMinor","currency"},"quantity"}]}` -> 201 devis |
| GET | `/api/v1/quotes/{quoteId}` | devis, 404 `QUOTE_NOT_FOUND` |

Regles (table `pricing_rule`, modifiables sans redeploiement) :

| Code | Valeur | Effet |
|---|---|---|
| DISCOUNT_2_MODES | 500 | -5 % si le voyage combine 2 modes |
| DISCOUNT_3_MODES | 1000 | -10 % si 3 modes |
| SERVICE_FEE | 1500 | +15 MAD par dossier |

Total = somme(prix unitaire x quantite) - remise + frais. Calcul en centimes
(`long`), remise arrondie au centime. Un devis est valable 10 minutes, comme les holds.

Le service ne rappelle pas les operateurs : il calcule a partir des prix des offres
fournis par l'appelant (gateway / saga), qui viennent de la recherche.
