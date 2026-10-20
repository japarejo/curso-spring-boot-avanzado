package org.springframework.samples.petclinic.web.security;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LoginSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void loginPageIsGeneratedWithCsrfToken() throws Exception {
		mockMvc.perform(get("/login"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("action=\"/login\"")))
			.andExpect(content().string(containsString("name=\"_csrf\"")));
	}

	@ParameterizedTest
	@CsvSource({ "admin1, 4dm1n", "owner1, 0wn3r", "vet1, v3t" })
	void demoUsersCanLogIn(String username, String password) throws Exception {
		mockMvc.perform(formLogin().user(username).password(password))
			.andExpect(authenticated().withUsername(username))
			.andExpect(redirectedUrl("/welcome"));
	}

	@Test
	void anonymousWebRequestRedirectsToLogin() throws Exception {
		mockMvc.perform(get("/owners/1"))
			.andExpect(status().isFound())
			.andExpect(redirectedUrl("http://localhost/login"));
	}
}
