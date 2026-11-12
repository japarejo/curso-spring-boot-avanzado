package org.springframework.samples.petclinic.web.api;

import java.util.Optional;

import org.springframework.samples.petclinic.model.Visit;
import org.springframework.samples.petclinic.service.VisitService;
import org.springframework.samples.petclinic.web.api.dto.FacturaDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;



@RestController 
@RequestMapping("/api/facturas")
@Tag (name = "Facturas", description = "Factura management REST API")
public class FacturaRestController {

	VisitService visitService;

	FacturaRestController(VisitService visitService) {
		this.visitService = visitService;
	}

	@PostMapping()
	public void createFactura(@RequestBody @Valid FacturaDTO facturaDTO) {
		System.out.println("Factura recibida: " + facturaDTO);
		System.out.println("Visit: " + facturaDTO.getVisit().toString());

	}
}
