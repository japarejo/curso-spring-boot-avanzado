package org.springframework.samples.petclinic.apiclient;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

public class FrikiTest {
	
	@Test
	public void consumoStarwars(){
		System.out.println("Me ejecuto");
		RestClient restClient = RestClient.builder().build();
		String response =restClient.get()
			.uri("https://swapi.dev/api/people/1")
			.retrieve()
			.body(String.class);
		
		System.out.println(response);
	}

}
