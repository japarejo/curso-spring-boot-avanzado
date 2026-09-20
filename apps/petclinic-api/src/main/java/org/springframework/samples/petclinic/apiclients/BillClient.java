package org.springframework.samples.petclinic.apiclients;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.samples.petclinic.model.Bill;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import reactor.core.publisher.Mono;


@FeignClient(value = "bills-microservice", url = "${bills.api.url:}")
public interface BillClient {
	@RequestMapping(method = RequestMethod.GET,value = "/api/v1/bills") 
	List<Bill> getBills(@RequestHeader("Authorization") String token);
	
	@RequestMapping(method = RequestMethod.GET,value = "/api/v1/bills")
	Mono<List<Bill>> getBillsAsMono(@RequestHeader("Authorization") String token);
}
