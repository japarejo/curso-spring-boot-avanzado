--
-- V2: datos de referencia y usuarios de demostracion.
--
-- Mismo contenido que data.sql, que es el que se usa con H2 en desarrollo y en
-- las pruebas. Se mantienen los dos por una razon deliberada:
--
--   H2 (perfil por defecto)  ddl-auto=create-drop + data.sql
--       Arranque instantaneo, base limpia en cada ejecucion. Es lo que quieres
--       en clase y en las pruebas.
--
--   MySQL (perfiles mysql y docker)  Flyway + ddl-auto=validate
--       Esquema versionado, reproducible y con historial. Es lo que quieres en
--       cualquier entorno que sobreviva a un reinicio.
--
-- Si los dos se separan, el sintoma aparece en las pruebas de Testcontainers,
-- que corren contra MySQL real. Ese es justamente uno de los ejercicios del
-- modulo 6: detectar la divergencia y decidir que hacer con ella.
--

-- Usuarios de demostracion. Las contrasenas se guardan cifradas con BCrypt
-- y con el prefijo {bcrypt} que espera el DelegatingPasswordEncoder.
--   admin1          / 4dm1n   -> autoridad admin
--   owner1..owner10 / 0wn3r   -> autoridad owner
--   vet1            / v3t     -> autoridad vet
INSERT INTO users(username,password,enabled) VALUES ('admin1','{bcrypt}$2a$10$3Qh8FLZ2C7NAtrXNQqBULOr8iRUo43dxEl6Vrg3/rPuoB4EXoT0.m',TRUE);
INSERT INTO authorities(id,version,username,authority) VALUES (1,1,'admin1','admin');
INSERT INTO users(username,password,enabled) VALUES ('owner1','{bcrypt}$2a$10$AsvJU.GvgyGwcovVRq34cOm0zH5JprJPBwlLqgPpM49MXAPZ2vWjS',TRUE);
INSERT INTO authorities(id,version,username,authority) VALUES (11,1,'owner1','owner');
INSERT INTO users(username,password,enabled) VALUES ('owner2','{bcrypt}$2a$10$AsvJU.GvgyGwcovVRq34cOm0zH5JprJPBwlLqgPpM49MXAPZ2vWjS',TRUE);
INSERT INTO authorities(id,version,username,authority) VALUES (2,1,'owner2','owner');
INSERT INTO users(username,password,enabled) VALUES ('owner3','{bcrypt}$2a$10$AsvJU.GvgyGwcovVRq34cOm0zH5JprJPBwlLqgPpM49MXAPZ2vWjS',TRUE);
INSERT INTO authorities(id,version,username,authority) VALUES (3,1,'owner3','owner');
INSERT INTO users(username,password,enabled) VALUES ('owner4','{bcrypt}$2a$10$AsvJU.GvgyGwcovVRq34cOm0zH5JprJPBwlLqgPpM49MXAPZ2vWjS',TRUE);
INSERT INTO authorities(id,version,username,authority) VALUES (4,1,'owner4','owner');
INSERT INTO users(username,password,enabled) VALUES ('owner5','{bcrypt}$2a$10$AsvJU.GvgyGwcovVRq34cOm0zH5JprJPBwlLqgPpM49MXAPZ2vWjS',TRUE);
INSERT INTO authorities(id,version,username,authority) VALUES (5,1,'owner5','owner');
INSERT INTO users(username,password,enabled) VALUES ('owner6','{bcrypt}$2a$10$AsvJU.GvgyGwcovVRq34cOm0zH5JprJPBwlLqgPpM49MXAPZ2vWjS',TRUE);
INSERT INTO authorities(id,version,username,authority) VALUES (6,1,'owner6','owner');
INSERT INTO users(username,password,enabled) VALUES ('owner7','{bcrypt}$2a$10$AsvJU.GvgyGwcovVRq34cOm0zH5JprJPBwlLqgPpM49MXAPZ2vWjS',TRUE);
INSERT INTO authorities(id,version,username,authority) VALUES (7,1,'owner7','owner');
INSERT INTO users(username,password,enabled) VALUES ('owner8','{bcrypt}$2a$10$AsvJU.GvgyGwcovVRq34cOm0zH5JprJPBwlLqgPpM49MXAPZ2vWjS',TRUE);
INSERT INTO authorities(id,version,username,authority) VALUES (8,1,'owner8','owner');
INSERT INTO users(username,password,enabled) VALUES ('owner9','{bcrypt}$2a$10$AsvJU.GvgyGwcovVRq34cOm0zH5JprJPBwlLqgPpM49MXAPZ2vWjS',TRUE);
INSERT INTO authorities(id,version,username,authority) VALUES (9,1,'owner9','owner');
INSERT INTO users(username,password,enabled) VALUES ('owner10','{bcrypt}$2a$10$AsvJU.GvgyGwcovVRq34cOm0zH5JprJPBwlLqgPpM49MXAPZ2vWjS',TRUE);
INSERT INTO authorities(id,version,username,authority) VALUES (10,1,'owner10','owner');
INSERT INTO users(username,password,enabled) VALUES ('vet1','{bcrypt}$2a$10$w6zmWCWlkgTH5yXyGs3JcOBWm4xc3eqNZWHR77NQ.mjEoWjUE75ya',TRUE);
INSERT INTO authorities(id,version,username,authority) VALUES (12,1,'vet1','vet');

