package org.springframework.samples.petclinic.web.api.dto;

import com.fasterxml.jackson.annotation.JsonView;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.samples.petclinic.model.Owner;

/**
 * Un unico DTO con tres representaciones, segun la vista activa al serializar.
 *
 * Ojo: Spring Boot configura Jackson con DEFAULT_VIEW_INCLUSION desactivado. Con
 * una vista activa, un campo SIN @JsonView no se serializa nunca. Por eso todos
 * los campos llevan su vista, incluido el id.
 */
@Schema(description = "Owner returned by the views demo. Which fields appear depends on the view.")
public class OwnerResponse {

	@JsonView(VistasPropietario.Publica.class)
	@Schema(description = "Owner identifier.", example = "1")
	private Integer id;

	@JsonView(value={VistasPropietario.Publica.class, VistasPropietario.Custom.class})
	@Schema(example = "George")
	private String firstName;

	@JsonView(VistasPropietario.Publica.class)
	@Schema(example = "Franklin")
	private String lastName;

	@JsonView(VistasPropietario.Publica.class)
	@Schema(example = "Madison")
	private String city;

	@JsonView(VistasPropietario.Recepcion.class)
	@Schema(description = "Only in the Recepcion and Admin views.", example = "110 W. Liberty St.")
	private String address;

	@JsonView(VistasPropietario.Recepcion.class)
	@Schema(description = "Only in the Recepcion and Admin views.", example = "6085551023")
	private String telephone;

	@JsonView(VistasPropietario.Admin.class)
	@Schema(description = "Only in the Admin view. Optimistic locking version.", example = "1")
	private Integer version;

	@JsonView(VistasPropietario.Admin.class)
	@Schema(description = "Only in the Admin view. Login of the owner, if any.", example = "owner1")
	private String username;

	public static OwnerResponse from(Owner owner) {
		OwnerResponse response = new OwnerResponse();
		response.setId(owner.getId());
		response.setFirstName(owner.getFirstName());
		response.setLastName(owner.getLastName());
		response.setCity(owner.getCity());
		response.setAddress(owner.getAddress());
		response.setTelephone(owner.getTelephone());
		response.setVersion(owner.getVersion());
		response.setUsername(owner.getUser() == null ? null : owner.getUser().getUsername());
		return response;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getTelephone() {
		return telephone;
	}

	public void setTelephone(String telephone) {
		this.telephone = telephone;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

}
