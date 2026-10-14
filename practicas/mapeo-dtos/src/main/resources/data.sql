INSERT INTO usuario (username, password) VALUES ('george', '{noop}george');

INSERT INTO propietario (nombre, apellidos, direccion, ciudad, telefono, version, usuario_username)
VALUES ('George', 'Franklin', '110 W. Liberty St.', 'Madison', '6085551023', 0, 'george');
INSERT INTO propietario (nombre, apellidos, direccion, ciudad, telefono, version, usuario_username)
VALUES ('Betty', 'Davis', '638 Cardinal Ave.', 'Sun Prairie', '6085551749', 0, NULL);

INSERT INTO mascota (nombre, fecha_nacimiento, propietario_id) VALUES ('Leo', '2020-09-07', 1);
INSERT INTO mascota (nombre, fecha_nacimiento, propietario_id) VALUES ('Basil', '2022-08-06', 1);
INSERT INTO mascota (nombre, fecha_nacimiento, propietario_id) VALUES ('Rosy', '2021-04-17', 2);
