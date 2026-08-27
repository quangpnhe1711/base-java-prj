package com.luvina.base.template.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.luvina.base.core.dto.DeleteBaseRequest;
import com.luvina.base.template.dto.request.SampleCreateRequest;
import com.luvina.base.template.dto.request.SampleSearchRequest;
import com.luvina.base.template.dto.request.SampleUpdateRequest;
import com.luvina.base.template.dto.response.SamplePageResponse;
import com.luvina.base.template.dto.response.SampleResponse;
import com.luvina.base.template.service.SampleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * HTTP entry point for samples.
 *
 * <p>Controllers stay thin: bind, validate, delegate, return. No business rules,
 * no repository access, no try/catch. Failures are thrown by the service and
 * turned into the standard error body by the global handler.
 *
 * <p>Conventions shown here:
 * <ul>
 *   <li>versioned base path, plural resource name;</li>
 *   <li>search filters bound from query parameters, so a list view is
 *       bookmarkable and cacheable;</li>
 *   <li>create answers 201 with a {@code Location} header;</li>
 *   <li>delete takes a body, because it carries the optimistic-lock token;</li>
 *   <li>authorisation expressed per endpoint with {@code @PreAuthorize}.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/samples")
@RequiredArgsConstructor
@Tag(name = "Samples", description = "Reference CRUD endpoints")
public class SampleController {

    private final SampleService sampleService;

    /**
     * Searches samples.
     *
     * @param request filters plus paging, bound from query parameters
     * @return one page of results
     */
    @GetMapping
    @Operation(summary = "Search samples with paging and sorting")
    public ResponseEntity<SamplePageResponse> search(@Valid @ModelAttribute SampleSearchRequest request) {
        return ResponseEntity.ok(sampleService.search(request));
    }

    /**
     * Returns one sample.
     *
     * @param id record id
     * @return the sample
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get one sample by id")
    public ResponseEntity<SampleResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(sampleService.getById(id));
    }

    /**
     * Creates a sample.
     *
     * @param request create payload
     * @return the created sample
     */
    @PostMapping
    @Operation(summary = "Create a sample")
    @PreAuthorize("hasRole('SAMPLE_EDITOR')")
    public ResponseEntity<SampleResponse> create(@Valid @RequestBody SampleCreateRequest request) {
        SampleResponse created = sampleService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/samples/" + created.getId())).body(created);
    }

    /**
     * Updates a sample.
     *
     * @param id      record id
     * @param request update payload including the lock token
     * @return the updated sample
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update a sample, rejecting stale writes")
    @PreAuthorize("hasRole('SAMPLE_EDITOR')")
    public ResponseEntity<SampleResponse> update(@PathVariable UUID id,
                                                 @Valid @RequestBody SampleUpdateRequest request) {
        return ResponseEntity.ok(sampleService.update(id, request));
    }

    /**
     * Soft-deletes a sample.
     *
     * @param request record id and lock token
     * @return an empty 204 response
     */
    @DeleteMapping
    @Operation(summary = "Soft-delete a sample, rejecting stale writes")
    @PreAuthorize("hasRole('SAMPLE_EDITOR')")
    public ResponseEntity<Void> delete(@Valid @RequestBody DeleteBaseRequest request) {
        sampleService.delete(request);
        return ResponseEntity.noContent().build();
    }
}
