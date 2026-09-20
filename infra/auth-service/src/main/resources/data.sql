-- Usuarios de demostracion del servicio de autenticacion.
--
-- Las contrasenas van cifradas con BCrypt y con el prefijo {bcrypt} que espera
-- el DelegatingPasswordEncoder. Son los mismos usuarios que petclinic-api, para
-- que el token emitido aqui sirva alli.
--
--   admin1 / 4dm1n  -> admin
--   owner1 / 0wn3r  -> owner
--   vet1   / v3t    -> vet
--
-- El proyecto anterior las guardaba en texto plano con un NoOpPasswordEncoder.
INSERT INTO users(username, password, enabled) VALUES ('admin1', '{bcrypt}$2a$10$3Qh8FLZ2C7NAtrXNQqBULOr8iRUo43dxEl6Vrg3/rPuoB4EXoT0.m', TRUE);
INSERT INTO users(username, password, enabled) VALUES ('owner1', '{bcrypt}$2a$10$AsvJU.GvgyGwcovVRq34cOm0zH5JprJPBwlLqgPpM49MXAPZ2vWjS', TRUE);
INSERT INTO users(username, password, enabled) VALUES ('vet1',   '{bcrypt}$2a$10$w6zmWCWlkgTH5yXyGs3JcOBWm4xc3eqNZWHR77NQ.mjEoWjUE75ya', TRUE);

INSERT INTO authorities(id, version, username, authority) VALUES (1, 1, 'admin1', 'admin');
INSERT INTO authorities(id, version, username, authority) VALUES (2, 1, 'owner1', 'owner');
INSERT INTO authorities(id, version, username, authority) VALUES (3, 1, 'vet1',   'vet');
