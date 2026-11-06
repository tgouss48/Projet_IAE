-- remises en points de base (500 = 5 %), montants en centimes de MAD
INSERT INTO pricing_rule (code, rule_value, description) VALUES
    ('DISCOUNT_2_MODES', 500,  'Remise voyage combinant 2 modes de transport'),
    ('DISCOUNT_3_MODES', 1000, 'Remise voyage combinant 3 modes de transport'),
    ('SERVICE_FEE',      1500, 'Frais de service par dossier');
