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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for {@link OwnerRestController}
 */
@WebMvcTest(OwnerRestController.class)
@Import(OwnerService.class)
@DisabledInNativeImage
@DisabledInAotMode
class OwnerRestControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OwnerRepository owners;

	private Owner george;

	@BeforeEach
	void setup() {
		this.george = new Owner();
		this.george.setId(1);
		this.george.setFirstName("George");
		this.george.setLastName("Franklin");
		this.george.setAddress("110 W. Liberty St.");
		this.george.setCity("Madison");
		this.george.setTelephone("6085551023");

		Pet max = new Pet();
		this.george.addPet(max);
		max.setId(1);
		max.setName("Max");
		max.setBirthDate(LocalDate.of(2015, 9, 7));
		PetType dog = new PetType();
		dog.setId(1);
		dog.setName("dog");
		max.setType(dog);

		given(this.owners.findById(1)).willReturn(Optional.of(this.george));
		given(this.owners.findById(999)).willReturn(Optional.empty());
	}

	@Test
	void getByIdShouldReturnOwnerJson() throws Exception {
		this.mockMvc.perform(get("/api/owners/1"))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.firstName").value("George"))
			.andExpect(jsonPath("$.lastName").value("Franklin"))
			.andExpect(jsonPath("$.address").value("110 W. Liberty St."))
			.andExpect(jsonPath("$.city").value("Madison"))
			.andExpect(jsonPath("$.telephone").value("6085551023"))
			.andExpect(jsonPath("$.pets", hasSize(1)))
			.andExpect(jsonPath("$.pets[0].name").value("Max"))
			.andExpect(jsonPath("$.pets[0].type").value("dog"));
	}

	@Test
	void getByIdShouldReturn404WhenOwnerDoesNotExist() throws Exception {
		this.mockMvc.perform(get("/api/owners/999")).andExpect(status().isNotFound());
	}

	@Test
	void findAllShouldFilterByLastName() throws Exception {
		given(this.owners.findByLastNameStartingWith(eq("Franklin"), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(this.george)));

		this.mockMvc.perform(get("/api/owners").param("lastName", "Franklin"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].lastName").value("Franklin"))
			.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void findAllWithoutParamsShouldReturnEveryonePaginated() throws Exception {
		given(this.owners.findByLastNameStartingWith(eq(""), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(this.george)));

		this.mockMvc.perform(get("/api/owners"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void responseShouldNotExposeJpaEntityInternals() throws Exception {
		// "new"/"pets[].visits" only exist on the JPA entity (BaseEntity#isNew,
		// Pet#getVisits); OwnerDto/PetDto must not leak them.
		this.mockMvc.perform(get("/api/owners/1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.new").doesNotExist())
			.andExpect(jsonPath("$.pets[0].visits").doesNotExist())
			.andExpect(jsonPath("$.pets[0].new").doesNotExist());
	}

}
