-- Datos de ejemplo del servicio de facturas.
-- Los identificadores de visita se corresponden con los de data.sql de
-- petclinic-api, para que la demostracion del modulo 2 (comparativa de
-- clientes HTTP) devuelva algo coherente.
INSERT INTO bill(id, version, visit_id, amount, concept) VALUES (1, 1, 1, 45.00, 'Consulta general');
INSERT INTO bill(id, version, visit_id, amount, concept) VALUES (2, 1, 2, 120.50, 'Radiografia y revision');
INSERT INTO bill(id, version, visit_id, amount, concept) VALUES (3, 1, 3, 80.00, 'Castracion');
INSERT INTO bill(id, version, visit_id, amount, concept) VALUES (4, 1, 4, 65.25, 'Esterilizacion');
