package org.springframework.samples.petclinic.apiclient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

public class SWAPITest {
	
	@Test 
	public void consumoStarwars(){
		System.out.println("Me ejecuto");
		RestClient restClient = RestClient.builder().build();
		Character response =restClient.get()
			.uri("https://swapi.dev/api/people/1")
			.retrieve()
			.body(Character.class);
		
		assertEquals("Luke Skywalker", response.getName());
		System.out.println(response);
	}
}
