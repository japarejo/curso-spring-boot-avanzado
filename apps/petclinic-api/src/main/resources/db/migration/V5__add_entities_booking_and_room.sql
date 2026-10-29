-- Entidades Room y Booking (reservas de habitaciones para mascotas).
--
-- Sin "drop table if exists": una migracion crea o altera, nunca borra lo que
-- pudiera haber. Si la tabla ya existe, es mejor que Flyway falle y avise.
--
-- room va antes que booking porque booking la referencia.

CREATE TABLE room (
  id INT NOT NULL AUTO_INCREMENT,
  version INT,
  name VARCHAR(50),
  capacidad INT NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT ck_room_capacidad CHECK (capacidad >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE booking (
  id INT NOT NULL AUTO_INCREMENT,
  version INT,
  -- LocalDateTime -> DATETIME(6). TIMESTAMP convertiria a UTC con la zona de
  -- la sesion y no admite fechas posteriores a 2038.
  start_date DATETIME(6) NOT NULL,
  end_date DATETIME(6) NOT NULL,
  pet_id INT NOT NULL,
  room_id INT NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_booking_pet FOREIGN KEY (pet_id) REFERENCES pets (id),
  CONSTRAINT fk_booking_room FOREIGN KEY (room_id) REFERENCES room (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
