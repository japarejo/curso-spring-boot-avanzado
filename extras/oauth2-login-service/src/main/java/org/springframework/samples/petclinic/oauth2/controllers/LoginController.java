package org.springframework.samples.petclinic.oauth2.controllers;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

	@Operation(summary = "Login form", description = "HTML form that authenticates with local username and password through Spring Security form login.")
	@GetMapping("/login")
	public String loginForm(@RequestParam(required = false) String error, Model model) {
		model.addAttribute("hasError", error != null);
		return "login";
	}
}
