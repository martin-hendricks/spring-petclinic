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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only query service backing the REST API (F-05). The Thymeleaf UI keeps using
 * {@link OwnerRepository} directly through {@link OwnerController}; this service is a
 * separate, additive read path for the JSON API.
 */
@Service
@Transactional(readOnly = true)
class OwnerService {

	private final OwnerRepository owners;

	OwnerService(OwnerRepository owners) {
		this.owners = owners;
	}

	OwnerDto findById(int id) {
		return this.owners.findById(id).map(OwnerMapper::toDto).orElseThrow(() -> new OwnerNotFoundException(id));
	}

	/**
	 * Mirrors {@code OwnerController#processFindForm}: a {@code null} last name means the
	 * broadest possible search (empty prefix matches everyone).
	 */
	Page<OwnerDto> findByLastName(String lastName, Pageable pageable) {
		String prefix = (lastName != null) ? lastName : "";
		return this.owners.findByLastNameStartingWith(prefix, pageable).map(OwnerMapper::toDto);
	}

}
