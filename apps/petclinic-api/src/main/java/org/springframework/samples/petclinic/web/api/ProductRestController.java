package org.springframework.samples.petclinic.web.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.Product;
import org.springframework.samples.petclinic.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


@Tag(name = "Products", description = "Product management REST API of the vet clinic")
@RestController
@RequestMapping("/api/v1/products") 
public class ProductRestController {
	
	ProductService productService;

	ProductRestController(ProductService productService) {
		this.productService = productService;
	}

	@GetMapping()
	public List<Product> getProducts(@RequestParam (required = false) Double minPrice, @RequestParam (required = false) Double maxPrice, Pageable pageable) {
		List<Product> products = new ArrayList();
		for(Product product : productService.getProducts()) {
			if (minPrice != null && minPrice > 0 && product.getPrice() < minPrice) {
				continue;
			}
			if (maxPrice != null && maxPrice 	> 0 && product.getPrice() > maxPrice) {
				continue;
			}
			products.add(product);
		}
		
		List<Product> paginatedProducts = new ArrayList<>();
		for(int i=0;i<pageable.getPageSize() && i<products.size();i++) {
			paginatedProducts.add(products.get(i));
		}
		
		return paginatedProducts;
	}

	@GetMapping("/{name}")
	public Product getProductByName(@PathVariable String name) {
		Product product=productService.getProductByName(name);
		if(product==null) {
			throw new ResourceNotFoundException("Product not found with name: " + name);
		}	
		return product;
	}

	@PostMapping	
	public ResponseEntity<Product> addProduct(@Valid @RequestBody Product product) {
		productService.addProduct(product);
		
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(product.getName())
			.toUri();
		return ResponseEntity.created(location).body(product);
	}

}
