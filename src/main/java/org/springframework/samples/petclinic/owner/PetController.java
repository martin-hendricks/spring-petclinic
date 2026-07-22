/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.owner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Wick Dynex
 */
@Controller
@RequestMapping("/owners/{ownerId}")
class PetController {

	private static final String VIEWS_PETS_CREATE_OR_UPDATE_FORM = "pets/createOrUpdatePetForm";

	private final OwnerRepository owners;

	private final PetTypeRepository types;

	private final PetService petService;

	public PetController(OwnerRepository owners, PetTypeRepository types, PetService petService) {
		this.owners = owners;
		this.types = types;
		this.petService = petService;
	}

	@ModelAttribute("types")
	public Collection<PetType> populatePetTypes() {
		return this.types.findPetTypes();
	}

	@ModelAttribute("owner")
	public Owner findOwner(@PathVariable("ownerId") int ownerId) {
		return findOwnerOrThrow(ownerId);
	}

	@ModelAttribute("pet")
	public Pet findPet(@PathVariable("ownerId") int ownerId,
			@PathVariable(name = "petId", required = false) Integer petId) {
		return (petId == null) ? new Pet() : findOwnerOrThrow(ownerId).getPet(petId);
	}

	private Owner findOwnerOrThrow(int ownerId) {
		Optional<Owner> optionalOwner = this.owners.findById(ownerId);
		return optionalOwner.orElseThrow(() -> new IllegalArgumentException(
				"Owner not found with id: " + ownerId + ". Please ensure the ID is correct "));
	}

	@InitBinder("owner")
	public void initOwnerBinder(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id", "*.id");
	}

	@InitBinder("pet")
	public void initPetBinder(WebDataBinder dataBinder) {
		dataBinder.setValidator(new PetValidator());
		dataBinder.setDisallowedFields("id", "*.id");
	}

	@GetMapping("/pets/new")
	public String initCreationForm(Owner owner, ModelMap model) {
		Pet pet = new Pet();
		owner.addPet(pet);
		return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping("/pets/new")
	public String processCreationForm(Owner owner, @Valid Pet pet, BindingResult result,
			RedirectAttributes redirectAttributes) {
		return validateAndSave(owner, pet, result, redirectAttributes, () -> this.petService.createPet(owner, pet),
				"New Pet has been Added");
	}

	@GetMapping("/pets/{petId}/edit")
	public String initUpdateForm() {
		return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping("/pets/{petId}/edit")
	public String processUpdateForm(Owner owner, @Valid Pet pet, BindingResult result,
			RedirectAttributes redirectAttributes) {
		return validateAndSave(owner, pet, result, redirectAttributes, () -> this.petService.updatePet(owner, pet),
				"Pet details has been edited");
	}

	/**
	 * Runs the service's business-rule checks unconditionally (so they combine with any
	 * pre-existing {@code @Valid} errors in the same round trip, matching the
	 * pre-refactor behaviour), then persists only if the combined result has no errors at
	 * all.
	 */
	private String validateAndSave(Owner owner, Pet pet, BindingResult result, RedirectAttributes redirectAttributes,
			Runnable persistAction, String successMessage) {
		List<FieldViolation> violations = new ArrayList<>();
		this.petService.validateDuplicateName(owner, pet, violations);
		this.petService.validateBirthDate(pet, violations);
		violations.forEach(v -> result.rejectValue(v.field(), v.code(), v.defaultMessage()));

		if (result.hasErrors()) {
			return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
		}
		try {
			persistAction.run();
		}
		catch (ValidationException ex) {
			ex.getViolations().forEach(v -> result.rejectValue(v.field(), v.code(), v.defaultMessage()));
			return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
		}
		redirectAttributes.addFlashAttribute("message", successMessage);
		return "redirect:/owners/{ownerId}";
	}

}