INSERT INTO vets(id,version,first_name,last_name) VALUES (1,1, 'James', 'Carter');
INSERT INTO vets(id,version,first_name,last_name) VALUES (2,1, 'Helen', 'Leary');
INSERT INTO vets(id,version,first_name,last_name) VALUES (3,1, 'Linda', 'Douglas');
INSERT INTO vets(id,version,first_name,last_name) VALUES (4,1, 'Rafael', 'Ortega');
INSERT INTO vets(id,version,first_name,last_name) VALUES (5,1, 'Henry', 'Stevens');
INSERT INTO vets(id,version,first_name,last_name) VALUES (6,1, 'Sharon', 'Jenkins');

INSERT INTO specialties(id,version,name) VALUES (1,1, 'radiology');
INSERT INTO specialties(id,version,name) VALUES (2,1, 'surgery');
INSERT INTO specialties(id,version,name) VALUES (3,1, 'dentistry');

INSERT INTO vet_specialties(vet_id,specialty_id) VALUES (2, 1);
INSERT INTO vet_specialties(vet_id,specialty_id) VALUES (3, 2);
INSERT INTO vet_specialties(vet_id,specialty_id) VALUES (3, 3);
INSERT INTO vet_specialties(vet_id,specialty_id) VALUES (4, 2);
INSERT INTO vet_specialties(vet_id,specialty_id) VALUES (5, 1);

INSERT INTO types(id,version,name) VALUES (1,1, 'cat');
INSERT INTO types(id,version,name) VALUES (2,1, 'dog');
INSERT INTO types(id,version,name) VALUES (3,1, 'lizard');
INSERT INTO types(id,version,name) VALUES (4,1, 'snake');
INSERT INTO types(id,version,name) VALUES (5,1, 'bird');
INSERT INTO types(id,version,name) VALUES (6,1, 'hamster');

INSERT INTO owners(id, version, first_name, last_name,address, city, telephone, username) VALUES 
 (1,1, 'George', 'Franklin', '110 W. Liberty St.', 'Madison', '6085551023', 'owner1'),
 (2,1, 'Betty', 'Davis', '638 Cardinal Ave.', 'Sun Prairie', '6085551749', 'owner2'),
 (3,1, 'Eduardo', 'Rodriquez', '2693 Commerce St.', 'McFarland', '6085558763', 'owner3'),
 (4,1, 'Harold', 'Davis', '563 Friendly St.', 'Windsor', '6085553198', 'owner4'),
 (5,1, 'Peter', 'McTavish', '2387 S. Fair Way', 'Madison', '6085552765', 'owner5'),
 (6,1, 'Jean', 'Coleman', '105 N. Lake St.', 'Monona', '6085552654', 'owner6'),
 (7,1, 'Jeff', 'Black', '1450 Oak Blvd.', 'Monona', '6085555387', 'owner7'),
 (8,1, 'Maria', 'Escobito', '345 Maple St.', 'Madison', '6085557683', 'owner8'),
 (9,1, 'David', 'Schroeder', '2749 Blackhawk Trail', 'Madison', '6085559435', 'owner9'),
 (10,1, 'Carlos', 'Estaban', '2335 Independence La.', 'Waunakee', '6085555487', 'owner10');

