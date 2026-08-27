package com.luvina.base.template.service;

import java.util.UUID;

import com.luvina.base.core.dto.DeleteBaseRequest;
import com.luvina.base.template.dto.request.SampleCreateRequest;
import com.luvina.base.template.dto.request.SampleSearchRequest;
import com.luvina.base.template.dto.request.SampleUpdateRequest;
import com.luvina.base.template.dto.response.SamplePageResponse;
import com.luvina.base.template.dto.response.SampleResponse;

/**
 * Use cases for samples.
 *
 * <p>The interface is the contract the controller depends on; the
 * implementation lives in {@code service.impl}. Keeping them apart is the
 * convention here, and it keeps controller tests free of persistence concerns.
 *
 * <p>No {@code Locale} parameter appears in any signature. The request locale is
 * ambient and is read where the message is built.
 */
public interface SampleService {

    /**
     * Returns one page of samples matching the filters.
     *
     * @param request filters plus paging
     * @return the page
     */
    SamplePageResponse search(SampleSearchRequest request);

    /**
     * Returns one sample.
     *
     * @param id record id
     * @return the sample
     */
    SampleResponse getById(UUID id);

    /**
     * Creates a sample.
     *
     * @param request create payload
     * @return the created sample, including its lock token
     */
    SampleResponse create(SampleCreateRequest request);

    /**
     * Updates a sample, rejecting the request when the caller worked from a
     * stale copy.
     *
     * @param id      record id
     * @param request update payload including the lock token
     * @return the updated sample
     */
    SampleResponse update(UUID id, SampleUpdateRequest request);

    /**
     * Soft-deletes a sample, rejecting the request when the caller worked from a
     * stale copy.
     *
     * @param request record id and lock token
     */
    void delete(DeleteBaseRequest request);
}
