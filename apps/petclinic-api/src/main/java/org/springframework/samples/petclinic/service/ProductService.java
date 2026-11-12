package org.springframework.samples.petclinic.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.model.Product;
import org.springframework.stereotype.Service;


@Service 
public class ProductService {

	List<Product> products;
	
	public ProductService() {
		initializeProducts();
	}

	// Method to initialize the list of products after object creation
	public void initializeProducts() {
		// Implementation for initializing products
		products = new ArrayList<>();
		products.add(new Product("Dog Food","", 20.0,List.of()));
		products.add(new Product( "Cat Toy","", 9.99,List.of()));
		products.add(new Product("Bird Cage","", 49.99,List.of()));
		PetType dogType = new PetType();
		dogType.setName("Dog");
		PetType catType = new PetType();
		catType.setName("Cat");
		products.get(0).setPetTypes(List.of(dogType));
		products.get(1).setPetTypes(List.of(catType));
		products.get(2).setPetTypes(List.of(catType,dogType));
	}

	public List<Product> getProducts() {
		return products;
	}


	public Product addProduct(Product product) {
		products.add(product);
		return product;
	}

	public Product getProductByName(String name) {
		return products.stream()
				.filter(product -> product.getName().equalsIgnoreCase(name))
				.findFirst()
				.orElse(null);
	}

}
