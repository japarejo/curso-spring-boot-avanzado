package org.springframework.samples.petclinic.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@AutoConfigureMockMvc 
public class DummyControllerTest {
	
	@Autowired 
	MockMvc mockMvc;

	@Test
	@WithMockUser (username = "japarejo", authorities = {"owner"}) 
	public void authenticatedUsersCanGetPets() throws Exception {
		mockMvc.perform(get("/api/pets")).
				andExpect(status().isOk()).	// 200 STATUS CODE.
				andExpect(jsonPath("$[0].name")
				              .value("Leo")).
				andExpect(jsonPath("$[0].type")
				              .value("cat"));	// El gato Leo es el primer pet del array de datos.
	}

	@Test	
	public void unauthenticatedUsersCanNotGetPets() throws Exception {
		mockMvc.perform(get("/api/pets")).
				andExpect(status().isUnauthorized());	// 401 STATUS CODE.
	}
}