INSERT INTO pets(id,version,name,birth_date,type_id,owner_id) VALUES (1,1, 'Leo', '2010-09-07', 1, 1);
INSERT INTO pets(id,version,name,birth_date,type_id,owner_id) VALUES (2,1, 'Basil', '2012-08-06', 6, 2);
INSERT INTO pets(id,version,name,birth_date,type_id,owner_id) VALUES (3,1, 'Rosy', '2011-04-17', 2, 3);
INSERT INTO pets(id,version,name,birth_date,type_id,owner_id) VALUES (4,1, 'Jewel', '2010-03-07', 2, 3);
INSERT INTO pets(id,version,name,birth_date,type_id,owner_id) VALUES (5,1, 'Iggy', '2010-11-30', 3, 4);
INSERT INTO pets(id,version,name,birth_date,type_id,owner_id) VALUES (6,1, 'George', '2010-01-20', 4, 5);
INSERT INTO pets(id,version,name,birth_date,type_id,owner_id) VALUES (7,1, 'Samantha', '2012-09-04', 1, 6);
INSERT INTO pets(id,version,name,birth_date,type_id,owner_id) VALUES (8,1, 'Max', '2012-09-04', 1, 6);
INSERT INTO pets(id,version,name,birth_date,type_id,owner_id) VALUES (9,1, 'Lucky', '2011-08-06', 5, 7);
INSERT INTO pets(id,version,name,birth_date,type_id,owner_id) VALUES (10,1, 'Mulligan', '2007-02-24', 2, 8);
INSERT INTO pets(id,version,name,birth_date,type_id,owner_id) VALUES (11,1, 'Freddy', '2010-03-09', 5, 9);
INSERT INTO pets(id,version,name,birth_date,type_id,owner_id) VALUES (12,1, 'Lucky', '2010-06-24', 2, 10);
INSERT INTO pets(id,version,name,birth_date,type_id,owner_id) VALUES (13,1, 'Sly', '2012-06-08', 1, 10);

INSERT INTO visits(id,version,pet_id,visit_date,description) VALUES (1,1, 7, '2013-01-01', 'rabies shot');
INSERT INTO visits(id,version,pet_id,visit_date,description) VALUES (2,1, 8, '2013-01-02', 'rabies shot');
INSERT INTO visits(id,version,pet_id,visit_date,description) VALUES (3,1, 8, '2013-01-03', 'neutered');
INSERT INTO visits(id,version,pet_id,visit_date,description) VALUES (4,1, 7, '2013-01-04', 'spayed');

INSERT INTO diseases(id,version,name,description) VALUES (1,1,'COVID-19','Es una ‎enfermedad infecciosacausada por un ‎coronavirus recientemente. ‎De acuerdo a los Centros para el Control y la Prevención de Enfermedades de los Estados Unidos, algunas mascotas — incluyendo perros y gatos — también se han infectado con el virus que causa la COVID-19. ‎Sin embargo, en base a la información limitada que existe, se considera poco el riesgo de que los animales trasmitan la COVID-19 a la gente.');
INSERT INTO diseases(id,version,name,description) VALUES (2,1,'Diabetes','La diabetes en perros es una enfermedad compleja causada por la falta de insulina o la respuesta inadecuada de esta. Cuando la mascota come, su sistema digestivo rompe los alimentos en varios componentes, incluyendo la glucosa, que es transportada a las células por la insulina, una hormona que segrega el páncreas. Cuando el animal no produce insulina o no puede utilizarla con normalidad, sus niveles de azúcar en sangre se elevan. El resultado es la hiperglucemia que si no se trata puede causar complicaciones.');

INSERT INTO diseases_pet_typeswith_prevalence(DISEASE_ID,PET_TYPESWITH_PREVALENCE_ID) VALUES (2,1);
INSERT INTO diseases_pet_typeswith_prevalence(DISEASE_ID,PET_TYPESWITH_PREVALENCE_ID) VALUES (2,2);
INSERT INTO diseases_pet_typeswith_prevalence(DISEASE_ID,PET_TYPESWITH_PREVALENCE_ID) VALUES (1,2);

INSERT INTO diagnoses(id,version,visit_id,disease_id,vet_id,description) VALUES (1,1,1,2,2,'La mascota presenta problemas de vista y comportamiento extraño. El dueño afirma alimentarlo regularmente con dulces y chucherías :-S.');
INSERT INTO diagnoses(id,version,visit_id,disease_id,vet_id,description) VALUES (2,1,2,1,1,'La mascota presenta fiebre, pérdida de apetito, y dificultad respiratoria.');
