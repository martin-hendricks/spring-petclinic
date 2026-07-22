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
 * Test class for {@link VisitService}, mocking {@link OwnerRepository} so it runs without
 * a Spring context.
 */
@ExtendWith(MockitoExtension.class)
class VisitServiceTests {

	@Mock
	private OwnerRepository owners;

	private VisitService visitService;

	private Owner owner;

	private Pet pet;

	@BeforeEach
	void setup() {
		this.visitService = new VisitService(this.owners);
		this.owner = new Owner();
		this.pet = new Pet();
		this.owner.addPet(this.pet);
		this.pet.setId(1);
	}

	@Test
	void shouldRejectVisitDateInThePast() {
		Visit visit = new Visit();
		visit.setDate(LocalDate.now().minusDays(1));

		ValidationException ex = assertThrows(ValidationException.class,
				() -> this.visitService.createVisit(this.owner, this.pet.getId(), visit));

		assertThat(ex.getViolations()).extracting(FieldViolation::field, FieldViolation::code)
			.containsExactly(tuple("date", "typeMismatch.visitDate"));
	}

	@Test
	void shouldRejectVisitDateOfToday() {
		Visit visit = new Visit();
		visit.setDate(LocalDate.now());

		assertThrows(ValidationException.class,
				() -> this.visitService.createVisit(this.owner, this.pet.getId(), visit));
	}

	@Test
	void shouldSaveOwnerExactlyOnceOnSuccessfulVisit() {
		Visit visit = new Visit();
		visit.setDate(LocalDate.now().plusDays(1));

		Visit result = this.visitService.createVisit(this.owner, this.pet.getId(), visit);

		assertThat(result).isSameAs(visit);
		assertThat(this.pet.getVisits()).contains(visit);
		verify(this.owners, times(1)).save(this.owner);
	}

}
