--
-- V1: esquema inicial.
--
-- MODULO 6 - Evolucion del esquema con Flyway.
--
-- Las migraciones son INMUTABLES. Si este fichero contiene un error, no se
-- edita: se anade V3__corrige_lo_que_sea.sql. Flyway guarda el hash de cada
-- migracion aplicada en la tabla flyway_schema_history y aborta el arranque
-- si detecta que una ya aplicada ha cambiado.
--
-- Este esquema lo genero Hibernate a partir de las entidades y despues se
-- fijo aqui. Es el camino habitual: prototipar con ddl-auto en desarrollo y
-- congelar el resultado en una migracion en cuanto el modelo se estabiliza.
--

CREATE TABLE users (
  username VARCHAR(64) NOT NULL,
  password VARCHAR(128) NOT NULL,
  enabled BOOLEAN NOT NULL,
  PRIMARY KEY (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE authorities (
  id INT NOT NULL AUTO_INCREMENT,
  version INT,
  username VARCHAR(64) NOT NULL,
  authority VARCHAR(50) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_authorities_users FOREIGN KEY (username) REFERENCES users(username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE vets (
  id INT NOT NULL AUTO_INCREMENT,
  version INT,
  first_name VARCHAR(255),
  last_name VARCHAR(255),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE specialties (
  id INT NOT NULL AUTO_INCREMENT,
  version INT,
  name VARCHAR(50),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE vet_specialties (
  vet_id INT NOT NULL,
  specialty_id INT NOT NULL,
  PRIMARY KEY (vet_id, specialty_id),
  CONSTRAINT fk_vet_specialties_vets FOREIGN KEY (vet_id) REFERENCES vets(id),
  CONSTRAINT fk_vet_specialties_specialties FOREIGN KEY (specialty_id) REFERENCES specialties(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE types (
  id INT NOT NULL AUTO_INCREMENT,
  version INT,
  name VARCHAR(50),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE diseases (
  id INT NOT NULL AUTO_INCREMENT,
  version INT,
  name VARCHAR(50),
  description VARCHAR(1024),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE diseases_pet_typeswith_prevalence (
  disease_id INT NOT NULL,
  pet_typeswith_prevalence_id INT NOT NULL,
  PRIMARY KEY (disease_id, pet_typeswith_prevalence_id),
  CONSTRAINT fk_diseases_pet_types_diseases FOREIGN KEY (disease_id) REFERENCES diseases(id),
  CONSTRAINT fk_diseases_pet_types_types FOREIGN KEY (pet_typeswith_prevalence_id) REFERENCES types(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE owners (
  id INT NOT NULL AUTO_INCREMENT,
  version INT,
  first_name VARCHAR(255),
  last_name VARCHAR(255),
  address VARCHAR(255),
  city VARCHAR(255),
  telephone VARCHAR(255),
  username VARCHAR(64),
  PRIMARY KEY (id),
  UNIQUE KEY uk_owners_username (username),
  CONSTRAINT fk_owners_users FOREIGN KEY (username) REFERENCES users(username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pets (
  id INT NOT NULL AUTO_INCREMENT,
  version INT,
  name VARCHAR(50),
  birth_date DATE,
  age INT,
  type_id INT,
  owner_id INT,
  PRIMARY KEY (id),
  CONSTRAINT fk_pets_types FOREIGN KEY (type_id) REFERENCES types(id),
  CONSTRAINT fk_pets_owners FOREIGN KEY (owner_id) REFERENCES owners(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE visits (
  id INT NOT NULL AUTO_INCREMENT,
  version INT,
  pet_id INT,
  visit_date DATE,
  description VARCHAR(255),
  PRIMARY KEY (id),
  CONSTRAINT fk_visits_pets FOREIGN KEY (pet_id) REFERENCES pets(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE diagnoses (
  id INT NOT NULL AUTO_INCREMENT,
  version INT,
  visit_id INT NOT NULL,
  disease_id INT NOT NULL,
  vet_id INT NOT NULL,
  description VARCHAR(1024),
  PRIMARY KEY (id),
  UNIQUE KEY uk_diagnoses_visit (visit_id),
  CONSTRAINT fk_diagnoses_visits FOREIGN KEY (visit_id) REFERENCES visits(id),
  CONSTRAINT fk_diagnoses_diseases FOREIGN KEY (disease_id) REFERENCES diseases(id),
  CONSTRAINT fk_diagnoses_vets FOREIGN KEY (vet_id) REFERENCES vets(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE payment (
  id INT NOT NULL AUTO_INCREMENT,
  version INT,
  amount DOUBLE NOT NULL,
  owner_id INT,
  creator VARCHAR(255),
  created_date DATETIME(6),
  modifier VARCHAR(255),
  last_modified_date DATETIME(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_payment_owners FOREIGN KEY (owner_id) REFERENCES owners(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
