package org.springframework.samples.petclinic.auth.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.samples.petclinic.auth.model.User;


public interface UserRepository extends  CrudRepository<User, String>{
	User findByUsername(String username);
}
