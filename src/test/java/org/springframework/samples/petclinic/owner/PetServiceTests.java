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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test class for {@link PetService}, mocking {@link OwnerRepository} so it runs without a
 * Spring context.
 */
@ExtendWith(MockitoExtension.class)
class PetServiceTests {

	@Mock
	private OwnerRepository owners;

	private PetService petService;

	@BeforeEach
	void setup() {
		this.petService = new PetService(this.owners);
	}

	@Test
	void shouldRejectDuplicateNameOnCreate() {
		Owner owner = new Owner();
		Pet existingPet = new Pet();
		owner.addPet(existingPet);
		existingPet.setId(1);
		existingPet.setName("Leo");

		Pet newPet = new Pet();
		newPet.setName("Leo");
		newPet.setBirthDate(LocalDate.now().minusYears(1));

		ValidationException ex = assertThrows(ValidationException.class,
				() -> this.petService.createPet(owner, newPet));

		assertThat(ex.getViolations()).extracting(FieldViolation::field, FieldViolation::code)
			.containsExactly(tuple("name", "duplicate"));
	}

	@Test
	void shouldAcceptDuplicateNameOnUpdateWhenItIsTheSamePet() {
		Owner owner = new Owner();
		// The MVC layer binds submitted form data directly onto the pet instance
		// already inside the owner's collection, so "pet" and "existingPet" are the
		// same object here, exactly like in production.
		Pet pet = new Pet();
		owner.addPet(pet);
		pet.setId(1);
		pet.setName("Leo");
		pet.setBirthDate(LocalDate.now().minusYears(1));

		Pet result = this.petService.updatePet(owner, pet);

		assertThat(result).isSameAs(pet);
		verify(this.owners).save(owner);
	}

	@Test
	void shouldRejectFutureBirthDate() {
		Owner owner = new Owner();
		Pet pet = new Pet();
		pet.setName("Leo");
		pet.setBirthDate(LocalDate.now().plusDays(1));

		ValidationException ex = assertThrows(ValidationException.class, () -> this.petService.createPet(owner, pet));

		assertThat(ex.getViolations()).extracting(FieldViolation::field, FieldViolation::code)
			.containsExactly(tuple("birthDate", "typeMismatch.birthDate"));
	}

	@Test
	void shouldSaveOwnerExactlyOnceOnSuccessfulCreate() {
		Owner owner = new Owner();
		Pet pet = new Pet();
		pet.setName("Leo");
		pet.setBirthDate(LocalDate.now().minusYears(1));

		Pet result = this.petService.createPet(owner, pet);

		assertThat(result).isSameAs(pet);
		assertThat(owner.getPets()).contains(pet);
		verify(this.owners, times(1)).save(owner);
	}

}
