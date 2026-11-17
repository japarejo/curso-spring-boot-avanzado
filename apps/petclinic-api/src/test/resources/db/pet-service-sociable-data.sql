-- Datos especificos para PetServiceSociableTests.
-- Ids altos para no chocar con los de data.sql.
-- Se ejecuta dentro de la transaccion de cada prueba, asi que se deshace
-- (rollback) junto con lo que haga la prueba.
INSERT INTO owners(id, version, first_name, last_name, address, city, telephone)
	VALUES (1000, 1, 'Ana', 'Pruebas', 'Calle Falsa 123', 'Sevilla', '600000000');

INSERT INTO pets(id, version, name, birth_date, type_id, owner_id)
	VALUES (1000, 1, 'Toby', '2020-01-15', 2, 1000);
INSERT INTO pets(id, version, name, birth_date, type_id, owner_id)
	VALUES (1001, 1, 'Kira', '2021-06-30', 1, 1000);
