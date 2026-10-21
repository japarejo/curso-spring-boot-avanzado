-- Con H2 (perfil por defecto) la tabla la crea Hibernate. Con el perfil mysql,
-- ddl-auto=validate y Flyway: sin esta migracion la aplicacion no arranca.
CREATE TABLE github_accounts (
  github_id     BIGINT      NOT NULL,
  login         VARCHAR(64),
  username      VARCHAR(64) NOT NULL,
  created_at    DATETIME(6) NOT NULL,
  last_login_at DATETIME(6),
  PRIMARY KEY (github_id),
  CONSTRAINT fk_github_accounts_users FOREIGN KEY (username) REFERENCES users (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
