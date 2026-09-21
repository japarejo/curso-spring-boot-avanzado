INSERT INTO internal_users(id,version,username,email,display_name,password,enabled) VALUES (1,1,'admin1','admin1@petclinic.local','Admin One','4dm1n',TRUE);
INSERT INTO internal_authorities(id,version,user_id,authority) VALUES (1,1,1,'admin');

INSERT INTO internal_users(id,version,username,email,display_name,password,enabled) VALUES (2,1,'owner1','owner1@petclinic.local','Owner One','0wn3r',TRUE);
INSERT INTO internal_authorities(id,version,user_id,authority) VALUES (2,1,2,'owner');

INSERT INTO internal_users(id,version,username,email,display_name,password,enabled) VALUES (3,1,'vet1','vet1@petclinic.local','Vet One','v3t',TRUE);
INSERT INTO internal_authorities(id,version,user_id,authority) VALUES (3,1,3,'veterinarian');

INSERT INTO external_identities(id,version,user_id,provider,provider_user_id,email,display_name,last_login_at) VALUES (1,1,2,'google','google-owner1','owner1@petclinic.local','Owner One',NULL);
INSERT INTO external_identities(id,version,user_id,provider,provider_user_id,email,display_name,last_login_at) VALUES (2,1,2,'github','github-owner1','owner1@petclinic.local','Owner One',NULL);

ALTER TABLE internal_users ALTER COLUMN id RESTART WITH 4;
ALTER TABLE internal_authorities ALTER COLUMN id RESTART WITH 4;
ALTER TABLE external_identities ALTER COLUMN id RESTART WITH 3;
