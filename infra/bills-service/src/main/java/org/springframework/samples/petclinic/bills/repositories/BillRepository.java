package org.springframework.samples.petclinic.bills.repositories;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.samples.petclinic.bills.model.Bill;


public interface BillRepository extends CrudRepository<Bill,Integer> {

	public List<Bill> findAll();
}
