package org.springframework.samples.petclinic.acl.controller;

import java.security.Principal;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.samples.petclinic.acl.model.ClinicalRecord;
import org.springframework.samples.petclinic.acl.model.RecordForm;
import org.springframework.samples.petclinic.acl.service.AclPermissionService;
import org.springframework.samples.petclinic.acl.service.ClinicalRecordService;

@Controller
public class ClinicalRecordController {

	private static final List<String> SHAREABLE_USERS = List.of("ana", "bruno", "carla");

	private final ClinicalRecordService recordService;
	private final AclPermissionService aclPermissionService;

	public ClinicalRecordController(ClinicalRecordService recordService, AclPermissionService aclPermissionService) {
		this.recordService = recordService;
		this.aclPermissionService = aclPermissionService;
	}

	@GetMapping("/")
	String home() {
		return "redirect:/records";
	}

	@GetMapping("/login")
	String login() {
		return "login";
	}

	@GetMapping("/records")
	String records(Model model, Authentication authentication) {
		List<ClinicalRecord> records = recordService.findVisibleRecords();
		model.addAttribute("records", records);
		model.addAttribute("writableRecordIds", records.stream()
				.filter(record -> aclPermissionService.canWrite(authentication, record))
				.map(ClinicalRecord::getId)
				.toList());
		model.addAttribute("username", authentication.getName());
		model.addAttribute("roles", authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.toList());
		return "records";
	}

	@GetMapping("/records/new")
	String newRecord(Model model) {
		model.addAttribute("recordForm", new RecordForm());
		model.addAttribute("mode", "create");
		return "record-form";
	}

	@PostMapping("/records")
	String createRecord(
			@Valid @ModelAttribute RecordForm recordForm,
			BindingResult bindingResult,
			Principal principal,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("mode", "create");
			return "record-form";
		}
		ClinicalRecord created = recordService.createRecord(recordForm, principal.getName());
		redirectAttributes.addFlashAttribute("message", "Registro creado con ACL de propietario para " + principal.getName());
		return "redirect:/records/" + created.getId();
	}

	@GetMapping("/records/{id}")
	String record(@PathVariable Long id, Model model, Authentication authentication) {
		ClinicalRecord record = recordService.findReadableRecord(id);
		model.addAttribute("record", record);
		model.addAttribute("canWrite", aclPermissionService.canWrite(authentication, record));
		model.addAttribute("canAdmin", aclPermissionService.canAdmin(authentication, record));
		model.addAttribute("shareableUsers", SHAREABLE_USERS);
		return "record-detail";
	}

	@GetMapping("/records/{id}/edit")
	String editRecord(@PathVariable Long id, Model model) {
		ClinicalRecord record = recordService.findWritableRecord(id);
		RecordForm form = new RecordForm();
		form.setPetName(record.getPetName());
		form.setDiagnosis(record.getDiagnosis());
		form.setTreatmentPlan(record.getTreatmentPlan());
		model.addAttribute("record", record);
		model.addAttribute("recordForm", form);
		model.addAttribute("mode", "edit");
		return "record-form";
	}

	@PostMapping("/records/{id}")
	String updateRecord(
			@PathVariable Long id,
			@Valid @ModelAttribute RecordForm recordForm,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("record", recordService.findReadableRecord(id));
			model.addAttribute("mode", "edit");
			return "record-form";
		}
		recordService.updateRecord(id, recordForm);
		redirectAttributes.addFlashAttribute("message", "Registro actualizado.");
		return "redirect:/records/" + id;
	}

	@PostMapping("/records/{id}/share/read")
	String grantRead(@PathVariable Long id, String username, RedirectAttributes redirectAttributes) {
		recordService.grantRead(id, username);
		redirectAttributes.addFlashAttribute("message", username + " ya puede leer este registro.");
		return "redirect:/records/" + id;
	}

	@PostMapping("/records/{id}/share/write")
	String grantWrite(@PathVariable Long id, String username, RedirectAttributes redirectAttributes) {
		recordService.grantWrite(id, username);
		redirectAttributes.addFlashAttribute("message", username + " ya puede editar este registro.");
		return "redirect:/records/" + id;
	}

	@ExceptionHandler(AccessDeniedException.class)
	String denied(Model model) {
		model.addAttribute("error", "Spring Security ACL ha denegado la operacion en la capa de servicio.");
		return "access-denied";
	}

}
