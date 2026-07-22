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

/**
 * Converts JPA entities to their read-only DTO representation for the REST API (F-05).
 * One-way only: this API is read-only, so no DTO-to-entity direction exists.
 */
final class OwnerMapper {

	private OwnerMapper() {
	}

	static OwnerDto toDto(Owner owner) {
		return new OwnerDto(owner.getId(), owner.getFirstName(), owner.getLastName(), owner.getAddress(),
				owner.getCity(), owner.getTelephone(), owner.getPets().stream().map(OwnerMapper::toDto).toList());
	}

	static PetDto toDto(Pet pet) {
		String typeName = (pet.getType() != null) ? pet.getType().getName() : null;
		return new PetDto(pet.getId(), pet.getName(), pet.getBirthDate(), typeName);
	}

}
