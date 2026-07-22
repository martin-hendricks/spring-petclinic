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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Read-only REST API for owners (F-05). Wraps the existing MVC/Thymeleaf application:
 * {@link OwnerController} keeps serving the HTML views unchanged, this controller adds a
 * JSON path alongside it.
 */
@RestController
@RequestMapping("/api/owners")
@Tag(name = "Owners", description = "Read-only queries over owners and their pets")
class OwnerRestController {

	private static final int DEFAULT_PAGE_SIZE = 5;

	private final OwnerService ownerService;

	OwnerRestController(OwnerService ownerService) {
		this.ownerService = ownerService;
	}

	@GetMapping
	@Operation(summary = "List owners",
			description = "Returns a paginated list of owners, " + "optionally filtered by a last-name prefix.")
	@ApiResponse(responseCode = "200", description = "Owners retrieved")
	ResponseEntity<Page<OwnerDto>> findAll(
			@Parameter(description = "Last name prefix filter; omit to match every owner") @RequestParam(
					required = false) String lastName,
			@Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Page size") @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
		Pageable pageable = PageRequest.of(page, size);
		return ResponseEntity.ok(this.ownerService.findByLastName(lastName, pageable));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get an owner by id")
	@ApiResponse(responseCode = "200", description = "Owner found")
	@ApiResponse(responseCode = "404", description = "No owner exists for the given id",
			content = @Content(schema = @Schema(hidden = true)))
	ResponseEntity<OwnerDto> findById(@PathVariable int id) {
		return ResponseEntity.ok(this.ownerService.findById(id));
	}

}
