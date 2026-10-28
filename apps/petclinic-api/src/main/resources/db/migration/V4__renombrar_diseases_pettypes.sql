-- Disease.petTypeswithPrevalence declara ahora @JoinTable(name = "diseases_pettypes").
-- Sin esta migracion, con el perfil mysql (ddl-auto=validate) la aplicacion no
-- arranca porque Hibernate no encuentra la tabla con el nombre nuevo.
-- Nunca se edita V1: Flyway detectaria el cambio de checksum en las bases ya migradas.
RENAME TABLE diseases_pet_typeswith_prevalence TO diseases_pettypes;

-- Mismo dato que data.sql (H2): Leo tiene edad 14, lo usa PetRepository.findByAge.
UPDATE pets SET age = 14 WHERE id = 1;
