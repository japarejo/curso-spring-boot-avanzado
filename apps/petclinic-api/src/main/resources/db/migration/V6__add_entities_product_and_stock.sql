-- Entidades Product y Stock (inventario de productos para mascotas).
--
-- Product pasa a ser @Entity en este mismo cambio, asi que tambien hay que
-- crear su tabla y la de union del @ManyToMany con PetType (tabla "types").
-- Nombres por defecto de Hibernate con la estrategia de Spring (snake_case):
--   Product                -> product
--   Product.petTypes       -> product_pet_types (product_id, pet_types_id)
--   Stock                  -> stock
--   Stock.product          -> product_id
--   Stock.quantity         -> howmuch (@Column(name = "howmuch"))
--
-- Orden: product antes que las tablas que la referencian.

CREATE TABLE product (
  id INT NOT NULL AUTO_INCREMENT,
  version INT,
  name VARCHAR(255),
  -- @Size(max = 200) es validacion de Bean Validation; aqui la reforzamos en BD.
  description VARCHAR(200),
  price DOUBLE,
  PRIMARY KEY (id),
  CONSTRAINT ck_product_price CHECK (price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE product_pet_types (
  product_id INT NOT NULL,
  pet_types_id INT NOT NULL,
  CONSTRAINT fk_product_pet_types_product FOREIGN KEY (product_id) REFERENCES product (id),
  CONSTRAINT fk_product_pet_types_type FOREIGN KEY (pet_types_id) REFERENCES types (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE stock (
  id INT NOT NULL AUTO_INCREMENT,
  product_id INT NOT NULL,
  howmuch INT NOT NULL,
  PRIMARY KEY (id),
  -- @OneToOne: un producto tiene como mucho una fila de stock.
  CONSTRAINT uk_stock_product UNIQUE (product_id),
  CONSTRAINT fk_stock_product FOREIGN KEY (product_id) REFERENCES product (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
