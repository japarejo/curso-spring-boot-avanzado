package org.springframework.samples.petclinic.bills.restcontrollers.billapi;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.bills.model.Bill;
import org.springframework.samples.petclinic.bills.restcontrollers.BadRequestException;
import org.springframework.samples.petclinic.bills.restcontrollers.ResourceNotFoundException;
import org.springframework.samples.petclinic.bills.services.BillService;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Tag(name = "Bills", description = "Billing REST API for invoices and billing operations")
@RestController
@RefreshScope
@RequestMapping("/api/v1/bills")
public class BillAPIController {

	BillService billService;
	@Value("${user.role}")
    private String role;

    
    
	
	@Autowired
	public BillAPIController(BillService billService) {
		super();
		this.billService = billService;
	}
	
@Operation(summary = "Identify whoami", description = "Returns a friendly greeting containing the requested username and configured role.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Greeting returned successfully",
			content = @Content(mediaType = "text/plain")),
		@ApiResponse(responseCode = "400", description = "Invalid username", content = @Content)
	})
	@GetMapping(
	      value = "/whoami/{username}",  
	      produces = MediaType.TEXT_PLAIN_VALUE)
	public String whoami(@Parameter(description = "Username to greet", required = true) @PathVariable("username") String username) {
	    return String.format("Hello! You're %s and you'll become a(n) %s...\n", username, role);
	}

	@Operation(summary = "List all bills", description = "Returns all bills stored in the billing service.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Bills returned successfully",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = Bill.class)))
	})
	@GetMapping()
	public List<Bill> getAllBills(HttpServletRequest request){
		System.out.println(request.getHeader("Authorization"));
		return billService.findAll();
	}
	
	
	@Operation(summary = "Get bill by ID", description = "Returns the bill matching the requested identifier.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Bill returned successfully",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = Bill.class))),
		@ApiResponse(responseCode = "404", description = "Bill not found", content = @Content)
	})
	@GetMapping("/{billId}")
	public Bill getBill(@Parameter(description = "Bill identifier", required = true) @PathVariable("billId") Integer billId) {
		Optional<Bill> bill=billService.findById(billId);
		if(!bill.isPresent())
			throw new ResourceNotFoundException("Bill with ID '"+billId+"' not found!");
		double result=0;
		for(int i=0;i<10000000;i++)
			result+=Math.random();
		return bill.get();		
	}
	
	@Operation(summary = "Create a bill", description = "Creates a new bill and returns the created resource.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "201", description = "Bill created successfully",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = Bill.class))),
		@ApiResponse(responseCode = "400", description = "Invalid bill payload", content = @Content)
	})
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Bill object to create", required = true,
		content = @Content(mediaType = "application/json", schema = @Schema(implementation = Bill.class)))
	@PostMapping()
	public ResponseEntity<Bill> createBill(@RequestBody @Valid Bill bill,BindingResult bindingResult){
		if(bindingResult.hasErrors())
			throw new BadRequestException(bindingResult.getAllErrors().toString());
		else
			billService.save(bill);
		
		URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(bill.getId())
                .toUri();
		
		return ResponseEntity
					.created(location)
					.body(bill);
	}
	
	
	
	@Operation(summary = "Update a bill", description = "Updates an existing bill with the given identifier.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "204", description = "Bill updated successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid bill payload", content = @Content),
		@ApiResponse(responseCode = "404", description = "Bill not found", content = @Content)
	})
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Bill object to update", required = true,
		content = @Content(mediaType = "application/json", schema = @Schema(implementation = Bill.class)))
	@PutMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void modifyBill(@RequestBody @Valid Bill bill, BindingResult bindingResult, @Parameter(description = "Bill identifier", required = true) @PathVariable("id") Integer id){
		
		if(bindingResult.hasErrors())
			throw new BadRequestException(bindingResult.getAllErrors().toString());
		
		Optional<Bill> billToUpdate=billService.findById(id);
		if(billToUpdate.isPresent())
		{
			BeanUtils.copyProperties(bill, billToUpdate.get(), "id");
			billService.save(billToUpdate.get());
		}else
			throw new ResourceNotFoundException("Bill with ID '"+id+"' not found!");
	}
	
	@Operation(summary = "Delete a bill", description = "Deletes the specified bill by identifier.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "204", description = "Bill deleted successfully"),
		@ApiResponse(responseCode = "404", description = "Bill not found", content = @Content)
	})
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteMapping(@Parameter(description = "Bill identifier", required = true) @PathVariable("id") Integer id) {
		
		Optional<Bill> billToUpdate=billService.findById(id);
		
		if(billToUpdate.isPresent())
		{			
			billService.remove(billToUpdate.get());
		}else
			throw new ResourceNotFoundException("Bill with ID '"+id+"' not found!");
	}
}
