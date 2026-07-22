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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for booking {@link Visit}s, extracted out of {@link VisitController} so
 * it can be reused without depending on Spring MVC's {@code BindingResult}.
 */
@Service
class VisitService {

	private final OwnerRepository owners;

	VisitService(OwnerRepository owners) {
		this.owners = owners;
	}

	@Transactional
	Visit createVisit(Owner owner, int petId, Visit visit) {
		List<FieldViolation> violations = new ArrayList<>();
		validateVisitDate(visit, violations);
		if (!violations.isEmpty()) {
			throw new ValidationException(violations);
		}

		owner.addVisit(petId, visit);
		this.owners.save(owner);
		return visit;
	}

	void validateVisitDate(Visit visit, List<FieldViolation> violations) {
		if (visit.getDate() != null && !visit.getDate().isAfter(LocalDate.now())) {
			violations.add(new FieldViolation("date", "typeMismatch.visitDate", null));
		}
	}

}
