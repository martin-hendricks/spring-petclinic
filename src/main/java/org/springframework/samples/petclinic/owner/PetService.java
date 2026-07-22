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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Business logic for creating and updating {@link Pet}s, extracted out of
 * {@link PetController} so it can be reused (e.g. by a future REST endpoint) without
 * depending on Spring MVC's {@code BindingResult}.
 */
@Service
class PetService {

	private final OwnerRepository owners;

	PetService(OwnerRepository owners) {
		this.owners = owners;
	}

	@Transactional
	Pet createPet(Owner owner, Pet pet) {
		List<FieldViolation> violations = new ArrayList<>();
		validateDuplicateName(owner, pet, violations);
		validateBirthDate(pet, violations);
		if (!violations.isEmpty()) {
			throw new ValidationException(violations);
		}

		owner.addPet(pet);
		this.owners.save(owner);
		return pet;
	}

	@Transactional
	Pet updatePet(Owner owner, Pet pet) {
		List<FieldViolation> violations = new ArrayList<>();
		validateDuplicateName(owner, pet, violations);
		validateBirthDate(pet, violations);
		if (!violations.isEmpty()) {
			throw new ValidationException(violations);
		}

		updatePetDetails(owner, pet);
		return pet;
	}

	/**
	 * Rejects a pet name already used by another pet of the same owner. Uses
	 * {@code pet.isNew()} as the {@code ignoreNew} flag of
	 * {@link Owner#getPet(String, boolean)}: for a brand-new pet this reproduces the
	 * original creation check (compare only against already-persisted pets); for an
	 * existing pet it reproduces the original edit check (compare against every pet,
	 * excluding itself by id).
	 */
	void validateDuplicateName(Owner owner, Pet pet, List<FieldViolation> violations) {
		if (!StringUtils.hasText(pet.getName())) {
			return;
		}
		Pet existingPet = owner.getPet(pet.getName(), pet.isNew());
		if (existingPet != null && !Objects.equals(existingPet.getId(), pet.getId())) {
			violations.add(new FieldViolation("name", "duplicate", "already exists"));
		}
	}

	void validateBirthDate(Pet pet, List<FieldViolation> violations) {
		LocalDate currentDate = LocalDate.now();
		if (pet.getBirthDate() != null && pet.getBirthDate().isAfter(currentDate)) {
			violations.add(new FieldViolation("birthDate", "typeMismatch.birthDate", null));
		}
	}

	/**
	 * Updates the pet details if it exists or adds a new pet to the owner.
	 * @param owner The owner of the pet
	 * @param pet The pet with updated details
	 */
	private void updatePetDetails(Owner owner, Pet pet) {
		Integer id = pet.getId();
		Assert.state(id != null, "'pet.getId()' must not be null");
		Pet existingPet = owner.getPet(id);
		if (existingPet != null) {
			// Update existing pet's properties
			existingPet.setName(pet.getName());
			existingPet.setBirthDate(pet.getBirthDate());
			existingPet.setType(pet.getType());
		}
		else {
			owner.addPet(pet);
		}
		this.owners.save(owner);
	}

}
