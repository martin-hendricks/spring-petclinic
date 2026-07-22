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

import java.util.List;

/**
 * Thrown by the {@code @Service} layer when one or more business rules are violated.
 * Carries every {@link FieldViolation} found in a single validation pass so a caller can
 * report them all at once, matching the pre-refactor behaviour of the MVC controllers.
 */
class ValidationException extends RuntimeException {

	private final List<FieldViolation> violations;

	ValidationException(List<FieldViolation> violations) {
		this.violations = violations;
	}

	List<FieldViolation> getViolations() {
		return this.violations;
	}

}
