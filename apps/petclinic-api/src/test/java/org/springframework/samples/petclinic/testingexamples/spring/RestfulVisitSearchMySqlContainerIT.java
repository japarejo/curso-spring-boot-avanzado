package org.springframework.samples.petclinic.testingexamples.spring;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
	"spring.jpa.hibernate.ddl-auto=create-drop",
	"spring.jpa.defer-datasource-initialization=true",
	"spring.sql.init.mode=always",
	"spring.sql.init.platform=mysql",
	"spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
@DisplayName("@SpringBootTest + MockMvc + MySQL real usando Testcontainers")
class RestfulVisitSearchMySqlContainerIT extends MySqlTestContainerSupport {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("Busca visitas pasando por controlador, servicio, repositorio y MySQL")
	void shouldSearchVisitsThroughHttpLayerUsingRealMySql() throws Exception {
		this.mockMvc.perform(get("/api/v1/visits")
				.param("ownerLastName", "Coleman")
				.param("petName", "Samantha")
				.param("petType", "cat")
				.param("description", "rabies")
				.param("size", "5")
				.param("sort", "date,asc")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].description").value("rabies shot"))
			.andExpect(jsonPath("$.content[0].pet.name").value("Samantha"))
			.andExpect(jsonPath("$.content[0].pet.type").value("cat"))
			.andExpect(jsonPath("$.content[0].owner.lastName").value("Coleman"))
			.andExpect(jsonPath("$.page.totalElements").value(1))
			.andExpect(jsonPath("$.filters.ownerLastName").value("Coleman"))
			.andExpect(jsonPath("$.sort").value("date,asc"));
	}

}
